package com.voya.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VoyaTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 7.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
)
