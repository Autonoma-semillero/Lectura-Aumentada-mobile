# InclusiveReadingAR (Android)

Android application for the university initiative **Inclusive Reading AR**.

## Stack
- Kotlin
- Jetpack Compose
- MVVM (single `app` module)
- Embedded marker-based WebAR with A-Frame + AR.js

## Base setup
- `applicationId`: `co.edu.uniautonoma.inclusivereadingar`
- `minSdk`: 26
- `targetSdk` / `compileSdk`: 36
- Java 21

## Architecture
- `presentation`: UI, navigation, ViewModel
- `domain`: models, repository contract, use cases
- `data`: mock repository and mappers
- `config`: API and activity URL placeholders

## WebAR

- The student opens a full-screen scanner from the Themes screen.
- Camera access starts only after pressing `Activar cámara`.
- Marker events are resolved through the authenticated backend endpoint
  `GET /api/assets/marker/:markerId` without exposing the JWT to JavaScript.
- GLB and audio lifecycle are isolated and cleaned when the marker or screen changes.

See [`webar/README.md`](webar/README.md) for build and physical-device verification.

## Tests included
- `MockLearningRepositoryTest`
- `WebArViewModelTest` and WebAR Node tests
- `MainActivityTest`

## Análisis estático (SonarCloud)

`.github/workflows/sonar.yml` analiza el repositorio en cada push y PR a
`main`/`master`/`develop`. La cobertura la produce `createDebugUnitTestCoverageReport`
(JaCoCo, habilitado con `enableUnitTestCoverage` en el build type `debug`) y Sonar la
lee de `app/build/reports/coverage/test/debug/report.xml`.

Quedan fuera del análisis `app/src/main/assets/webar/**` (bundle generado desde
`webar/src`; analizarlo duplicaría hallazgos) y `webar/node_modules/**`.

Si `SONAR_TOKEN` no está configurado, el workflow avisa y se omite en vez de fallar.

### Puesta en marcha (una sola vez)

1. En [sonarcloud.io](https://sonarcloud.io), crear la organización a partir de
   `Autonoma-semillero` e importar este repositorio.
2. Si el `projectKey` o la `organization` que genera SonarCloud no coinciden con los
   de `build.gradle.kts`, corregirlos ahí.
3. *Analysis Method* → **GitHub Actions**, copiar el token.
4. Guardarlo en *Settings → Secrets and variables → Actions* como **`SONAR_TOKEN`**.
5. Desactivar *Automatic Analysis* en SonarCloud: es incompatible con el análisis
   por CI.

## Notes

- Gradle wrapper is included and ready to run.
- Release builds reject cleartext traffic; AR assets must use HTTPS.
