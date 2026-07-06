package com.venom.club.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Летний вайб: закатные тёплые акценты на тёмной "веномовской" базе
val VenomBlack = Color(0xFF0E0F13)
val VenomSurface = Color(0xFF1A1C22)
val SunsetOrange = Color(0xFFFF7A45)
val SunsetCoral = Color(0xFFFF4D6D)
val SummerYellow = Color(0xFFFFC94D)
val AquaPool = Color(0xFF35D0C5)
val VenomWhite = Color(0xFFF4F2EE)
val FreeGreen = Color(0xFF4CD964)
val BusyRed = Color(0xFFFF3B30)
val BookedBlue = Color(0xFF4DA3FF)
val BrokenGray = Color(0xFF6B6F7B)

val SummerGradient = Brush.linearGradient(
    listOf(SunsetOrange, SunsetCoral, SummerYellow)
)

private val DarkScheme = darkColorScheme(
    primary = SunsetOrange,
    secondary = AquaPool,
    tertiary = SummerYellow,
    background = VenomBlack,
    surface = VenomSurface,
    onPrimary = VenomBlack,
    onBackground = VenomWhite,
    onSurface = VenomWhite,
    error = BusyRed,
)

@Composable
fun VenomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

/** Анимированный "летний" фон-градиент для экранов. */
@Composable
fun Modifier.summerBackground(): Modifier {
    val t by rememberInfiniteTransition(label = "bg").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bgShift"
    )
    return background(
        Brush.linearGradient(
            colors = listOf(VenomBlack, Color(0xFF2A1A22), Color(0xFF33202A), VenomBlack),
            start = Offset(0f, 1400f * t),
            end = Offset(1000f, 2400f * (1f - t))
        )
    )
}
