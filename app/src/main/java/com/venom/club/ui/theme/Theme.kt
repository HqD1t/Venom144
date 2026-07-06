package com.venom.club.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Летний вайб VENOM: ядовитая зелень + лаймовые акценты на тёмной базе (как схема клуба)
val VenomBlack = Color(0xFF0A120D)     // чёрный с зелёным подтоном
val VenomSurface = Color(0xFF1D2E23)   // поверхность карточек — светлее фона, не сливается
val VenomGreen = Color(0xFF35D96B)     // главный ядовито-зелёный
val LimeZest = Color(0xFFA8E063)       // сочный лайм
val SummerYellow = Color(0xFFF6E27A)   // солнечный жёлтый
val AquaPool = Color(0xFF35D0C5)       // вода в бассейне
val SunsetCoral = Color(0xFFFF4D6D)    // коралл — для лайков/ошибок
val VenomWhite = Color(0xFFF0F5F0)
val FreeGreen = Color(0xFF4CD964)
val BusyRed = Color(0xFFFF3B30)
val BookedBlue = Color(0xFF4DA3FF)
val BrokenGray = Color(0xFF6E7B72)

val SummerGradient = Brush.linearGradient(
    listOf(VenomGreen, LimeZest, SummerYellow)
)

private val DarkScheme = darkColorScheme(
    primary = VenomGreen,
    secondary = AquaPool,
    tertiary = LimeZest,
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

/** "Летний" фон: статичный тёмно-зелёный градиент (без анимации — бережём FPS). */
private val bgBrush = Brush.verticalGradient(
    listOf(VenomBlack, Color(0xFF0E2415), Color(0xFF123420), VenomBlack)
)

fun Modifier.summerBackground(): Modifier = background(bgBrush)

/** Кликабельность с пружинным сжатием — вместо обычного clickable. */
fun Modifier.bouncyClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressBounce"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Бегущий блик по градиенту — для кнопок. */
@Composable
fun shimmerGradient(): Brush {
    val shift by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerShift"
    )
    return Brush.linearGradient(
        colors = listOf(VenomGreen, LimeZest, SummerYellow, LimeZest, VenomGreen),
        start = Offset(shift - 450f, 0f),
        end = Offset(shift + 450f, 120f)
    )
}
