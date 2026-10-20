# Plan: Tarjeta AR dinámica para `LiveStudentRoomScreen`

## Objetivo

Hoy, cuando un estudiante entra a una sala en vivo y `showingCard` es `true`
(`StudentClassRoom` en
`app/src/main/java/co/edu/uniautonoma/inclusivereadingar/presentation/student/LiveStudentRoomScreen.kt:290-306`),
la palabra activa (`session.displayText`) se muestra como texto plano en una
`Surface` de Compose. No hay cámara ni AR en ese flujo.

Se pide: encender la cámara al entrar a la sala, reconocer una tarjeta física
**en blanco** (sin texto ni marcador impreso), proyectar sobre ella la palabra
activa en 3D, y mantenerla sincronizada en tiempo real cuando el profesor
cambia de tarjeta vía Ably — reutilizando el motor WebAR existente en `webar/`.

## Decisión ya tomada

> "decisión YA TOMADA con el usuario: image tracking de la textura de la
> tarjeta en blanco, no un marcador ArUco/Hiro impreso aparte."

El tracking debe ser **sin marcador tipo Hiro/Kanji/ArUco**: se reconoce la
textura/bordes de la cartulina física en blanco mediante NFT (Natural Feature
Tracking), no un código impreso adicional sobre la tarjeta.

## 1. Arquitectura actual del motor WebAR (`webar/`)

Fuente: `webar/README.md`, `webar/src/index.html`, `webar/src/js/*.js`.

- **Build/empaquetado**: `webar/scripts/build.mjs` copia `src/` + los
  `node_modules` de `aframe` (1.6.0) y `@ar-js-org/ar.js` (3.4.8) hacia
  `app/src/main/assets/webar/`. Gradle no depende de Node en build time; los
  artefactos se versionan. El bundle vendorizado actual solo incluye
  `vendor/aframe.min.js` y `vendor/aframe-ar.js` (la build de **marcadores**,
  `node_modules/@ar-js-org/ar.js/aframe/build/aframe-ar.js`).
- **Activación de cámara**: `camera-controller.js` crea el `<a-scene arjs="...
  detectionMode: mono_and_matrix">` y expone `capabilities()` / `start()` /
  `stop()`. La cámara se activa explícitamente por un botón `#activate-camera`
  en `app.js` (`activateCamera()`), no automáticamente al cargar el documento.
- **Dos mecanismos de contenido ya conviven en el motor**:
  1. **Marcadores Hiro/Kanji** (`marker-config.js`, `a-marker preset="hiro"`):
     el modelo se ancla a un `a-entity` hijo del `<a-marker>` real (tracking
     3D verdadero vía ARToolKit, estable en posición y rotación).
  2. **"Word target" basado en OCR** (`word-target.js`,
     `camera-controller.js#positionWordTarget`, `#word-model-root`): esto
     **no es tracking de imagen**. Android ejecuta OCR sobre frames de la
     cámara (ver abajo), detecta la palabra impresa y manda
     `{type:"activate-word-target", word, centerX, centerY}`. El JS solo
     reproyecta esas coordenadas de pantalla normalizadas a un plano fijo
     frente a la cámara (`#unprojectToCameraPlane`, distancia fija de 2
     unidades) y posiciona ahí un `a-entity` anclado a la cámara — **no sigue
     la tarjeta física si se mueve o rota**, solo sigue el punto 2D detectado
     por OCR cuadro a cuadro.
  - Esta distinción es crítica: el mecanismo "word-target" ya existente
    **depende de que la tarjeta tenga texto legible por OCR**. Una tarjeta en
    blanco no genera ninguna detección OCR, así que ese camino no sirve de
    base para "reconocer la tarjeta física en blanco".
- **Carga/posicionamiento de modelos 3D** (`model-controller.js`): recibe un
  `markerId` (o el pseudo-id `WORD_TARGET_MODEL_ID` del word-target) y un
  `modelUrl` HTTPS, crea un `<a-entity gltf-model="...">` dentro del anchor
  registrado (`registerMarker(markerId, root)`), espera `model-loaded` /
  `model-error`, y para el word-target normaliza el tamaño del GLB cargado
  (`#fitWordModel`) a un bounding box máximo de `0.45` unidades. Este mismo
  componente es reutilizable para anclar el GLB de tarjeta: solo necesita que
  se le registre un nuevo `root` (el anchor del `a-nft` de NFT, ver sección 3).
- **`native-bridge.js`**: contrato mínimo y ya bidireccional:
  - Nativo → JS: `window.WebAR.receiveNativeMessage(jsonString)` (expuesto en
    `app.js` como `window.WebAR = Object.freeze({ receiveNativeMessage, stop })`).
    Soporta ya `asset-ready`, `activate-word-target`, `clear-word-target`,
    `word-not-found`, `marker-not-found`, `clear-marker`,
    `camera-permission-denied`. Es trivial añadir un nuevo tipo de mensaje
    (p. ej. `update-card-word`) sin tocar el resto del protocolo.
  - JS → Nativo: `NativeBridge.send(type, payload)` llama
    `window.lecturaAumentada.postMessage(JSON.stringify({...payload, type,
    sessionId}))`. El `sessionId` viaja en la URL (`?nativeSession=...`) y se
    valida con un regex; esto evita que un documento WebView viejo/zombi
    inyecte eventos en una sesión nueva.
  - Conclusión: el puente **es reusable tal cual** para mandar "la palabra
    cambió" desde Kotlin → JS. No se necesita rediseñarlo, solo agregar un
    mensaje nuevo al switch de `receiveNativeMessage` en `app.js`.
- **Detección de marcador**: `detection-controller.js` + `marker-gate.js`
  gestionan `markerFound`/`markerLost` con una gracia de 450 ms para evitar
  parpadeos. Este patrón (`found`/`lost` con debounce) es el que se replicaría
  para los eventos `markerFound`/`markerLost` que emite `a-nft` en NFT.
- **Audio**: `audio-controller.js` reproduce `audioUrl` ligado al asset; no es
  parte directa de este feature pero comparte el mismo ciclo de vida
  (`clear()`/`destroy()`) que se deberá respetar al parar la experiencia.

## 2. Integración de WebView nativo — YA EXISTE

Contrario a lo que el enunciado contempla como posible gap, la integración ya
está construida y en producción para la pantalla de escaneo de temas (no para
la sala en vivo):

- `app/src/main/java/co/edu/uniautonoma/inclusivereadingar/presentation/screens/WebArScreen.kt`:
  `WebView` completo con `WebViewAssetLoader` (origen
  `https://appassets.androidplatform.net/assets/webar/index.html`),
  `WebViewClient`/`WebChromeClient` para permisos de cámara, puente de mensajes
  (`WebViewCompat.addWebMessageListener` o `addJavascriptInterface` como
  fallback legacy), recarga de documento por sesión (`nativeSession` en la
  URL) y reenvío de comandos salientes vía
  `webView.evaluateJavascript("window.WebAR.receiveNativeMessage(...)")`.
- `presentation/viewmodel/WebArViewModel.kt`: máquina de estados que reacciona
  a `onMarkerFound`, `onOcrWordDetected`, etc., y publica `WebArCommand`
  (`AssetReady`, `ActivateWordTarget`, `ClearWordTarget`, ...) como un
  `outboundCommand` consumido por un único `LaunchedEffect` en
  `WebArScreen.kt:323-332`, que es exactamente el patrón a copiar para
  entregar la palabra nueva.
- `config/WebArConfig.kt`: constantes de origen confiable y nombre del puente.
- **Gap real**: `LiveStudentRoomScreen.kt` / `StudentClassRoom` **no usa nada
  de esto**. Es una pantalla 100% Compose sin `WebView`. El plan debe:
  1. Añadir una variante de `WebArScreen`-like embebida dentro de
     `StudentClassRoom` (o reutilizar `WebArScreen` parametrizado) cuando
     `showingCard` es verdadero.
  2. Crear un `WebArViewModel`-equivalente (o extender el actual) que, en vez
     de reaccionar a OCR/marker events para buscar un asset en el backend,
     reciba directamente `session.displayText` desde
     `LiveStudentRoomViewModel` y lo empuje al WebView como texto a dibujar
     sobre el modelo de tarjeta (no busca un asset GLB por palabra: el GLB es
     siempre el mismo "molde" de tarjeta en blanco, solo cambia el texto).

## 3. Mecanismo de image tracking sin marcador (NFT)

- El build vendorizado actual (`@ar-js-org/ar.js@3.4.8`, archivo
  `aframe/build/aframe-ar.js`) es la variante de **marcadores** (Hiro/Kanji/
  barcode vía ARToolKit, `detectionMode: mono_and_matrix`). **No incluye**
  soporte NFT.
- AR.js sí ofrece NFT, pero como build **separada y mutuamente excluyente**:
  `aframe/build/aframe-ar-nft.js` (fuente:
  `https://ar-js-org.github.io/AR.js-Docs/image-tracking/` — "AR.js comes in
  two different builds. They are both maintained. **They are exclusive.**").
  Usa `jsartoolkit5` para extraer features de una imagen de referencia y
  requiere una etiqueta `<a-nft>` en vez de `<a-marker>`.
- **Preparación de la imagen de referencia**: NFT necesita compilar la foto de
  la tarjeta en blanco a un set de 3 archivos (`.fset`, `.fset3`, `.iset`) con
  una herramienta offline (p. ej. `NFT-Marker-Creator`, Node/Docker). Esto es
  un paso de build, no runtime: se ejecuta una vez contra una foto de
  referencia de la cartulina física y el resultado se empaqueta como asset
  estático en `app/src/main/assets/webar/markers-nft/`.
- **Riesgo/bloqueante real**: una cartulina en blanco sin textura (color
  liso, sin bordes ni textos) es un *mal objetivo* para NFT — el algoritmo
  depende de encontrar features visuales (esquinas, gradientes, patrones) y
  una superficie uniforme no provee suficientes. Esto es un riesgo de producto,
  no solo técnico: conviene decidir si la "tarjeta en blanco" tendrá algún
  patrón sutil impreso (textura, marco, logo institucional de bajo contraste)
  que sea invisible/neutro para el estudiante pero suficiente para NFT. Si la
  tarjeta es verdaderamente lisa, NFT no es viable sin modificar el objeto
  físico, y habría que reconsiderar la decisión ya tomada (abrir la
  conversación con el usuario en vez de forzar una implementación que no va a
  trackear de forma confiable).
- **Alternativa evaluada — MindAR**: tiene mejor documentación y estabilidad
  de NFT que AR.js, pero es un motor distinto (no A-Frame-AR.js) con su propio
  componente `mindar-image`. Adoptarlo contradice "reutilizar lo que ya
  existe" y duplicaría vendorización/build. **No se recomienda** salvo que una
  prueba de campo demuestre que `aframe-ar-nft.js` no trackea de forma
  aceptable la tarjeta real; en ese caso, MindAR sería el plan de
  contingencia documentado, no la ruta por defecto.
- **Conflicto con los marcadores Hiro/Kanji existentes**: como ambas builds
  son excluyentes, cargar `aframe-ar-nft.js` en el documento de
  `LiveStudentRoomScreen` significa que esa instancia de WebView **no puede
  simultáneamente detectar Hiro/Kanji**. Esto es aceptable porque es un
  documento HTML separado del flujo de "Temas" (`WebArScreen.kt` sigue usando
  `aframe-ar.js` sin tocar), pero debe quedar explícito que no son la misma
  página ni el mismo bundle.

## 4. Plantilla 3D de tarjeta en blanco

Búsqueda web realizada (WebSearch disponible). Candidatos concretos:

| Candidato | Fuente | Licencia | Formato | Notas |
|---|---|---|---|---|
| **CC0 – Magnet Card** (plaggy) | https://sketchfab.com/3d-models/cc0-magnet-card-cd30d8a0aef14fdba16ea317d3f8ceed | CC0 1.0 Universal (dominio público) | Descargable como glTF/GLB desde Sketchfab | Autor conocido por modelos técnicos limpios y bien escalados; uso comercial sin restricción. Candidato recomendado si el biselado/grosor se ajusta bien al tamaño real de la cartulina. |
| **Blank Business Card (Rectangle)** (moguljordan) | https://sketchfab.com/3d-models/blank-business-card-rectangle-847f472d6bdd40e0a95fe40a2c69b8d3 | CC Attribution (CC-BY) | glTF/GLB vía Sketchfab | Forma rectangular plana explícita ("blank"), pero CC-BY exige atribución visible en créditos de la app — verificar si el equipo legal lo acepta. |
| **Pickup Key Card** (Quaternius) | https://poly.pizza/m/EDvCEBvs8k | CC0 (Public Domain) | FBX / GLTF (Poly Pizza permite exportar GLB) | Forma de "tarjeta" tipo llave-tarjeta, grosor y proporciones ya listos para juegos; todo el catálogo de Quaternius en Poly Pizza es CC0 y de uso comercial libre. |

**Recomendación**: priorizar **CC0 – Magnet Card** o **Pickup Key Card**
(ambos CC0, sin atribución obligatoria) sobre el de `moguljordan` (CC-BY). Se
recomienda descargar el GLB, abrirlo en Blender para (a) confirmar escala real
en metros equivalente a una cartulina de tarjeta didáctica (~9×6 cm o el
tamaño físico real que use el colegio), y (b) aplicar un material simple
(blanco mate) si el original trae textura de marca/logo que no aplica aquí.

No se encontró en la búsqueda un asset "flashcard educativa en blanco"
dedicado con licencia permisiva; los tres candidatos son aproximaciones de
forma de tarjeta genérica que requieren un ajuste de escala/material menor en
Blender antes de integrarlos (trabajo de arte, no de ingeniería AR).

### Plan B (si ningún candidato convence o la escala no calza)

Un plano (`a-plane`) o `a-box` delgado con una textura generada por
`<canvas>` 2D que dibuje bisel/sombra con un gradiente radial sutil y un borde
redondeado, aplicada como `material.src` o `three.CanvasTexture`. Es menos
convincente visualmente que un GLB real con geometría biselada, pero:
- Elimina la dependencia de licencias de terceros.
- Es trivial de regenerar en cualquier resolución/color.
- Se integra con el mismo mecanismo de "canvas → textura" que se necesita de
  todas formas para pintar el texto dinámico (sección 5), así que no agrega
  una tecnología nueva al stack.

Trade-off: pierde la sensación de "tarjeta cobrando vida en 3D" que pide el
punto 3 del enunciado (es más un "panel 2D flotante" que un objeto 3D). Se
documenta como fallback, no como recomendación principal.

## 5. Texto dinámico sobre el modelo 3D

Tres mecanismos evaluados dado el stack vendorizado (A-Frame 1.6 + AR.js):

1. **Componente `text` de A-Frame** (`<a-text value="...">`), hijo del
   `a-entity` de la tarjeta. Más simple de implementar (una línea,
   `setAttribute("value", word)`), pero el texto queda como una geometría
   separada flotando sobre/junto al modelo — no se "funde" con la superficie
   de la tarjeta ni respeta perspectiva/curvatura del GLB.
2. **Canvas 2D + `THREE.CanvasTexture`** aplicado al material de la cara
   superior del GLB de tarjeta. Requiere: (a) que el GLB de tarjeta tenga un
   slot de material/UV dedicado para la cara visible, o (b) superponer un
   plano hijo alineado exactamente con esa cara y aplicarle la textura ahí.
   Da el efecto más fiel de "la palabra está impresa en la tarjeta", y
   reutiliza el mismo patrón de canvas que ya se evaluó para el Plan B de la
   sección 4.
3. **Material con `src` apuntando a un canvas vía `id`** (`<a-plane
   material="src: #word-canvas">` + `<canvas id="word-canvas">` en el DOM).
   Es la forma declarativa de A-Frame para (2): A-Frame observa el canvas y
   lo sube como textura automáticamente cuando cambia, sin tocar `THREE.*`
   manualmente.

**Recomendación**: opción 3 (canvas con `id` referenciado por
`material.src`) montado como plano hijo alineado a la cara visible del GLB de
tarjeta. Es la opción con menos código imperativo (A-Frame gestiona la
resubida de textura), no exige tocar `THREE.CanvasTexture` a mano, y es
reutilizable 1:1 para el Plan B de la sección 4 si se termina optando por el
plano procedural en vez de un GLB de terceros.

**Actualización de la textura al llegar una palabra nueva**: dibujar la nueva
palabra en el `<canvas>` 2D (texto centrado, fuente grande, fondo
transparente o blanco según el diseño de la tarjeta) y, si el motor no
refresca solo, forzar `material.map.needsUpdate = true` sobre el
componente `material` del plano (vía
`planeEl.getObject3D("mesh").material.map.needsUpdate = true`) después de
redibujar. Esto no requiere recargar el GLB ni recrear ninguna entidad — solo
redibujar el canvas — por lo que el cambio de palabra es prácticamente
instantáneo y no reintroduce el costo de descarga de un modelo nuevo por cada
palabra (a diferencia de `model-controller.js#show`, que sí descarga un GLB
por asset — aquí el GLB de tarjeta se carga una sola vez por sesión AR).

## 6. Flujo de datos end-to-end propuesto

```
Profesor cambia de tarjeta/palabra (app del profesor)
  → Ably publica evento en el canal de la sala
  → LiveDomanRealtimeClient (data/remote) decodifica el payload Ably
    (ver commit "fix: decode Ably message payloads on Android")
  → LiveStudentRoomViewModel#applyRealtimeSession actualiza
    _uiState.session (incluye session.displayText)
  → StudentClassRoom (Compose) recalcula showingCard / session.displayText
  → [NUEVO] LaunchedEffect(session.displayText, webViewRef) en la variante
    AR de StudentClassRoom detecta el cambio de texto
  → webView.evaluateJavascript(
      "window.WebAR && window.WebAR.updateCardWord(" +
      JSONObject.quote(session.displayText) + ");"
    )
  → [NUEVO] app.js expone window.WebAR.updateCardWord(word) (o se integra
    como un nuevo case "update-card-word" en receiveNativeMessage, igual
    que el patrón ya usado para "activate-word-target")
  → ese handler redibuja el <canvas id="word-canvas"> con la palabra nueva
    y marca needsUpdate = true sobre la textura del plano hijo del modelo
    de tarjeta anclado al <a-nft>
```

Notas de diseño:
- Se reutiliza el patrón exacto de `uiState.outboundCommand` +
  `LaunchedEffect(...) { webView.evaluateJavascript(...); onCommandDelivered(id) }`
  ya presente en `WebArScreen.kt:323-332`, para evitar reenvíos duplicados o
  perdidos durante recomposición.
- `session.displayText` ya es la fuente de verdad única (no hay que inventar
  un nuevo campo de estado); solo se necesita observarlo con `LaunchedEffect`
  y comparar contra el último valor enviado al WebView para no espamear
  `evaluateJavascript` en cada recomposición no relacionada.
- No hay backend/asset lookup involucrado en este flujo (a diferencia de
  `WebArViewModel` actual, que busca un `ArAsset` por palabra/marcador): la
  palabra viaja directo del estado de la sala al canvas. Esto simplifica
  bastante respecto al flujo de "Temas".

## 7. Fallback, accesibilidad y permisos

- **Permiso de cámara runtime**: mismo patrón ya implementado en
  `WebArScreen.kt` (`PermissionRequest` interceptado en `WebChromeClient`,
  `rememberLauncherForActivityResult(RequestPermission())`). Se reutiliza sin
  cambios de diseño.
- **Permiso denegado**: `onCameraPermissionDenied()` ya existe en
  `WebArViewModel`; para la sala en vivo, el fallback natural es **no
  reemplazar la tarjeta de texto plano actual** — si el permiso se niega o el
  dispositivo no tiene cámara compatible (`capabilities().camera === false`),
  `StudentClassRoom` debe renderizar la `Surface` de texto plano que ya existe
  hoy (líneas 302-306) en vez de montar el `WebView`. Esto hace que la AR sea
  un *enhancement* progresivo, no un requisito duro para que la clase
  funcione — coherente con el comentario ya existente en el código: *"Student
  rendering is deliberately passive: it only reflects teacher/server state."*
- **Tarjeta física no reconocida / fuera de cuadro**: mientras `a-nft` no
  emite `markerFound`, se debe mostrar una guía visual simple (overlay con
  instrucción tipo "Enfoca la tarjeta en blanco" + ícono), igual en espíritu
  al estado `#loading`/`debug-panel` que ya tiene `index.html`. Si tras N
  segundos no se detecta nada, ofrecer un botón para volver al modo texto
  plano sin cerrar la clase.
- **Accesibilidad**: la pantalla actual usa `liveRegion = LiveRegionMode.Polite`
  sobre la `Surface` de texto (línea 303) para que lectores de pantalla
  anuncien el cambio de palabra. La variante AR debe preservar un anuncio
  equivalente (p. ej. un `Text` oculto visualmente pero con el mismo
  `semantics { liveRegion = ... }`) para no regresionar accesibilidad al
  introducir la cámara — la AR es visual por naturaleza y no puede ser el
  único canal de la palabra para un estudiante con baja visión.

## 8. Fases de implementación

**Fase 1 — WebView embebido en la sala en vivo, sin AR nueva**
- Archivos: `LiveStudentRoomScreen.kt` (nueva composición condicional cuando
  `showingCard`), posible extracción de piezas reusables de `WebArScreen.kt`
  a un componente compartido (`WebArContainer` o similar) para no duplicar la
  lógica de `WebViewAssetLoader`/permiso de cámara/ciclo de vida.
- Objetivo verificable: la cámara se activa al entrar a la sala y se detiene
  al salir, cargando el `index.html` de `webar/` existente sin cambios de
  comportamiento AR (se puede probar con los marcadores Hiro/Kanji de prueba
  para validar solo el wiring de WebView).

**Fase 2 — Modelo de tarjeta 3D (o procedural) + build NFT**
- Archivos: nuevo asset GLB en `app/src/main/assets/models/` (o
  `app/src/main/assets/webar/models/`), script de generación de `.fset`/
  `.fset3`/`.iset` (nuevo, p. ej. `webar/scripts/build-nft-marker.mjs` o
  documentación de un paso manual con `NFT-Marker-Creator`), actualización de
  `webar/scripts/build.mjs` para copiar `aframe-ar-nft.js` y los archivos NFT
  generados hacia `assets/webar/vendor` y `assets/webar/markers-nft`.
- Objetivo verificable: con la foto real de la tarjeta física, `a-nft` dispara
  `markerFound`/`markerLost` de forma estable al enfocar/retirar la tarjeta,
  en un documento HTML de prueba aislado (sin aún texto dinámico).

**Fase 3 — Wiring de texto dinámico sobre el modelo**
- Archivos: `webar/src/js/app.js` (nuevo caso en `receiveNativeMessage`, p. ej.
  `update-card-word`), nuevo módulo `webar/src/js/card-text-controller.js`
  (canvas 2D + plano con `material.src`), `webar/src/index.html` (añadir el
  `<canvas>` oculto y el `<a-nft>` con el GLB de tarjeta anidado).
- Objetivo verificable: con un valor de palabra fijo mandado por un botón de
  prueba (o la consola de DevTools vía `WebView.setWebContentsDebuggingEnabled`),
  la palabra se dibuja sobre la tarjeta reconocida y cambia sin recargar el
  modelo al invocar el mensaje de nuevo con otra palabra.

**Fase 4 — Conexión al estado real de Ably/`displayText`**
- Archivos: `LiveStudentRoomScreen.kt` (`LaunchedEffect(session.displayText,
  webViewRef)` + `evaluateJavascript`), posiblemente
  `LiveStudentRoomViewModel.kt` si se decide modelar el envío como un
  "outboundCommand" formal en vez de un efecto directo en la UI (preferible,
  por consistencia con `WebArViewModel`).
- Objetivo verificable: con dos dispositivos (profesor cambiando de tarjeta y
  estudiante en la sala con la tarjeta física enfocada), el texto proyectado
  en AR cambia en tiempo real al cambiar la tarjeta del profesor, sin
  recargar la pantalla del estudiante.

**Fase 5 — Permisos, fallback y accesibilidad**
- Archivos: `LiveStudentRoomScreen.kt` (rama de fallback a texto plano cuando
  no hay cámara/permiso denegado/tarjeta no reconocida tras timeout),
  anuncios de accesibilidad equivalentes a los de la `Surface` actual.
- Objetivo verificable: con permiso de cámara denegado, la sala sigue siendo
  usable en modo texto plano (comportamiento actual, sin regresión);
  TalkBack anuncia el cambio de palabra en ambos modos.

## 9. Riesgos y supuestos explícitos

- **Riesgo alto**: una cartulina verdaderamente lisa (sin textura/bordes
  marcados) puede no dar suficientes features para NFT. Esto se debe validar
  con una prueba física temprana (idealmente al cierre de la Fase 2) antes de
  invertir en las fases 3-5.
- **Riesgo medio**: `aframe-ar-nft.js` y `aframe-ar.js` son builds excluyentes;
  si en el futuro se quisiera un único documento HTML que soporte Hiro/Kanji
  **y** NFT de tarjeta en la misma sesión, se necesitaría una solución
  distinta (dos escenas, o cambiar de motor). Hoy no es necesario porque son
  flujos de pantalla distintos (`WebArScreen` vs. la nueva pantalla de sala).
- **Riesgo medio**: no se encontró en la búsqueda web un asset de "tarjeta
  educativa en blanco" dedicado; los tres candidatos requieren ajuste manual
  de escala/material en Blender. Si ese ajuste no es trivial, el Plan B
  procedural (canvas) puede terminar siendo la ruta más rápida y debería
  revalidarse antes de cerrar la Fase 2.
- **Riesgo bajo**: el `model-controller.js` actual fue diseñado para
  descargar un GLB *distinto* por cada asset/marcador. Para este feature el
  GLB de tarjeta es fijo (una sola descarga por sesión) y solo el canvas
  cambia; hay que confirmar que no se reutiliza por error el flujo de
  `modelController.show()` (que limpia y recrea la entidad en cada llamada)
  para el cambio de palabra — el texto debe actualizarse sin re-disparar la
  descarga/carga del GLB.
- **Supuesto**: `session.displayText` ya llega sano (sin HTML/caracteres de
  control) desde el backend vía Ably, dado que hoy se renderiza directo en un
  `Text` de Compose. Al pasar a dibujarse en un `<canvas>` del WebView, debe
  tratarse igual de confiable (no es un vector de XSS porque no se inserta
  como HTML, se dibuja con `CanvasRenderingContext2D.fillText`), pero conviene
  acotar longitud máxima para que no desborde el área de la tarjeta.
- **Supuesto**: el tamaño físico real de la cartulina usada en el aula es
  conocido y estable (mismo tamaño para todas las tarjetas), para poder fijar
  la escala del GLB de tarjeta y del `a-nft` de forma consistente.

## Checklist de aprobación

- [ ] ¿La licencia del asset 3D elegido (sección 4) permite uso comercial y
      sin atribución obligatoria dentro de la app publicada? (CC0 recomendado
      sobre CC-BY).
- [ ] ¿La cartulina física real tiene suficiente textura/borde/patrón visual
      para que NFT la reconozca de forma confiable, o se necesita imprimirle
      un patrón sutil adicional? Esto debe validarse con una prueba física
      antes de avanzar más allá de la Fase 2.
- [ ] ¿El mecanismo de texto dinámico (canvas + `material.src`, sección 5) es
      aceptable visualmente, o se requiere una fidelidad mayor (texto
      verdaderamente extruido/relieve sobre la geometría 3D)?
- [ ] ¿El mecanismo de texto dinámico propuesto no requiere reescribir el
      motor AR ni reemplazar AR.js/A-Frame por otro motor (MindAR u otro)?
- [ ] ¿Es aceptable que, al no reconocerse la tarjeta física o al negarse el
      permiso de cámara, el estudiante caiga de vuelta al modo texto plano
      actual, en vez de bloquear la clase?
- [ ] ¿El tamaño físico de la cartulina es uniforme entre todas las aulas/
      kits, para poder fijar una sola escala de GLB y de referencia NFT?
- [ ] ¿Se acepta que el documento HTML de la sala en vivo (NFT) y el de
      "Temas" (marcadores Hiro/Kanji) sean dos bundles/documentos AR.js
      distintos, dado que las builds de marcador y NFT son excluyentes?
- [ ] ¿El orden de fases (1→5) propuesto en la sección 8 es el deseado, o se
      prefiere validar primero el riesgo de NFT (Fase 2) antes de invertir en
      el wiring de WebView (Fase 1)?

## Revisión técnica independiente

**Veredicto: APROBADO CON CAMBIOS.**

El plan es, en términos generales, honesto y bien fundamentado en el código
real — no inventa APIs que no existen y marca explícitamente sus propios
riesgos y supuestos no verificados. Verifiqué contra el repo real (no contra
lo que el plan afirma) los puntos críticos antes de aprobar.

### Afirmaciones verificadas como correctas

- `StudentClassRoom` en `LiveStudentRoomScreen.kt:287-313` es 100% Compose sin
  `WebView`; `showingCard` se calcula como `session.status == "running" &&
  !session.displayText.isNullOrBlank() && !uiState.isLocallyPaused` (línea
  290) y hoy renderiza texto plano en un `Surface` con
  `liveRegion = LiveRegionMode.Polite` (línea 303). Coincide con el plan.
- `LiveStudentRoomViewModel.kt` tiene `applyRealtimeSession` (línea 116) y
  `displayText` como campo de `LiveDomanSession`, confirmando el flujo Ably →
  ViewModel → `displayText` tal como lo describe la sección 6.
- `WebArScreen.kt:323-332` usa exactamente el patrón
  `LaunchedEffect(uiState.outboundCommand?.id, webViewRef) { ...
  webView.evaluateJavascript("window.WebAR && window.WebAR.receiveNativeMessage(...)")
  ... onCommandDelivered(id) }` que el plan propone reutilizar. Correcto.
- `native-bridge.js` y el `switch (message.type)` en `app.js:129-209` son
  tal como el plan los describe: agregar un `case` nuevo (p. ej.
  `update-card-word`) es mecánicamente trivial y no rompe el resto del
  protocolo porque el `default: break` ya tolera tipos desconocidos.
- **Confirmado con alta confianza (no solo "el plan lo dice")**: revisé
  `webar/package.json` (`@ar-js-org/ar.js@3.4.8`) y el contenido real
  instalado en `webar/node_modules/@ar-js-org/ar.js/aframe/build/`: existe
  `aframe-ar-nft.js` junto a `aframe-ar.js` en la dependencia ya declarada —
  es decir, NFT no requiere agregar una dependencia nueva, solo vendorizarla.
  Y confirmé en `webar/scripts/build.mjs:41-48` que el build actual **solo**
  copia `aframe-ar.js` hacia `assets/webar/vendor/`, nunca `aframe-ar-nft.js`.
  El plan tiene razón: NFT está disponible en el paquete pero no vendorizado
  hoy, y falta un paso de build nuevo (Fase 2).
- `model-controller.js#show()` (líneas 20-62) efectivamente hace `this.clear()`
  al inicio de cada llamada y recrea la entidad `a-entity` con
  `gltf-model`, descargando el GLB otra vez. El riesgo "bajo" que el plan
  marca en la sección 9 (no reusar por error `modelController.show()` para
  el cambio de palabra) es real y está correctamente identificado — si la
  Fase 3 llama a `show()` en cada palabra nueva en vez de solo redibujar el
  canvas, se re-descargaría el GLB de tarjeta en cada cambio de palabra,
  introduciendo latencia/parpadeo innecesarios en cada ciclo Ably.
- El patrón `found`/`lost` con debounce de `marker-gate.js` /
  `detection-controller.js` es replicable para `a-nft`, que en AR.js sí emite
  `markerFound`/`markerLost` igual que `a-marker` (API real de AR.js, no
  inventada).

### Correcciones / puntos marcados como no verificables

- **Las 3 URLs de modelos 3D (sección 4) no son verificables desde este
  entorno** (sin acceso a red para confirmar Sketchfab/Poly Pizza en este
  momento de la auditoría). No las apruebo ni las rechazo — quedan marcadas
  como **"no verificadas, requieren confirmación manual antes de descargar"**.
  Independientemente de si las URLs son exactas, la cautela del plan (CC0
  preferido sobre CC-BY, confirmar escala/licencia antes de integrar) es la
  postura correcta y no cambia aunque una URL puntual esté desactualizada o
  rota — el checklist de aprobación ya exige esa verificación antes de
  avanzar, lo cual es suficiente mitigación.
- La sección 5 (texto dinámico vía canvas + `material.src`) es coherente con
  los componentes estándar de A-Frame (`material` component observa un
  `<canvas>` por `id` y lo sube como textura); no encontré nada en
  `model-controller.js` que la mecánica en sí contradiga, pero el plan **no
  especifica cómo el plano-hijo con el canvas se registra/ancla sin pasar por
  `registerMarker`/`show()`** de `ModelController` — esto debe resolverse
  explícitamente en el diseño de la Fase 3 (un mecanismo paralelo al de
  `ModelController`, o una extensión de la clase), no delegarse a
  "implementación trivial" como sugiere el texto.

### Huecos no cubiertos por el plan (cambios obligatorios antes de ejecutar)

1. **Falta cobertura de tests explícita.** El repo tiene disciplina de tests
   unitarios para cada módulo JS nuevo (`webar/tests/*.test.mjs` cubre cada
   controller existente: `model-controller.test.mjs`,
   `native-bridge.test.mjs`, `camera-controller.test.mjs`, etc.) y para los
   ViewModels de Kotlin (`WebArViewModelTest.kt`,
   `WebArCommandSerializationTest.kt`). El plan no menciona en ninguna fase
   que `card-text-controller.js` nuevo, el nuevo caso en `app.js`, o la
   variante AR de `StudentClassRoom`/su ViewModel deban llevar tests
   equivalentes. **Obligatorio**: cada fase (especialmente 3 y 4) debe incluir
   tests unitarios nuevos siguiendo la convención existente antes de
   considerarse "verificable", no solo pruebas manuales con botón/DevTools.
2. **No se menciona impacto de batería/rendimiento en gama baja.** El propio
   repo tiene benchmarks de Rive para dispositivos de gama baja
   (`docs/mascota-rive.md`), lo que indica que el target de hardware ya es
   una preocupación conocida del equipo. A-Frame + WebGL + NFT (que corre
   `jsartoolkit5` en JS, más pesado que el tracking de marcador clásico) en
   un `WebView` sobre un dispositivo de gama baja es un riesgo de
   rendimiento/batería no mencionado en absoluto en la sección 9. **Cambio
   obligatorio**: agregar un criterio de aceptación de rendimiento (FPS
   mínimo aceptable, comportamiento si el frame rate cae) a la Fase 2 o 3, y
   probarlo en el mismo tipo de dispositivo de gama baja usado para los
   benchmarks de Rive.
3. **Manejo de "dispositivo sin cámara trasera" no está explícito.** La
   sección 7 cubre permiso denegado y `capabilities().camera === false`
   (verificado: existe en `camera-controller.js:15-16`, chequea
   `navigator.mediaDevices?.getUserMedia`), pero no cubre el caso de un
   dispositivo con cámara frontal únicamente o sin cámara trasera utilizable
   para AR (common en tablets educativas baratas). `capabilities()` no
   parece distinguir front/back camera — debe confirmarse si eso importa para
   este flujo (si getUserMedia por defecto selecciona la trasera, puede ser
   un no-issue, pero el plan no lo examina ni lo descarta).
4. **No hay mención de WebGL no disponible.** El chequeo de `capabilities()`
   cubre `getUserMedia` y `webxr`, no si el `WebView` del dispositivo soporta
   WebGL (requerido por A-Frame/three.js). En un WebView viejo o con
   aceleración de hardware deshabilitada, A-Frame falla de forma distinta a
   "sin cámara". El plan no define ese fallback — debe agregarse a la Fase 5.
5. **Riesgo de producto de la sección 9 (cartulina lisa) es correctamente
   identificado como bloqueante, y la mitigación propuesta por el plan mismo
   (validar físicamente al cierre de la Fase 2, antes de invertir en 3-5) es
   razonable y suficiente** — no es necesario escalar esto a un rechazo total
   del plan, solo respetar estrictamente ese orden de validación (ver
   autorización de fases abajo).
6. **Dependencia silenciosa entre fases**: la Fase 4 asume que la Fase 3 ya
   decidió si el envío de la palabra se modela como `outboundCommand` formal
   o como efecto directo en la UI ("preferible, por consistencia con
   `WebArViewModel`" — texto del propio plan, sección 8). Esa decisión de
   diseño debe tomarse y documentarse en la Fase 3, no dejarse abierta hasta
   la Fase 4, porque cambia la forma del `ViewModel` que la Fase 3 ya habría
   empezado a construir. Recomiendo fijarla explícitamente como entregable de
   la Fase 3: "decidir y documentar el mecanismo de entrega de comandos
   (outboundCommand vs. efecto directo) antes de cerrarla", para que la
   Fase 4 sea un consumidor simple y no un rediseño.

### Fases autorizadas para ejecución inmediata

- **Fase 1 autorizada sin condiciones**: no depende de NFT, de ningún asset
  3D nuevo ni de ninguna decisión de producto pendiente. Es wiring de
  `WebView` reutilizando `WebArScreen.kt` ya probado en producción. Puede
  ejecutarse ya, agregando los tests unitarios/instrumentados equivalentes a
  los existentes para `WebArScreen`/`WebArViewModel` (cambio obligatorio 1).
- **Fase 2 autorizada, pero con el gate físico como criterio de salida
  obligatorio**: el trabajo de vendorizar `aframe-ar-nft.js` y escribir el
  script de generación de `.fset`/`.fset3`/`.iset` puede empezar ya (es
  trabajo de build, no depende de resolver el riesgo de producto), **pero no
  se puede dar por cerrada, y no se debe iniciar la Fase 3, hasta validar con
  la tarjeta física real que el tracking NFT es estable**. Si esa prueba
  falla, se activa el Plan B procedural de la sección 4 y se reevalúa antes
  de tocar una sola línea de la Fase 3.
- **Fases 3, 4 y 5 quedan bloqueadas** hasta que: (a) la validación física de
  NFT de la Fase 2 dé resultado positivo (o se confirme el pivote a Plan B),
  y (b) se resuelvan los cambios obligatorios 1 (tests), 2 (rendimiento en
  gama baja) y 6 (fijar el mecanismo de entrega de comandos en la Fase 3,
  no dejarlo abierto para la Fase 4) antes de escribir código de esas fases.
