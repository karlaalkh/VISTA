# VISTA

Interactive audio experience for EIT Culture & Creativity. The game now interprets the player's spoken answers as Maya and generates English replies voiced as Mercer. See [AI dialogue setup](server/README.md) for server credentials, emulator/device connections, dialogue rules and verification.

Kotlin Android starter using Jetpack Compose, Material 3, and MVVM. Package/application ID: `com.example.vista`. Minimum Android version: Android 8.0 (API 26).

## Open and run

1. Open this directory in Android Studio and sync the Gradle project.
2. Use the bundled Android Studio JDK (JDK 17 or newer) for Gradle. Install Android SDK 36 and Build Tools 36.0.0 if prompted.
3. Select an emulator or device and run the `app` configuration.

From PowerShell with `JAVA_HOME` pointing to your JDK:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Structure

```text
app/src/main/java/com/example/vista/
  MainActivity.kt              Activity entry point; hosts Compose
  VistaApplication.kt           Owns application-scoped dependencies
  di/AppContainer.kt          Creates and connects repository implementations
  data/model/                 Plain Kotlin data models
  data/repository/            Data contracts and implementations
  ui/home/
    HomeUiState.kt            Immutable screen state
    HomeViewModel.kt          Exposes state and handles screen actions
    HomeScreen.kt             Lifecycle-aware route and stateless screen
  ui/theme/                   Compose colors and theme
```

The example follows `Screen -> ViewModel -> Repository`; repository changes flow back through `StateFlow` to the screen. The screen collects state with `collectAsStateWithLifecycle`. The ViewModel receives its repository through its constructor and has no Activity or Context dependency. `AppContainer` provides manual dependency injection without an additional framework.

The counter is deliberately stored in memory. It survives rotation while the process lives and resets after process death. Replace its repository with Room, DataStore, or an API implementation when you need persistent data. If a future data source can fail, add loading/error states and handle those failures in the ViewModel before connecting it to the UI.

## Add a feature

1. Add its data models and repository contract under `data/`.
2. Implement the repository and wire it in `AppContainer`.
3. Create `ui/<feature>/<Feature>UiState`, `<Feature>ViewModel`, and `<Feature>Screen`.
4. Expose immutable `StateFlow` state from the ViewModel and accept user actions as methods. Keep database/network access behind the repository.
5. Collect state in a route composable, then pass state and callbacks to a stateless screen. Connect the route from `MainActivity` (add navigation when you have multiple destinations).
6. Add ViewModel tests using a fake repository and repository tests for data behavior. Existing tests demonstrate both.

Add a `domain/` layer with use cases when business logic becomes complex or is reused across ViewModels. Change `namespace`, `applicationId`, and Kotlin packages when you choose your final application ID.

This structure follows the [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations). AGP 9 uses built-in Kotlin; only the Compose compiler plugin is applied separately. Dependency versions are centralized in `gradle/libs.versions.toml`.
