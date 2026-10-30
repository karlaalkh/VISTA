package com.vista.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vista.ui.theme.VistaColors
import com.vista.ui.theme.VistaTheme

@Composable
fun EmpathyLens(
    modifier: Modifier = Modifier,
    eyeColor: Color = VistaColors.Gold,
    lensColor: Color = VistaColors.VoiceBlue,
    openness: Float = 1f,
    gaze: Float = 0f
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
        val top = center.y - eyeHeight / 2f * openness.coerceIn(0f, 1f)
        val bottom = center.y + eyeHeight / 2f * openness.coerceIn(0f, 1f)

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

        // The eyelids hide the iris during a blink while it keeps its round shape.
        clipPath(eyePath) {
            translate(left = w * 0.025f * gaze.coerceIn(-1f, 1f)) {
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
                            VistaColors.Background
                        ),
                        center = center,
                        radius = irisRadius
                    ),
                    radius = irisRadius * 0.84f,
                    center = center
                )

                // Pupil
                drawCircle(
                    color = VistaColors.Background,
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
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07111C)
@Composable
private fun EmpathyLensPreview() {
    VistaTheme {
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

    val openness by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5200
                1f at 0
                1f at 4400 using EaseInOutSine
                0.02f at 4520 using EaseInOutSine
                1f at 4700
                1f at 5200
            }
        ),
        label = "LensBlink"
    )

    val gaze by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LensGaze"
    )

    EmpathyLens(
        openness = openness,
        gaze = gaze,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}
