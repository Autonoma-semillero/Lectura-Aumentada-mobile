# InclusiveReadingAR (Android)

Initial Android project scaffold for the university initiative **Inclusive Reading AR**.

## Stack
- Kotlin
- Jetpack Compose
- MVVM (single `app` module)
- Embedded learning activity rendered with `WebView` (internal implementation detail)

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

## Current behavior
- User-facing screen focused on inclusive reading practice.
- Local activity fallback in `assets/reading_activity.html` so the app works without backend deployment.
- Safe navigation guard in `WebView` to block non-allowed URLs.

## Tests included
- `MockLearningRepositoryTest`
- `WebArViewModelTest`
- `MainActivityTest`

## Notes
- `WebArConfig` keeps placeholders for future NestJS and remote activity integration.
- Gradle wrapper is included and ready to run.
