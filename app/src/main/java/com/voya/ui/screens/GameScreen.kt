package com.voya.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voya.data.model.InterrogationState

@Composable
fun GameScreen(onExit: () -> Unit = {}) {
    var lineIndex by rememberSaveable { mutableIntStateOf(0) }
    var awaitingResponse by rememberSaveable { mutableStateOf(false) }
    val state = InterrogationState(lineIndex, awaitingResponse)
    val line = state.currentLine

    fun update(next: InterrogationState) {
        lineIndex = next.lineIndex
        awaitingResponse = next.awaitingResponse
    }

    BackHandler(onBack = onExit)

    key(lineIndex, awaitingResponse) {
        when {
            line == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Experience complete", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onExit) { Text("Back to story") }
            }
            awaitingResponse && line.expectsResponse -> MayaRecordingScreen(
                questionText = line.caption,
                autoStart = true,
                onBack = onExit,
                onRecordingFinished = { update(state.responseRecorded()) }
            )
            else -> FbiDialogueScreen(
                audioResId = line.audioResId,
                dialogueText = line.caption,
                onAudioFinished = { update(state.audioFinished()) },
                onBack = onExit
            )
        }
    }
}
