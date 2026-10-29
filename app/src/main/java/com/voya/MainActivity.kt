package com.voya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voya.data.model.OnboardingPreferences
import com.voya.ui.screens.HomeScreen
import com.voya.ui.screens.OnboardingScreen
import com.voya.ui.screens.GameScreen
import com.voya.ui.theme.VoyaTheme
import kotlinx.coroutines.launch
import com.voya.ui.screens.StoryDetailsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = OnboardingPreferences(applicationContext)

        var isLoading by mutableStateOf(true)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isLoading }

        super.onCreate(savedInstanceState)

        setContent {
            VoyaTheme {
                val scope = rememberCoroutineScope()

                val completed by preferences
                    .isOnboardingCompleted
                    .collectAsStateWithLifecycle(
                        initialValue = null
                    )

                LaunchedEffect(completed) {
                    if (completed != null) {
                        isLoading = false
                    }
                }

                var currentScreen by rememberSaveable {
                    mutableStateOf("home")
                }

                when (completed) {
                    null -> {
                        // Splash screen remains visible.
                    }

                    false -> {
                        OnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    preferences.completeOnboarding()
                                }
                            }
                        )
                    }

                    true -> {
                        when (currentScreen) {
                            "home" -> HomeScreen(
                                onStartGame = {
                                    currentScreen = "details"
                                }
                            )

                            "details" -> StoryDetailsScreen(
                                onPlay = {
                                    currentScreen = "game"
                                },
                                onBack = {
                                    currentScreen = "home"
                                }
                            )

                            "game" -> GameScreen(
                                onExit = { currentScreen = "details" }
                            )
                        }
                    }
                }
            }
        }
    }
}
