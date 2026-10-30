package com.vista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vista.data.model.OnboardingPreferences
import com.vista.ui.screens.HomeScreen
import com.vista.ui.screens.OnboardingScreen
import com.vista.ui.screens.GameScreen
import com.vista.ui.theme.VistaTheme
import kotlinx.coroutines.launch
import com.vista.ui.screens.StoryDetailsScreen
import com.vista.ui.screens.StoryIntroScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = OnboardingPreferences(applicationContext)

        var isLoading by mutableStateOf(true)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isLoading }

        super.onCreate(savedInstanceState)

        setContent {
            VistaTheme {
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
                                    currentScreen = "intro"
                                },
                                onBack = {
                                    currentScreen = "home"
                                }
                            )

                            "intro" -> StoryIntroScreen(
                                onStartDialogue = { currentScreen = "game" },
                                onBack = { currentScreen = "details" }
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
