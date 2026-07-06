package com.venom.club.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.venom.club.R
import com.venom.club.ui.theme.*

@Composable
fun AuthFlow(activity: Activity, onDone: () -> Unit, vm: AuthViewModel = viewModel()) {
    val step by vm.step.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(step) { if (step == AuthStep.DONE) onDone() }

    Box(Modifier.fillMaxSize().summerBackground()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Image(
                painterResource(R.drawable.venom_logo), null,
                Modifier.size(140.dp).clip(CircleShape)
            )
            Text("VENOM CLUB", fontSize = 30.sp, fontWeight = FontWeight.Black, color = VenomWhite)
            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(tween(400)) { it } + fadeIn()) togetherWith
                        (slideOutHorizontally(tween(400)) { -it } + fadeOut())
                }, label = "authStep"
            ) { s ->
                when (s) {
                    AuthStep.PHONE -> PhoneStep(loading) { vm.sendCode(it, activity) }
                    AuthStep.CODE -> CodeStep(loading) { vm.confirmCode(it) }
                    AuthStep.TERMS -> TermsStep { vm.acceptTerms() }
                    AuthStep.PIN_CREATE -> PinStep("Придумай пин-код") { vm.createPin(it) }
                    AuthStep.PIN_ENTER -> PinStep("Введи пин-код") { vm.checkPin(it) }
                    AuthStep.DONE -> Box {}
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = BusyRed, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PhoneStep(loading: Boolean, onSend: (String) -> Unit) {
    var phone by remember { mutableStateOf("8") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Вход по номеру телефона", color = VenomWhite, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("Тот же номер, что и в клубе (через 8)", color = BrokenGray, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone, onValueChange = { if (it.length <= 11) phone = it.filter { c -> c.isDigit() } },
            placeholder = { Text("89000000000") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        GradientButton("Получить код", loading) { onSend(phone) }
    }
}

@Composable
private fun CodeStep(loading: Boolean, onConfirm: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Код из SMS", color = VenomWhite, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code, onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
            placeholder = { Text("••••••") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        GradientButton("Подтвердить", loading, enabled = code.length == 6) { onConfirm(code) }
    }
}

@Composable
private fun TermsStep(onAccept: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Column {
        Text("Пользовательское соглашение и оферта", color = VenomWhite,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = VenomSurface,
            modifier = Modifier.height(280.dp)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(TERMS_TEXT, color = VenomWhite.copy(alpha = .8f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(agreed, { agreed = it })
            Text("Принимаю условия соглашения и оферты", color = VenomWhite, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        GradientButton("Продолжить", enabled = agreed) { onAccept() }
    }
}

/** Пин-клавиатура с точками */
@Composable
fun PinStep(title: String, onComplete: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    LaunchedEffect(pin) { if (pin.length == 4) { onComplete(pin); pin = "" } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = VenomWhite, fontSize = 18.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                val filled = i < pin.length
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (filled) SunsetOrange else VenomSurface)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Surface(
                        shape = CircleShape,
                        color = if (key.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else VenomSurface,
                        onClick = {
                            when {
                                key == "⌫" -> pin = pin.dropLast(1)
                                key.isNotEmpty() && pin.length < 4 -> pin += key
                            }
                        },
                        enabled = key.isNotEmpty(),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(key, color = VenomWhite, fontSize = 22.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun GradientButton(text: String, loading: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(SummerGradient, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = VenomBlack)
            else Text(text, color = VenomBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private const val TERMS_TEXT = """ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ И ПУБЛИЧНАЯ ОФЕРТА

1. Общие положения. Настоящее соглашение регулирует использование мобильного приложения компьютерного клуба VENOM (далее — Клуб) и является публичной офертой.

2. Регистрация. Регистрация производится по номеру мобильного телефона, совпадающему с номером в учётной системе Клуба. Пользователь подтверждает достоверность указанных данных.

3. Бронирование. Заявка на бронь игрового места считается принятой только после подтверждения администратором. Клуб вправе отменить бронь при опоздании более чем на 15 минут.

4. Персональные данные. Пользователь даёт согласие на обработку персональных данных (номер телефона, никнейм, статистика игровых сессий) в целях работы приложения.

5. Оплата услуг. Услуги оплачиваются в Клубе согласно действующему прайс-листу. Приложение отображает статистику расходов справочно.

6. Ответственность. Клуб не несёт ответственности за перерывы в работе приложения, вызванные техническими причинами.

7. Заключительные положения. Клуб вправе изменять условия соглашения, уведомляя пользователей через приложение. Продолжение использования приложения означает согласие с изменениями.

Актуальная редакция и реквизиты — на ресепшн Клуба."""
