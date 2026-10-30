package com.vista.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vista.ui.theme.VistaColors

@Composable
fun GameScreen(onExit: () -> Unit = {}) {
    val model: GameViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(model) { model.prepareOpening(context.cacheDir) }
    val exit = {
        model.reset()
        onExit()
    }
    BackHandler(onBack = exit)
    key(state.phase, state.introIndex, state.audioFile) {
        when (state.phase) {
            GamePhase.INTRO -> {
                val (audio, text) = model.intro[state.introIndex]
                FbiDialogueScreen(audioResId = audio, dialogueText = text,
                    onAudioFinished = model::introFinished, onBack = exit)
            }
            GamePhase.RECORDING -> MayaRecordingScreen(
                questionText = state.question, autoStart = false, onBack = exit,
                onSilentResponse = { model.answer(null, context.cacheDir) },
                onRecordingFinished = { model.answer(it, context.cacheDir) }
            )
            GamePhase.REPLY -> {
                val segment = state.reply?.segments?.getOrNull(state.segmentIndex)
                FbiDialogueScreen(
                    audioFile = state.audioFile, dialogueText = segment?.text.orEmpty(),
                    speaker = segment?.speaker ?: "Mercer",
                    generatedVoice = segment?.kind != "sound_effect", segmentKind = segment?.kind ?: "dialogue",
                    suspicion = if (state.opening) null else state.suspicion,
                    onAudioFinished = model::replyFinished, onBack = exit
                )
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().background(VistaColors.Background)
                    .statusBarsPadding().navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state.phase) {
                    GamePhase.OPENING, GamePhase.PROCESSING -> {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(24.dp))
                        Text(if (state.opening) "Preparing the scene..." else "Mercer is considering your answer...", color = VistaColors.TextPrimary,
                            textAlign = TextAlign.Center)
                    }
                    GamePhase.ERROR -> {
                        Text(state.error ?: "Could not process your answer.", color = VistaColors.TextPrimary,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { model.retry(context.cacheDir) }) {
                            Text(if (state.opening) "Retry narration" else "Retry answer")
                        }
                        if (!state.opening) TextButton(onClick = model::recordAgain) { Text("Record again") }
                    }
                    GamePhase.COMPLETE -> {
                        Text(when (state.reply?.action) {
                            "GameOver" -> "Game over"
                            "TakeToStation" -> "Taken to the station"
                            else -> "Interrogation complete"
                        },
                            style = MaterialTheme.typography.headlineMedium, color = VistaColors.TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        if (state.reply?.action == "TakeToStation") {
                            Text("Mercer has ordered Maya's transfer to the police station.",
                                color = VistaColors.TextSecondary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                        }
                        Text("Suspicion: ${state.suspicion}/100", color = VistaColors.Gold)
                        Text(state.reply?.reason.orEmpty(), color = VistaColors.TextSecondary,
                            textAlign = TextAlign.Center)
                    }
                    else -> Unit
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = exit) { Text("Back to story") }
            }
        }
    }
}
