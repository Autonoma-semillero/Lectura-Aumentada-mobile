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

## Notes

- Gradle wrapper is included and ready to run.
- Release builds reject cleartext traffic; AR assets must use HTTPS.
