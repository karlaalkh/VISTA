package com.voya.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voya.ui.theme.VoyaColors
import com.voya.ui.theme.VoyaTheme

@Composable
fun EmpathyLens(
    modifier: Modifier = Modifier,
    eyeColor: Color = VoyaColors.Gold,
    lensColor: Color = VoyaColors.VoiceBlue
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val eyeWidth = w * 0.88f
        val eyeHeight = h * 0.43f
        val eyeStrokeWidth = w * 0.018f
        val irisStrokeWidth = w * 0.025f

        val left = center.x - eyeWidth / 2f
        val right = center.x + eyeWidth / 2f
        val top = center.y - eyeHeight / 2f
        val bottom = center.y + eyeHeight / 2f

        // Outer eye shape
        val eyePath = Path().apply {
            moveTo(left, center.y)

            cubicTo(
                center.x - eyeWidth * 0.25f, top,
                center.x + eyeWidth * 0.25f, top,
                right, center.y
            )

            cubicTo(
                center.x + eyeWidth * 0.25f, bottom,
                center.x - eyeWidth * 0.25f, bottom,
                left, center.y
            )

            close()
        }

        drawPath(
            path = eyePath,
            color = eyeColor,
            style = Stroke(
                width = eyeStrokeWidth,
                cap = StrokeCap.Round
            )
        )

        // Soft halo behind the lens
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lensColor.copy(alpha = 0.24f),
                    lensColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = center,
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = center
        )

        // Outer iris ring
        // The cubic curves reach 75% of the distance to their control points.
        // Leave room for both strokes and a small gap inside the eye opening.
        val eyeOpeningHalfHeight = eyeHeight * 0.375f
        val irisRadius = (
            minOf(eyeWidth / 2f, eyeOpeningHalfHeight) -
                (eyeStrokeWidth + irisStrokeWidth) / 2f
            ).coerceAtLeast(0f) * 0.9f

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    eyeColor,
                    lensColor,
                    lensColor.copy(alpha = 0.6f),
                    eyeColor
                ),
                center = center
            ),
            radius = irisRadius,
            center = center,
            style = Stroke(width = irisStrokeWidth)
        )

        // Inner lens
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF244968),
                    VoyaColors.Background
                ),
                center = center,
                radius = irisRadius
            ),
            radius = irisRadius * 0.84f,
            center = center
        )

        // Pupil
        drawCircle(
            color = VoyaColors.Background,
            radius = irisRadius * 0.40f,
            center = center
        )

        // Warm central light
        val lightCenter = Offset(
            center.x + irisRadius * 0.18f,
            center.y - irisRadius * 0.20f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    eyeColor.copy(alpha = 0.9f),
                    eyeColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = lightCenter,
                radius = irisRadius * 0.55f
            ),
            radius = irisRadius * 0.55f,
            center = lightCenter
        )

        // Small reflective highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = irisRadius * 0.075f,
            center = Offset(
                center.x - irisRadius * 0.27f,
                center.y - irisRadius * 0.28f
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07111C)
@Composable
private fun EmpathyLensPreview() {
    VoyaTheme {
        EmpathyLens(
            modifier = Modifier.size(
                width = 220.dp,
                height = 140.dp
            )
        )
    }
}


@Composable
fun AnimatedEmpathyLens(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "LensBreathing"
    )

    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LensScale"
    )

    EmpathyLens(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}
