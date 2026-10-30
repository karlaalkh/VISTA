package com.vista.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vista.R
import com.vista.ui.theme.VistaColors

@Composable
fun StoryDetailsScreen(
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VistaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top navigation
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 8.dp
            )
        ) {
            Text(
                text = "‹ Back",
                color = Color.White
            )
        }

        Spacer(Modifier.height(8.dp))

        // Image section: full artwork, no cropping
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(VistaColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.tye_intro),
                contentDescription = "Through Your Eyes cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(24.dp))

        // Story information
        Column(
            modifier = Modifier.padding(
                horizontal = 26.dp
            )
        ) {
            Text(
                text = "Through\nYour Eyes",
                color = Color.White,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "THRILLER  ·  INTERACTIVE AUDIO",
                color = VistaColors.Gold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "A cinematic thriller about control, " +
                        "responsibility and what remains of a person " +
                        "when others begin to shape their perception.",
                color = VistaColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VistaColors.Gold,
                    contentColor = VistaColors.Background
                )
            ) {
                Text(
                    text = "Play Experience",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}