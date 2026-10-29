package com.voya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voya.ui.components.EmpathyLens
import com.voya.ui.theme.VoyaColors

@Composable
fun HomeScreen(
    onStartGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VoyaColors.Background,
                        VoyaColors.MidnightBlue,
                        VoyaColors.Background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            EmpathyLens(
                modifier = Modifier.size(
                    width = 220.dp,
                    height = 140.dp
                )
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "V O Y A",
                color = VoyaColors.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 5.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Your voice. Your story.",
                color = VoyaColors.Gold,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Step into another perspective.",
                color = VoyaColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoyaColors.Gold,
                    contentColor = VoyaColors.Background
                )
            ) {
                Text(
                    text = "Start Playing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
