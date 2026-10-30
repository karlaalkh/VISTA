package com.voya.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.voya.R
import com.voya.ui.components.AudioWaveform
import com.voya.ui.components.CharacterSceneImage
import com.voya.ui.theme.VoyaColors
import java.io.File

@Composable
fun FbiDialogueScreen(
    audioResId: Int = R.raw.question_1,
    dialogueText: String = "Where is your father?",
    audioFile: File? = null,
    generatedVoice: Boolean = false,
    speaker: String = "Mercer",
    segmentKind: String = "dialogue",
    suspicion: Int? = null,
    onAudioFinished: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onAudioFinished)
    val lifecycleOwner = LocalLifecycleOwner.current
    var playbackFailed by remember(audioResId, audioFile) { mutableStateOf(false) }
    var retryCount by remember(audioResId, audioFile) { mutableIntStateOf(0) }

    DisposableEffect(context, audioResId, audioFile, lifecycleOwner, retryCount) {
        playbackFailed = false
        val player = if (audioFile == null) MediaPlayer.create(context, audioResId) else {
            val localPlayer = MediaPlayer()
            try {
                localPlayer.setDataSource(audioFile.absolutePath)
                localPlayer.prepare()
                localPlayer
            } catch (_: Exception) {
                localPlayer.release()
                null
            }
        }
        var finished = false

        player?.setOnCompletionListener {
            if (!finished) {
                finished = true
                latestOnFinished()
            }
        }
        player?.setOnErrorListener { _, _, _ ->
            playbackFailed = true
            true
        }
        if (player == null) playbackFailed = true

        val observer = LifecycleEventObserver { _, event ->
            if (!finished && !playbackFailed) {
                when (event) {
                    Lifecycle.Event.ON_START -> player?.start()
                    Lifecycle.Event.ON_STOP -> if (player?.isPlaying == true) player.pause()
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            player?.start()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoyaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (speaker == "Drone") {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.SmartToy, contentDescription = "Security drone",
                        tint = VoyaColors.VoiceBlue, modifier = Modifier.size(112.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("DRONE SCAN", color = VoyaColors.VoiceBlue, letterSpacing = 3.sp)
                }
            } else if (speaker == "Narrator") {
                CharacterSceneImage(imageRes = R.drawable.tye_intro, modifier = Modifier.fillMaxSize())
            } else {
                CharacterSceneImage(imageRes = R.drawable.fbi_speaking, modifier = Modifier.fillMaxSize())
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text("✕", color = Color.White, fontSize = 22.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = speaker.uppercase(),
                color = VoyaColors.Gold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))
            if (generatedVoice) {
                Text("AI-generated voice", color = VoyaColors.TextSecondary, fontSize = 12.sp)
            }
            suspicion?.let {
                Text("Suspicion: $it/100", color = VoyaColors.Gold, fontSize = 12.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VoyaColors.Surface)
                    .padding(20.dp)
            ) {
                Text(
                    text = dialogueText,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            AudioWaveform(
                active = !playbackFailed,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    playbackFailed -> "Could not play this audio."
                    segmentKind == "sound_effect" -> "Scanning..."
                    speaker == "Narrator" -> "Narrating the scene..."
                    else -> "$speaker speaking..."
                },
                color = VoyaColors.TextSecondary,
                fontSize = 13.sp
            )
            if (playbackFailed) {
                TextButton(onClick = { retryCount++ }) { Text("Retry audio") }
                TextButton(onClick = { latestOnFinished() }) { Text("Continue with captions") }
            }
        }
    }
}
