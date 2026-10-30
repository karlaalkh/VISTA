package com.voya.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voya.ui.components.AnimatedEmpathyLens
import com.voya.ui.theme.VoyaColors

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "See through their eyes.",
        subtitle = "Every story is a new perspective.",
        description = "Step into someone else's world. " +
                "Experience their choices, emotions, and consequences."
    ),
    OnboardingPage(
        title = "Your voice matters.",
        subtitle = "Don't just listen. Speak.",
        description = "When a character asks you a question, " +
                "answer naturally. Your words shape what happens next."
    ),
    OnboardingPage(
        title = "Ready to step inside?",
        subtitle = "Put on your headphones.",
        description = "Find a quiet place, listen closely, " +
                "and let the story begin."
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentPage by rememberSaveable {
        mutableIntStateOf(0)
    }

    val page = onboardingPages[currentPage]

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
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOYA",
                    color = VoyaColors.Gold,
                    fontSize = 18.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${currentPage + 1} / 3",
                    color = VoyaColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // Illustration changes with the page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> AnimatedEmpathyLens(
                        modifier = Modifier.size(
                            width = 240.dp,
                            height = 150.dp
                        )
                    )

                    1 -> OnboardingSymbol(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = VoyaColors.Gold,
                                modifier = Modifier.size(74.dp)
                            )
                        }
                    )

                    else -> OnboardingSymbol(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = VoyaColors.Gold,
                                modifier = Modifier.size(90.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = page.title,
                color = VoyaColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = page.subtitle,
                color = VoyaColors.Gold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = page.description,
                color = VoyaColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .width(
                                if (index == currentPage) 26.dp
                                else 8.dp
                            )
                            .height(8.dp)
                            .background(
                                color = if (index == currentPage) {
                                    VoyaColors.Gold
                                } else {
                                    VoyaColors.TextSecondary.copy(
                                        alpha = 0.35f
                                    )
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (currentPage < onboardingPages.lastIndex) {
                        currentPage++
                    } else {
                        onComplete()
                    }
                },
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
                    text = if (currentPage == 2) {
                        "Start Playing"
                    } else {
                        "Continue"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (currentPage < onboardingPages.lastIndex) {
                TextButton(
                    onClick = onComplete
                ) {
                    Text(
                        text = "Skip",
                        color = VoyaColors.TextSecondary
                    )
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun OnboardingSymbol(
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        VoyaColors.VoiceBlue.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )

            drawCircle(
                color = VoyaColors.VoiceBlue.copy(alpha = 0.35f),
                radius = size.minDimension * 0.42f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        icon()
    }
}
