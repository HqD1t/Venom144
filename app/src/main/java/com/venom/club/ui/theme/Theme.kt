package com.venom.club.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.club.R
import androidx.compose.foundation.layout.size as sizeMod

/*
 * Дизайн-система VENOM.
 * База — тёмный графит (нейтральный, без зелёного подтона),
 * зелёный — ТОЛЬКО акцент: главные кнопки, активные элементы, статус «свободен».
 */
val VenomBlack = Color(0xFF0C0E11)      // фон
val VenomSurface = Color(0xFF161A20)    // карточки
val SurfaceHigh = Color(0xFF1E242C)     // приподнятые элементы: поля ввода, диалоги
val VenomGreen = Color(0xFF3DDC68)      // единственный акцент
val VenomGreenDim = Color(0xFF1FA34D)   // тёмная пара акцента (градиент кнопок)
val VenomWhite = Color(0xFFF2F4F6)      // основной текст
val SoftWhite = Color(0xFFC9D1D9)       // заголовки секций, второй план
val TextMuted = Color(0xFF8B949E)       // подписи, метаданные
val SunsetCoral = Color(0xFFFF5470)     // лайки, ошибки
val FreeGreen = VenomGreen              // статус: свободен
val BusyRed = Color(0xFFFF453A)         // статус: занят
val BookedBlue = Color(0xFF4DA3FF)      // статус: бронь
val BrokenGray = Color(0xFF636E7B)      // статус: не работает
val AquaPool = TextMuted                // легаси-алиас
val SummerYellow = SoftWhite            // легаси-алиас
val LimeZest = VenomGreenDim            // легаси-алиас

val SummerGradient = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(VenomGreen, VenomGreenDim)
)

// ---------- Шрифт: Rubik (variable, кириллица) ----------
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun rubik(weight: FontWeight) = Font(
    R.font.rubik, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Rubik = FontFamily(
    rubik(FontWeight.Normal),
    rubik(FontWeight.Medium),
    rubik(FontWeight.SemiBold),
    rubik(FontWeight.Bold),
    rubik(FontWeight.Black),
)

private val base = Typography()
private val VenomTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Rubik),
    displayMedium = base.displayMedium.copy(fontFamily = Rubik),
    displaySmall = base.displaySmall.copy(fontFamily = Rubik),
    headlineLarge = base.headlineLarge.copy(fontFamily = Rubik),
    headlineMedium = base.headlineMedium.copy(fontFamily = Rubik),
    headlineSmall = base.headlineSmall.copy(fontFamily = Rubik),
    titleLarge = base.titleLarge.copy(fontFamily = Rubik),
    titleMedium = base.titleMedium.copy(fontFamily = Rubik),
    titleSmall = base.titleSmall.copy(fontFamily = Rubik),
    bodyLarge = base.bodyLarge.copy(fontFamily = Rubik),
    bodyMedium = base.bodyMedium.copy(fontFamily = Rubik),
    bodySmall = base.bodySmall.copy(fontFamily = Rubik),
    labelLarge = base.labelLarge.copy(fontFamily = Rubik),
    labelMedium = base.labelMedium.copy(fontFamily = Rubik),
    labelSmall = base.labelSmall.copy(fontFamily = Rubik),
)

private val DarkScheme = darkColorScheme(
    primary = VenomGreen,
    secondary = SoftWhite,
    tertiary = VenomGreenDim,
    background = VenomBlack,
    surface = VenomSurface,
    surfaceVariant = SurfaceHigh,
    onPrimary = VenomBlack,
    onBackground = VenomWhite,
    onSurface = VenomWhite,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF2A313A),
    error = BusyRed,
)

@Composable
fun VenomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = VenomTypography, content = content)
}

/** Фон приложения — чистый графит, без градиентного шума. */
fun Modifier.summerBackground(): Modifier = background(VenomBlack)

/** Кликабельность с пружинным сжатием — вместо обычного clickable. */
fun Modifier.bouncyClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressBounce"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Главная кнопка: акцентная, с анимацией нажатия. Единый вид по всему приложению. */
@Composable
fun VenomButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btnScale"
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VenomGreen,
            contentColor = VenomBlack,
            disabledContainerColor = SurfaceHigh,
            disabledContentColor = TextMuted,
        ),
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier)
            .height(50.dp)
            .scale(scale)
    ) {
        if (loading) CircularProgressIndicator(Modifier.sizeMod(22.dp), color = VenomBlack, strokeWidth = 2.5.dp)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
