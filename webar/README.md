# Motor WebAR

Módulo web embebido por la aplicación Android. El bundle usa A-Frame 1.6 y
AR.js 3.4.8, se genera dentro de `app/src/main/assets/webar` y se carga mediante
`WebViewAssetLoader` con el origen seguro `https://appassets.androidplatform.net`.

## Responsabilidades

- `camera-controller.js`: compatibilidad, inicio explícito y cierre de tracks.
- `detection-controller.js`: eventos de marcador, tolerancia a pérdidas breves y deduplicación.
- `native-bridge.js`: contrato de mensajes con Android y validación de activos.
- `model-controller.js`: ciclo de vida del GLB y liberación de GPU.
- `audio-controller.js`: reproducción, repetición accesible, volumen y exclusión mutua.
- `word-target.js`: normalización, deduplicación y correlación de objetivos OCR.
- Kotlin: autenticación, consulta de API, caché de sesión y entrega del DTO saneado.

El JWT permanece en la capa Kotlin. El contenido web solamente emite el estado
de cámara y los `markerId`; recibe objetivos OCR y las URLs HTTPS autorizadas
del modelo y del audio.

## Contrato de objetivos por palabra

Android activa la palabra reconocida con
`{"type":"activate-word-target","word":"Árbol","centerX":0.48,"centerY":0.61}` y la retira con
`{"type":"clear-word-target","word":"Árbol"}`. El campo `word` de limpieza
es opcional; cuando está presente, WebAR ignora limpiezas obsoletas que no
coincidan con el objetivo actual. La comparación ignora mayúsculas, acentos y
espacios repetidos, pero conserva `ñ` como una letra distinta de `n`.
`centerX` y `centerY` son opcionales, normalizados entre 0 y
1, se limitan a ese intervalo y se usa 0.5 cuando faltan. Observaciones sucesivas
de la misma palabra actualizan la posición sin descargar el modelo. El GLB queda
ligeramente por encima del centro detectado para no tapar la palabra que el OCR
necesita seguir viendo.

Después de activar el objetivo, Android entrega el mismo mensaje `asset-ready`
existente. WebAR lo correlaciona por `asset.word` y monta el GLB en un root fijo
frente a la cámara, sin exigir Hiro o Kanji. Cuando no hay una palabra activa,
`asset-ready` conserva la correlación por `asset.markerId` de los marcadores.
Para activar y entregar sin una carrera entre comandos, se admite el formato
atómico `{"type":"asset-ready","asset":{...},"target":{"type":"word","centerX":0.48,"centerY":0.61}}`;
en él la palabra se toma de `asset.word`.
Si la consulta no encuentra una asociación, Android envía
`{"type":"word-not-found","word":"Árbol","target":{"type":"word","centerX":0.48,"centerY":0.61}}`.
La metadata activa y posiciona el objetivo de forma atómica; si se omite, WebAR
solo aplica la respuesta cuando todavía coincide con la palabra activa, para
descartar resultados atrasados.

WebAR emite `{"type":"camera-ready"}` hacia Android una vez que AR.js confirma
la cámara. Antes de emitirlo oculta su panel de diagnóstico para que PixelCopy no
lo entregue al OCR; lo restaura al detenerse o mostrar un error. El host debe
iniciar el reconocimiento OCR después de ese evento.

Cuando A-Frame termina de cargar el GLB, WebAR emite `{"type":"model-ready"}`.
Si falla, emite `{"type":"model-error","message":"..."}` sin cerrar la cámara
ni detener el OCR, de modo que el panel nativo pueda mostrar el resultado.
El botón de reintento de la tarjeta de error emite
`{"type":"camera-retry-requested"}`; Android desmonta y recarga el documento
antes de volver a activar la cámara, evitando reutilizar una sesión AR.js dañada.

## Desarrollo

```bash
cd webar
npm ci
npm run check
```

`npm run build` copia el código y las dependencias fijadas hacia los assets del
APK. Los archivos generados se versionan para que Gradle no dependa de Node al
compilar la aplicación.

## Marcadores de prueba

| Imagen | `marker_id` enviado al backend |
|---|---|
| `src/markers/hiro.png` | `demo-animales-gato` |
| `src/markers/kanji.png` | `demo-animales-perro` |

Las imágenes proceden del repositorio oficial de AR.js y se incluyen solamente
como fixtures reproducibles. Imprímelas sin recortar el borde negro y evita
reflejos directos.

`demo-animales-gato` usa el modelo CC0 `animal-cat.glb` de Kenney Cube Pets,
incluido en `app/src/main/assets/models/animals`. La URL almacenada en MongoDB
apunta al origen seguro local de `WebViewAssetLoader`, por lo que el modelo no
depende de una descarga de red durante la experiencia AR.

## Verificación manual

1. Configura `backendBaseUrl` en `local.properties` y levanta el backend con una
   cuenta de estudiante y unidades `demo-animales-gato`/`demo-animales-perro`.
2. Asegura que `model_3d` y `audio_pronunciacion` sean URLs HTTPS accesibles y
   permitan CORS desde `https://appassets.androidplatform.net`.
3. Instala la app en un dispositivo Android físico con System WebView actualizado.
4. Inicia sesión, pulsa el icono de escaneo en Temas y luego `Activar cámara`.
5. Acepta el permiso y enfoca `hiro.png`; verifica una sola consulta, GLB alineado
   y audio. Usa `Repetir audio` para comprobar accesibilidad.
6. Retira el marcador: modelo y audio deben desaparecer. Enfoca `kanji.png` para
   comprobar el reemplazo sin superposición.
7. Cierra la pantalla y verifica desde el indicador de privacidad de Android que
   la cámara deja de estar en uso.
8. Repite rechazando el permiso, usando un marcador sin asociación y una URL de
   activo inválida para comprobar los estados controlados.

La detección óptica y el permiso real no se automatizan de forma fiable en JVM;
por ello se cubren con esta prueba física y con tests unitarios del gate, contrato,
caché y estados.
