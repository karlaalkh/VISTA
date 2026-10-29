package com.voya.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable

@Composable
fun CharacterSceneImage(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF07111C)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}