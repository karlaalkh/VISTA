package com.voya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.voya.ui.components.AnimatedEmpathyLens
import com.voya.ui.theme.VoyaColors
import com.voya.ui.theme.VoyaTheme

@Composable
fun WelcomeScreen(
    onStartPlaying: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VoyaColors.Background,
                        VoyaColors.MidnightBlue,
                        VoyaColors.Background,
                        Color.Black
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(290.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            VoyaColors.VoiceBlue.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            AnimatedEmpathyLens(
                modifier = Modifier.size(
                    width = 220.dp,
                    height = 140.dp
                )
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "V O Y A",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Your voice. Your story.",
                style = MaterialTheme.typography.bodyLarge,
                color = VoyaColors.TextSecondary
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Step inside the story.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "An interactive audio experience\n" +
                        "where your words change everything.",
                style = MaterialTheme.typography.bodyMedium,
                color = VoyaColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onStartPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Start Playing",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    VoyaTheme {
        WelcomeScreen(onStartPlaying = {})
    }
}
