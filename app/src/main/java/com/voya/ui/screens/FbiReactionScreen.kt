package com.voya.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voya.R
import com.voya.ui.theme.VoyaColors

@Composable
fun FbiReactionScreen(
    onContinue: () -> Unit,
    audioResId: Int = R.raw.listen,
    dialogueText: String = "Listen carefully."
) {
    val context = LocalContext.current
    var audioFinished by remember(audioResId) {
        mutableStateOf(false)
    }

    DisposableEffect(context, audioResId) {
        val player = MediaPlayer.create(
            context,
            audioResId
        )

        if (player == null) {
            audioFinished = true
        } else {
            player.setOnCompletionListener {
                audioFinished = true
            }
            player.start()
        }

        onDispose {
            player?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoyaColors.Background)
    ) {
        Image(
            painter = painterResource(R.drawable.fbi_speaking),
            contentDescription = "FBI agent",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "THROUGH YOUR EYES",
                color = Color.White,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "FBI AGENT",
                color = VoyaColors.Gold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        VoyaColors.Surface.copy(alpha = 0.92f)
                    )
                    .padding(22.dp)
            ) {
                Text(
                    text = dialogueText,
                    color = Color.White,
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (audioFinished) {
                        "●  AUDIO COMPLETE"
                    } else {
                        "●  AGENT SPEAKING"
                    },
                    color = VoyaColors.VoiceBlue,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(26.dp))

            Button(
                onClick = onContinue,
                enabled = audioFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoyaColors.Gold,
                    contentColor = VoyaColors.Background
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}