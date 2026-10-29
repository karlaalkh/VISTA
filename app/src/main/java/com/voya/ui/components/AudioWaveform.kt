package com.voya.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.voya.ui.theme.VoyaColors
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioWaveform(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "waveform"
    )

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1200,
                easing = LinearEasing
            )
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val count = 45
        val spacing = size.width / count
        val centerY = size.height / 2f

        repeat(count) { index ->
            val wave = abs(
                sin(index * 0.48f + if (active) phase else 0f)
            )

            val height = if (active) {
                6.dp.toPx() + wave * size.height * 0.75f
            } else {
                4.dp.toPx() + wave * size.height * 0.18f
            }

            val x = index * spacing + spacing / 2f

            drawLine(
                color = VoyaColors.VoiceBlue.copy(
                    alpha = if (active) 0.95f else 0.45f
                ),
                start = Offset(x, centerY - height / 2f),
                end = Offset(x, centerY + height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}