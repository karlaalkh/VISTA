package com.vista.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vista.ui.theme.VistaColors
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(
    onComplete: () -> Unit
) {
    val latestOnComplete by rememberUpdatedState(onComplete)

    val transition = rememberInfiniteTransition(
        label = "processing"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    // Demo only: processing lasts 2 seconds.
    LaunchedEffect(Unit) {
        delay(2000)
        latestOnComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VistaColors.Background,
                        VistaColors.MidnightBlue,
                        VistaColors.Background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Canvas(
                modifier = Modifier.size(110.dp)
            ) {
                val stroke = 5.dp.toPx()

                drawCircle(
                    color = VistaColors.VoiceBlue.copy(
                        alpha = 0.16f
                    ),
                    radius = size.minDimension / 2f - stroke,
                    style = Stroke(width = stroke)
                )

                drawArc(
                    color = VistaColors.VoiceBlue,
                    startAngle = rotation,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )
            }

            Spacer(Modifier.height(34.dp))

            Text(
                text = "Analyzing your response...",
                color = Color.White,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "The story is listening.",
                color = VistaColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}