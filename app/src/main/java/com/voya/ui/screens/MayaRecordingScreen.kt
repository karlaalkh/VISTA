package com.voya.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.voya.R
import com.voya.audio.VoiceRecorder
import com.voya.ui.components.AudioWaveform
import com.voya.ui.components.CharacterSceneImage
import com.voya.ui.theme.VoyaColors
import java.io.File

@Composable
fun MayaRecordingScreen(
    questionText: String? = null,
    autoStart: Boolean = false,
    onBack: (() -> Unit)? = null,
    onSilentResponse: (() -> Unit)? = null,
    onRecordingFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val recorder = remember(context) {
        VoiceRecorder(context)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun startRecording() {
        errorMessage = null
        isRecording = recorder.start()
        if (!isRecording) errorMessage = "Could not start recording."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            errorMessage = "Microphone permission denied."
        }
    }

    LaunchedEffect(autoStart) {
        if (autoStart) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(recorder, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isRecording) {
                recorder.release()
                isRecording = false
                errorMessage = "Recording interrupted. Tap to answer again."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recorder.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoyaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("Back to story") }
        }
        CharacterSceneImage(
            imageRes = R.drawable.maya_listening,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR TURN",
                color = VoyaColors.Gold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(14.dp))
            Text("Answer as Maya in English. Your recording is sent for AI processing.",
                color = VoyaColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))

            questionText?.let {
                Text(it, color = VoyaColors.TextSecondary, fontSize = 16.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = if (isRecording) {
                    "I'm listening..."
                } else {
                    "Speak naturally to answer."
                },
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            AudioWaveform(
                active = isRecording
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRecording) "Tap to stop recording" else "Tap to speak",
                color = VoyaColors.TextPrimary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isRecording) {
                        val file = recorder.stop()
                        isRecording = false

                        if (file != null) {
                            onRecordingFinished(file)
                        } else {
                            errorMessage = "Recording failed."
                        }
                    } else {
                        errorMessage = null

                        val hasPermission =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            isRecording = recorder.start()
                            if (!isRecording) {
                                errorMessage = "Could not start recording."
                            }
                        } else {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    }
                },
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoyaColors.VoiceBlue,
                    contentColor = VoyaColors.Background
                )
            ) {
                Icon(
                    imageVector = if (isRecording) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Mic
                    },
                    contentDescription = if (isRecording) {
                        "Stop recording"
                    } else {
                        "Start recording"
                    },
                    modifier = Modifier.size(40.dp)
                )
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    color = VoyaColors.Error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            if (!isRecording && onSilentResponse != null) {
                TextButton(onClick = onSilentResponse) { Text("Remain silent") }
            } else if (onSilentResponse != null) {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
