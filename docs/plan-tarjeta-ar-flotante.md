# Plan: Tarjeta AR flotante (world-tracking sin marcador) para `LiveStudentRoomScreen`

## Contexto y relación con el plan anterior

Este documento **reemplaza el mecanismo de AR** descrito en
`docs/plan-tarjeta-ar-dinamica.md`, pero reutiliza buena parte de su
investigación de contenido (modelo 3D, texto dinámico, flujo de datos). No
edito ese archivo: queda como registro histórico de la decisión original (NFT
sobre cartulina física) y de su "Revisión técnica independiente".

**Decisión de arquitectura ya confirmada con el usuario (no se cuestiona
aquí):** no existe cartulina física. El esquema cambia a **AR sin marcador**
con world-tracking tipo "objeto flotando frente a la cámara, fijo en el
espacio, siguiendo el movimiento del celular" — **no** reconocimiento de
imagen NFT, **no** marcador Hiro/Kanji. Esto elimina de raíz el riesgo
principal que bloqueaba las Fases 3-5 del plan anterior (cartulina lisa sin
suficientes features visuales para NFT): ya no hay cartulina que reconocer.

## 1. Estado real de lo ya ejecutado

La Fase 1 del plan anterior (wiring de `WebView` en la sala en vivo) **ya está
implementada y en producción de código**, independiente del mecanismo AR que
se decida:

- `WebArContainer.kt`
  (`app/src/main/java/co/edu/uniautonoma/inclusivereadingar/presentation/webar/WebArContainer.kt`)
  monta el `WebView` del motor WebAR existente, gestiona permiso de cámara
  (`WebChromeClient.onPermissionRequest`), `WebViewAssetLoader`, el puente de
  mensajes (`WebViewCompat.addWebMessageListener` con fallback
  `addJavascriptInterface`) y el teardown
  (`evaluateJavascript("window.WebAR && window.WebAR.stop();")`). Por
  comentario propio del archivo, es "Fase 1 scope only: it loads the existing
  bundle as-is and activates the camera on mount, it does not drive
  marker/OCR/word-target state or send outbound AR commands."
- `LiveStudentRoomScreen.kt` ya integra `WebArContainer` condicionalmente en
  `StudentClassRoom` vía `shouldMountArWebView(showingCard, hasCameraHardware,
  arUnavailable)`, con fallback a `Surface` de texto plano y accesibilidad
  (`liveRegion = LiveRegionMode.Polite`) ya cableada. **No hay todavía** texto
  dinámico ni objeto 3D sobre la cámara: solo la cámara activa.
- `webar/scripts/build.mjs` **ya vendoriza `aframe-ar-nft.js`** (el build NFT
  de AR.js), copiado desde
  `node_modules/@ar-js-org/ar.js/aframe/build/aframe-ar-nft.js` hacia
  `app/src/main/assets/webar/vendor/aframe-ar-nft.js` (1,678,733 bytes reales
  en disco, no un placeholder). Sin embargo, `webar/src/index.html` **no
  carga** ese script (solo carga `vendor/aframe.min.js` y
  `vendor/aframe-ar.js`, el build de marcadores) — es decir, el archivo está
  en el bundle pero no se ejecuta en ningún flujo.

**Decisión explícita: revertir el vendorizado de `aframe-ar-nft.js` en
`build.mjs`.**

Justificación con evidencia del código real:
- Bajo la nueva arquitectura, NFT (tracking de imagen sobre una superficie
  física) no tiene ningún rol: no hay cartulina que fotografiar ni imagen de
  referencia que compilar a `.fset`/`.fset3`/`.iset`.
- El archivo nunca se referencia desde `index.html` ni desde ningún módulo
  JS existente (`app.js`, `camera-controller.js`, etc.) — es peso muerto
  (~1.6 MB) empaquetado en el APK sin ningún caller.
- AR.js documenta que `aframe-ar.js` (marcadores) y `aframe-ar-nft.js` (NFT)
  son builds **mutuamente excluyentes**; mantener el NFT vendorizado "por si
  acaso" no habilita nada y puede inducir a error a quien lea `build.mjs`
  pensando que el documento de la sala en vivo usa NFT.
- No se recomienda dejarlo "documentado como no utilizado": es más simple y
  menos ambiguo borrar las 3 líneas de `copyFile` de `aframe-ar-nft.js` en
  `webar/scripts/build.mjs` y, de paso, el comentario que lo justificaba
  (referencia al plan viejo), que mantener un artefacto binario sin dueño.
  Si en el futuro se necesita reconocimiento de imagen real, se puede
  re-vendorizar en una fase dedicada con su propio caso de uso.

Nota: `aframe-ar.js` (build de marcadores Hiro/Kanji) sigue usándose para la
pantalla de "Temas" (`WebArScreen.kt`) y **no se toca** en este plan — es un
documento HTML distinto (`webar` compartido pero contexto de uso separado).

## 2. Mecanismo técnico de AR sin marcador

Evalué tres vías dado lo que ya está vendorizado y el target de hardware
(gama baja, ver benchmarks de Rive en sección 5):

### Opción A — A-Frame puro + `getUserMedia` como fondo de video + objeto fijo en coordenadas de cámara

- Mecanismo: un `<video>` oculto alimentado por `getUserMedia` (ya existe
  este patrón de chequeo en `camera-controller.js`,
  `navigator.mediaDevices?.getUserMedia`) se usa como textura de fondo de la
  escena A-Frame (o simplemente se superpone el `<a-scene>` transparente
  sobre un `<video>` de HTML en el DOM). El objeto 3D de la tarjeta se coloca
  como hijo de la entidad `<a-camera>` (o de `a-entity` con
  `camera`), a una distancia fija (p. ej. `position: 0 0 -2`), de forma que
  siempre "flota" delante del lente sin ningún tracking de imagen ni de
  mundo: cuando el usuario mueve el celular, la cámara rota (vía
  `look-controls`/sensores de orientación de A-Frame, que sí son estándar y
  no dependen de WebXR) y el objeto se mueve con ella porque es su hijo.
- Ventajas: **cero dependencias nuevas**. A-Frame 1.6 (ya vendorizado) trae
  `look-controls` con soporte de `deviceorientation` de fábrica; no se
  necesita AR.js en absoluto para este modo (ni la build de marcador ni la de
  NFT). Es el mecanismo más simple, predecible y barato en CPU/batería.
- Esto reusa exactamente el mismo patrón que `camera-controller.js` ya usa
  para el "word-target" (sección 3 del plan viejo): reproyectar un punto fijo
  frente a la cámara sin tracking real de mundo. Confirma que el motor ya
  tiene precedente de este enfoque.

### Opción B — AR.js modo location-based

- AR.js ofrece un tercer modo (`location-based`, vía GPS/brújula) pensado
  para AR geolocalizada al aire libre, no para "objeto flotando frente al
  usuario en una habitación". No aplica a este caso de uso (no hay
  coordenadas GPS involucradas) y se descarta.

### Opción C — WebXR Device API real (`navigator.xr`)

- Daría el world-tracking "de verdad" (seis grados de libertad con SLAM,
  anclaje estable en el espacio real independientemente de rotación simple).
- **Punto más incierto del plan, verificado parcialmente por búsqueda web**:
  la evidencia encontrada indica que `android.webkit.WebView` (el componente
  que usa esta app, no Chrome completo) **no expone `navigator.xr`** — Chrome
  para Android sí lo soporta desde la versión 79 vía ARCore, pero el System
  WebView embebido históricamente no. No encontré evidencia de que esto haya
  cambiado en 2025/2026. **Esto sigue siendo un supuesto con confianza media,
  no una certeza absoluta de laboratorio** — la búsqueda no cubrió
  específicamente la versión de WebView que corre en los dispositivos de
  gama baja objetivo de este proyecto. **Recomendación: validar en
  dispositivo real** ejecutando `if (navigator.xr) {...}` en el WebView real
  de la app antes de descartar definitivamente esta opción, pero no bloquear
  el plan a esa validación.
- Independientemente del resultado, construir sobre WebXR sería más frágil
  (depende de soporte no garantizado) y más complejo que la Opción A para un
  efecto visual que no requiere SLAM real.

**Decisión: Opción A (A-Frame + `getUserMedia` + objeto fijo en coordenadas
de cámara).** No hace falta vendorizar nada nuevo: A-Frame 1.6 ya está en el
bundle y trae todo lo necesario (`look-controls`, soporte de cámara vía
componente `camera`). AR.js deja de ser necesario para este flujo
específico de la sala en vivo (ni `aframe-ar.js` ni `aframe-ar-nft.js` se
cargan en este documento HTML nuevo/variante).

## 3. Dónde vive el modelo 3D de la tarjeta

La investigación de `docs/plan-tarjeta-ar-dinamica.md` (sección 4) sobre
candidatos **sigue siendo válida y ahora es más simple de aplicar**, porque
el modelo ya no necesita calzar con el tamaño/proporciones de una cartulina
física real — es puramente decorativo, puede ser cualquier tamaño/forma
"de tarjeta" razonable sin validación contra un objeto del mundo real.

Candidatos documentados en el plan viejo (no se rehace la búsqueda, se cita):

| Candidato | Fuente | Licencia | Formato |
|---|---|---|---|
| CC0 – Magnet Card (plaggy) | Sketchfab | CC0 1.0 | GLB |
| Blank Business Card (Rectangle) (moguljordan) | Sketchfab | CC-BY | GLB |
| Pickup Key Card (Quaternius) | Poly Pizza | CC0 | FBX/GLTF |

Esas URLs fueron marcadas como **"no verificadas, requieren confirmación
manual"** por la revisión técnica independiente del plan viejo (sin acceso a
red en esa auditoría) — ese estado no cambia aquí, sigue pendiente de
confirmación manual antes de descargar.

**Plan B procedural (canvas con bisel/sombra)**, también documentado en el
plan viejo, sigue vigente como alternativa: un `a-plane`/`a-box` delgado con
una textura generada por `<canvas>` 2D (gradiente radial sutil + borde
redondeado). Bajo la nueva arquitectura es, si acaso, **más atractivo que
antes**: ya no hace falta que el modelo "calce" con nada físico, así que la
fidelidad visual de un GLB real importa menos, y el Plan B reutiliza el
mismo mecanismo canvas→textura que de todas formas se necesita para el texto
dinámico (sección 4).

**Recomendación**: priorizar el Plan B procedural como punto de partida
(cero dependencia de licencias de terceros, cero riesgo de URLs rotas o
escalas desajustadas, iteración instantánea) y evaluar reemplazarlo por un
GLB de Sketchfab/Poly Pizza (CC0 preferido sobre CC-BY) como mejora visual
posterior, no como bloqueante de la Fase B.

## 4. Texto dinámico sobre la tarjeta 3D flotante

No cambia respecto al plan viejo: canvas 2D referenciado por `id` desde
`material.src` de un `<a-plane>` (`<a-plane material="src: #word-canvas">` +
`<canvas id="word-canvas">` en el DOM), forzando
`planeEl.getObject3D("mesh").material.map.needsUpdate = true` tras redibujar
si el motor no refresca solo. Es la forma declarativa de A-Frame para subir
una textura de canvas sin tocar `THREE.CanvasTexture` a mano, y es la misma
mecánica que sirve tanto para el Plan B de la sección 3 (tarjeta procedural)
como para el texto en sí — un solo canvas puede dibujar ambos (fondo de
tarjeta + palabra) o pueden ser dos planos superpuestos, a decidir en la
Fase C según resultado visual.

## 5. Posicionamiento y estabilidad de la tarjeta flotante

Dos enfoques evaluados:

**Objeto "pegado" a la cámara (HUD-3D)**: la tarjeta es hija de la entidad
`camera`/`a-camera`, a distancia y offset fijos. Siempre visible, siempre
centrada (o en una esquina, según diseño), simple de implementar (ya
descrito en la Opción A de la sección 2), sin dependencia de soporte de
hardware/WebXR.

**Anclado al mundo vía WebXR Anchors**: la tarjeta queda fija en un punto del
espacio real aunque el usuario se aleje o rote completamente; requiere
`navigator.xr` con soporte de `anchors` — ya documentado en la sección 2
como **no verificado confiablemente en WebView de Android** (supuesto a
validar en dispositivo real, no certeza).

**Recomendación: objeto pegado a la cámara (HUD-3D)**, por dos razones:

1. No depende de WebXR, cuyo soporte en el WebView objetivo es el punto más
   incierto de todo este plan (sección 2 y sección 6 de riesgos).
2. El target es hardware de gama baja. El repo ya tiene evidencia concreta
   de que ese hardware sufre con renderizado 3D continuo: los benchmarks de
   Rive en `docs/mascota-rive.md` (emulador `sdk_gphone64_x86_64`, Android
   16, **2 GB RAM con swap en uso, deliberadamente gama baja**) midieron
   entre 20 y 29 FPS según la escena (p95 de frame time hasta 101ms en la
   animación más pesada), **sin llegar a 60 fps**, atribuido en parte a que
   la GPU emulada no soporta fragment interlock y cae a un modo de
   renderizado más costoso. Si Rive solo (motor de vectores 2D optimizado)
   ya no llega a 60fps en ese perfil de hardware, agregarle world-tracking
   WebXR completo (SLAM, detección de planos) sobre A-Frame/WebGL en un
   WebView es un riesgo de rendimiento/batería mucho mayor que mantener el
   objeto simplemente pegado a la cámara (que no requiere tracking de mundo
   en absoluto, solo renderizar una entidad 3D estática relativa a la
   cámara). La opción HUD-3D es la que menos exige al hardware.

## 6. Flujo de datos end-to-end

No cambia de forma sustancial respecto al plan viejo; se verificó contra el
código real y sigue aplicando:

```
Profesor cambia de tarjeta/palabra (app del profesor)
  → Ably publica evento en el canal de la sala
  → LiveDomanRealtimeClient decodifica el payload Ably
  → LiveStudentRoomViewModel#applyRealtimeSession actualiza
    _uiState.session (incluye session.displayText)
  → StudentClassRoom (Compose) recalcula showingCard / session.displayText
  → [NUEVO] LaunchedEffect(session.displayText, webViewRef) detecta el
    cambio de texto
  → webView.evaluateJavascript(
      "window.WebAR && window.WebAR.updateCardWord(" +
      JSONObject.quote(session.displayText) + ");"
    )
  → [NUEVO] app.js expone window.WebAR.updateCardWord(word) (nuevo case
    "update-card-word" en receiveNativeMessage, mismo patrón que
    "activate-word-target")
  → ese handler redibuja el <canvas id="word-canvas"> con la palabra nueva
    y marca needsUpdate = true sobre la textura del plano de la tarjeta
    flotante (ya no anclado a <a-nft>, ahora hijo de la cámara)
```

Diferencia respecto al plan viejo: el anclaje ya no es un `a-nft`/`a-marker`
sino un `a-entity` hijo de la cámara (Opción A, sección 2) — el resto del
flujo (Ably → ViewModel → Compose → `evaluateJavascript` → handler JS →
canvas) es idéntico y ya está validado en el código real
(`native-bridge.js`, `app.js`, el patrón `outboundCommand` +
`LaunchedEffect` de `WebArScreen.kt:323-332`).

## 7. Fases nuevas

**Fase A — Mecanismo de AR sin marcador: vendorizado y escena base**

- Archivos: `webar/scripts/build.mjs` (eliminar las líneas que copian
  `aframe-ar-nft.js` y el comentario asociado; confirmar que `aframe.min.js`
  sigue copiándose, es la única dependencia AR necesaria para este flujo),
  `webar/src/index.html` o un nuevo documento HTML de escena
  (`webar/src/index-float.html` o similar, a decidir según si conviene un
  bundle separado del de "Temas") con `<a-scene>` + `<video>`/`getUserMedia`
  de fondo + `<a-camera>` sin `aframe-ar.js` cargado.
- Criterio de verificación independiente: abrir el documento en el WebView
  real (o `chrome://inspect` sobre el WebView con debugging habilitado) y
  confirmar que la cámara se ve de fondo y que no hay errores de consola por
  scripts AR.js faltantes/no usados.

**Fase B — Modelo 3D flotante (sin texto) en la escena ya conectada**

- Archivos: nuevo módulo JS (p. ej. `webar/src/js/floating-card-controller.js`)
  que cree la entidad `a-entity` hija de la cámara con el GLB (o el Plan B
  procedural de canvas, sección 3) en una posición/distancia fija; ajuste en
  `webar/src/index.html`/escena para instanciar el controller al iniciar.
- Criterio de verificación independiente: con `WebArContainer.kt` ya montado
  en `LiveStudentRoomScreen`, al mover el celular la tarjeta se mueve
  solidaria a la cámara (siempre centrada/en el mismo offset relativo), sin
  parpadeos ni pérdida de referencia — probar en el dispositivo de gama baja
  usado para los benchmarks de Rive, no solo en emulador de escritorio.

**Fase C — Texto dinámico estático hardcodeado**

- Archivos: `webar/src/js/card-text-controller.js` (nuevo, canvas 2D + plano
  `material.src`), ajuste de `floating-card-controller.js` para montar el
  plano de texto como hijo del mismo anchor.
- Criterio de verificación independiente: con un valor de palabra fijo en el
  código (sin backend), la palabra se ve dibujada sobre la tarjeta flotante
  al abrir la pantalla, con tipografía/contraste legibles sobre el fondo de
  cámara real (probar en exterior e interior, por brillo variable).

**Fase D — Conexión real a `session.displayText` / Ably**

- Archivos: `webar/src/js/app.js` (nuevo `case "update-card-word"` en
  `receiveNativeMessage`), `LiveStudentRoomScreen.kt`
  (`LaunchedEffect(session.displayText, webViewRef)` +
  `evaluateJavascript`), posiblemente `LiveStudentRoomViewModel.kt` si se
  decide modelar el envío como `outboundCommand` formal (recomendado, por
  consistencia con `WebArViewModel` y para evitar reenvíos duplicados en
  recomposición).
- Criterio de verificación independiente: con dos dispositivos (profesor
  cambiando de palabra, estudiante con la sala abierta), la palabra sobre la
  tarjeta flotante cambia en tiempo real sin recargar la pantalla del
  estudiante y sin re-descargar el GLB en cada cambio (confirmar que el
  cambio de palabra solo redibuja el canvas, no reinstancia la entidad 3D).

**Fase E — Pulido de posicionamiento/estabilidad y fallback**

- Archivos: `floating-card-controller.js` (suavizado de movimiento si el HUD
  se siente demasiado "nervioso" con el sensor de orientación),
  `LiveStudentRoomScreen.kt` (rama de fallback a texto plano si
  `getUserMedia`/WebGL no están disponibles, reusando
  `shouldMountArWebView`/`arUnavailable` ya existentes), chequeo explícito de
  soporte de `WebGL` (no solo de cámara) antes de montar la escena 3D.
- Criterio de verificación independiente: con cámara denegada o WebGL no
  disponible, la sala sigue siendo usable en modo texto plano sin
  regresión; con todo disponible, la tarjeta se siente estable (sin drift
  excesivo) durante al menos 60 segundos de uso continuo en el dispositivo
  de gama baja de referencia.

## 8. Riesgos y supuestos actualizados

- **Riesgo eliminado**: la cartulina sin suficientes features visuales para
  NFT **ya no aplica**. Era el riesgo "alto" principal del plan anterior
  (sección 9 de `plan-tarjeta-ar-dinamica.md`) y desaparece por completo al
  no haber cartulina física que trackear.
- **Riesgo nuevo y más incierto — soporte de WebXR en WebView de Android**:
  la evidencia de búsqueda web indica que `android.webkit.WebView` no expone
  `navigator.xr` de forma confiable (a diferencia de Chrome completo con
  ARCore desde la versión 79), pero **esto no se verificó directamente contra
  la versión de WebView instalada en los dispositivos de gama baja objetivo
  de este proyecto** — es un supuesto con confianza media basado en fuentes
  generales (BrowserStack, documentación de Android Developers sobre Android
  XR), no una prueba de laboratorio propia. **Marcado explícitamente como
  supuesto a validar en dispositivo real** antes de cualquier decisión futura
  de migrar a WebXR Anchors. Mitigación ya incorporada en el plan: la Opción
  A (sección 2) y la recomendación HUD-3D (sección 5) **no dependen de
  WebXR en absoluto**, así que este riesgo no bloquea ninguna fase actual —
  solo queda documentado para no asumir en el futuro que WebXR Anchors está
  disponible sin probarlo.
- **Riesgo medio — rendimiento en gama baja**: confirmado con evidencia
  propia del repo (benchmarks de Rive en `docs/mascota-rive.md`, 20-29 FPS en
  un perfil de 2GB RAM con swap). Mitigado por elegir HUD-3D simple en vez de
  world-tracking completo, pero sigue siendo necesario medir FPS real de la
  escena A-Frame + canvas en el mismo tipo de dispositivo antes de cerrar la
  Fase B.
- **Riesgo bajo — fallback si `getUserMedia` no está disponible**: ya existe
  el patrón `capabilities().camera` en `camera-controller.js` y
  `arUnavailable` en `WebArContainer.kt`/`LiveStudentRoomScreen.kt`; se
  reusa en la Fase E, no es trabajo nuevo de diseño.
- **Supuesto heredado del plan viejo, sigue vigente**: `session.displayText`
  llega sano desde Ably (sin HTML/caracteres de control) y se dibuja con
  `CanvasRenderingContext2D.fillText` (no inserción de HTML), por lo que no
  es vector de XSS, pero conviene acotar longitud máxima para no desbordar
  el área del canvas.
- **Supuesto nuevo**: no hay restricción de tamaño/escala física real que
  cumplir (a diferencia del plan viejo, que exigía que el GLB calzara con el
  tamaño de la cartulina física). Esto simplifica la Fase B pero significa
  que la escala final del modelo es una decisión puramente de diseño visual,
  no una restricción técnica — debe decidirse con criterio de UX, no de
  ingeniería.

## Checklist de aprobación

- [ ] ¿Se aprueba revertir el vendorizado de `aframe-ar-nft.js` en
      `webar/scripts/build.mjs` (sección 1), en vez de dejarlo documentado
      como no utilizado?
- [ ] ¿Se aprueba la Opción A (A-Frame + `getUserMedia` + objeto fijo
      hijo de la cámara) como mecanismo de AR sin marcador, en vez de
      invertir en validar soporte de WebXR Device API primero?
- [ ] ¿Se acepta empezar con el Plan B procedural (canvas con bisel/sombra)
      para el modelo de tarjeta, dejando el GLB de Sketchfab/Poly Pizza como
      mejora visual posterior y no como bloqueante de la Fase B?
- [ ] ¿Se acepta la recomendación HUD-3D (tarjeta pegada a la cámara) sobre
      anclaje de mundo vía WebXR Anchors, dado el riesgo de rendimiento en
      gama baja documentado con los benchmarks de Rive?
- [ ] ¿Es aceptable que el soporte real de `navigator.xr` en el WebView de
      los dispositivos objetivo quede como supuesto no verificado (a validar
      en dispositivo real en una fase posterior), sin que esto bloquee el
      plan actual porque ninguna fase depende de WebXR?
- [ ] ¿El orden de fases (A→E) propuesto en la sección 7 es el deseado, o se
      prefiere adelantar la medición de FPS en gama baja (hoy parte de la
      Fase B/E) a una fase 0 explícita antes de escribir cualquier código de
      escena?
- [ ] ¿Se acepta que este documento conviva con
      `docs/plan-tarjeta-ar-dinamica.md` como registro histórico sin
      editarlo, en vez de fusionar/reemplazar su contenido?

## Revisión técnica independiente

**Veredicto: APROBADO CON CAMBIOS**

### Verificado como correcto (contra código real, no contra la narrativa del plan)

1. **Vendorizado actual** (`webar/scripts/build.mjs`): confirmado que hoy
   copia `aframe.min.js`, `aframe-ar.js` y `aframe-ar-nft.js`, con el
   comentario citado por el plan sobre NFT presente tal cual. `index.html`
   carga solo `aframe.min.js` y `aframe-ar.js`; `aframe-ar-nft.js` nunca se
   referencia — el plan describe correctamente este archivo como peso muerto.
2. **`navigator.xr` ausente en Android WebView**: la afirmación del plan es
   correcta, y de hecho la evidencia encontrada en esta auditoría es *más
   fuerte* de lo que el plan reporta. Hay un issue abierto en el bug tracker
   oficial de **Chromium** (`issues.chromium.org/issues/40889430`, título
   "WebXR - avoid packaging libarcore_sdk_c.so into WebView as WebXR is
   unsupported there") que confirma, desde el propio proyecto Chromium, que
   WebXR **no está soportado en `android.webkit.WebView`** por diseño, no
   solo por falta de rollout. Esto eleva la confianza de "media" (como lo
   marca el plan) a **alta** — no es solo evidencia de blogs/BrowserStack,
   es el propio tracker de Chromium admitiendo la limitación de forma
   explícita. El plan subestima su propia certeza aquí.
3. **Mecanismo Opción A (A-Frame + `getUserMedia` + objeto hijo de cámara)**:
   técnicamente coherente. `camera-controller.js` (líneas 15-26) ya tiene
   exactamente el patrón de capability-check que el plan cita
   (`navigator.mediaDevices?.getUserMedia`, con chequeo adicional de
   `navigator.xr?.isSessionSupported?.("immersive-ar")` ya presente en el
   código — dato que el plan no menciona pero que es evidencia adicional a
   favor: el motor ya intenta sondear WebXR en runtime, lo que podría
   reusarse para el spike de validación en vez de escribir uno nuevo desde
   cero).
4. **Coexistencia con `WebArScreen.kt` (modo marcador Hiro/Kanji)**:
   confirmado que son instancias de `WebView` completamente separadas (cada
   composable crea su propio `AndroidView`/`WebView`), pero **ambas cargan
   hoy la misma URL hardcodeada** `WebArConfig.WEB_AR_URL` →
   `.../assets/webar/index.html` (constante única, no parametrizada). El
   plan menciona de pasada "un nuevo documento HTML... a decidir" pero
   **no señala que esto requiere tocar `WebArConfig.kt` y
   `webArDocumentUrl()`** para soportar una segunda URL/documento — hoy la
   función que resuelve la URL del WebAR es una constante única compartida
   por ambas pantallas. Si la Fase A termina creando `index-float.html`,
   es una tarea adicional no presupuestada en el plan, aunque de bajo
   riesgo (no modifica el documento que ya usa `WebArScreen.kt`).
5. **Revertir vendorizado de NFT — riesgo real detectado, el plan no lo
   contempla**: existe `webar/tests/build-vendor.test.mjs` con un test
   (`"build vendorizes aframe-ar-nft.js unmodified alongside the marker
   build"`) que hace *hash-compare* exigiendo que `aframe-ar-nft.js` siga
   siendo copiado byte a byte. **Si se revierte `build.mjs` sin tocar este
   test, el test rompe inmediatamente.** El plan (sección 1) habla de
   "borrar las 3 líneas de `copyFile`" pero no menciona el test ni pide
   actualizarlo/eliminarlo en el mismo cambio. Esto es un hueco concreto,
   no una opinión — verificado leyendo el archivo de test real.
6. **Benchmarks de Rive citados**: verificados contra
   `docs/mascota-rive.md` sección 6 — el rango 20-29 FPS, el p95 de 101ms
   (escena "celebrate ×3"), y el perfil de hardware (`sdk_gphone64_x86_64`,
   Android 16, 2GB RAM con swap) son citas **fieles y exactas**, no
   parafraseadas de forma tendenciosa. El plan usa esta evidencia de forma
   honesta.
7. **Fallback de `getUserMedia` denegado y contexto WebView-en-Compose**:
   confirmado que `shouldMountArWebView(showingCard, hasCameraHardware,
   arUnavailable)` y el fallback a `Surface`/`Text` de texto plano ya
   existen en `LiveStudentRoomScreen.kt`/`WebArContainer.kt`, y que el
   permiso de cámara (`onPermissionRequest`) ya está resuelto ahí mismo
   (chequeo de origen confiable + permiso Android). El plan asume
   correctamente ese trabajo previo en vez de duplicarlo — no hay
   sobre-ingeniería aquí.

### Corregido / señalado como inexacto

- **Sección 2, "A-Frame 1.6 ya vendorizado" / afirmación implícita de "cero
  vendorizado nuevo"**: no existe ningún directorio `webar/src/vendor/` en
  el árbol fuente — los archivos vendorizados viven únicamente en el
  artefacto de build `app/src/main/assets/webar/vendor/`, generado por
  `build.mjs` desde `node_modules`. La distinción importa poco para la
  decisión técnica (igual no hace falta vendorizar nada *nuevo*), pero si
  el plan en algún punto llega a decir textualmente "ya está en
  `webar/src/vendor/`" debe corregirse esa ruta para no inducir a buscar un
  directorio que no existe.
- **Riesgo "WebXR no verificado" — nivel de confianza**: como se detalla en
  el punto 2 arriba, el plan es *demasiado* conservador marcando esto como
  "confianza media". La evidencia de un issue de Chromium explícito sobre
  la ausencia deliberada de ARCore en WebView es evidencia de alta
  confianza. Esto no cambia la decisión (Opción A sigue siendo correcta)
  pero sí cambia la urgencia del spike: si la confianza ya es alta, el
  spike de validación en dispositivo real pasa de "nice to have" a
  "confirmación barata de algo casi seguro", no a una investigación
  abierta — argumento más fuerte todavía para incluirlo como paso 1 de la
  Fase A (ver recomendación abajo), porque cuesta 10 minutos y cierra el
  único punto de incertidumbre real del plan con evidencia de primera mano
  del dispositivo objetivo.

### No verificable en esta auditoría

- Las URLs de Sketchfab/Poly Pizza para los candidatos de modelo 3D
  (sección 3) siguen sin confirmación manual — ya lo señala el plan mismo,
  consistente con lo marcado en la revisión del plan viejo. No se intentó
  verificar en esta auditoría (fuera de alcance: requiere navegación a
  sitios de terceros, no es una afirmación sobre el código del repo).
- Comportamiento real en el dispositivo de gama baja de referencia (FPS de
  la escena A-Frame + canvas, estabilidad del HUD-3D con sensores de
  orientación) — no verificable sin ejecutar en hardware; el plan ya lo
  reconoce correctamente como pendiente de medición en Fase B/E.

### Fases autorizadas para ejecución inmediata

- **Fase A puede arrancar en paralelo sin esperar el spike de WebXR**: la
  decisión de Opción A (sección 2) ya está tomada y no depende del
  resultado de `navigator.xr` — es correcto que el plan no bloquee la fase
  a esa validación. Sin embargo, dado que la confirmación es barata (10
  minutos) y que ya existe en `camera-controller.js` un sondeo
  `navigator.xr?.isSessionSupported?.("immersive-ar")` reusable,
  **recomiendo que la Fase A incluya ese spike como su primer paso
  verificable** (cargar el documento en el WebView real del proyecto,
  loguear `!!navigator.xr` y el resultado de `capabilities()`, antes de
  escribir el controller de la tarjeta) en vez de dejarlo como nota al
  pie en la sección de riesgos. No porque el plan esté mal fundamentado,
  sino porque es la clase de evidencia de primera mano que cierra la
  última duda del plan al costo más bajo posible, y documenta con
  certeza (no con "confianza media") la base de toda la sección 5.

### Cambios obligatorios antes de ejecutar

1. **Fase A debe incluir, además de lo ya descrito, la actualización (o
   eliminación explícita) de `webar/tests/build-vendor.test.mjs`** en el
   mismo cambio que revierte el vendorizado de `aframe-ar-nft.js` en
   `build.mjs`. Revertir uno sin el otro deja el test suite roto.
2. **Fase A debe incluir el spike de `navigator.xr`/`capabilities()`
   como paso explícito y verificable**, no como nota de riesgo a validar
   "en algún momento futuro" — ver justificación arriba.
3. Si la Fase A termina creando un documento HTML separado
   (`index-float.html`), debe presupuestar el cambio en
   `WebArConfig.kt`/`webArDocumentUrl()` (hoy una constante única
   compartida con `WebArScreen.kt`) como parte del alcance de la fase, no
   como detalle a resolver sobre la marcha.

Con estos tres cambios, las Fases A-E del plan son ejecutables en el orden
propuesto; ninguna depende de resolver la incertidumbre de WebXR antes de
empezar.
