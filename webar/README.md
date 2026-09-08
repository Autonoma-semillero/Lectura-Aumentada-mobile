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
- Kotlin: autenticación, consulta de API, caché de sesión y entrega del DTO saneado.

El JWT permanece en la capa Kotlin. El contenido web solamente envía un
`markerId` y recibe las URLs HTTPS autorizadas del modelo y del audio.

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
