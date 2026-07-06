package com.venom.club.booking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.venom.club.data.StationRepo
import com.venom.club.data.model.*
import com.venom.club.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val timeFmt = SimpleDateFormat("HH:mm", Locale("ru"))

fun statusColor(s: Station): Color = when (StationStatus.valueOf(s.status)) {
    StationStatus.FREE -> FreeGreen
    StationStatus.BUSY -> BusyRed
    StationStatus.BOOKED -> BookedBlue
    StationStatus.BROKEN, StationStatus.MAINTENANCE -> BrokenGray
}

/** Позиция на карте клуба: в процентах ширины (x, w) и в тех же единицах по вертикали (y, h). */
private data class MapPos(val x: Float, val y: Float, val w: Float = 10f, val h: Float = 10f)

/** Раскладка по схеме клуба VENOM (числа = номер станции). */
private val stationPos = mapOf(
    // ПК 1-5 — верхний ряд
    1 to MapPos(34f, 4f), 2 to MapPos(45f, 4f), 3 to MapPos(56f, 4f), 4 to MapPos(67f, 4f), 5 to MapPos(78f, 4f),
    // ПК 6-10 — правая колонка (верх)
    6 to MapPos(86f, 14f), 7 to MapPos(86f, 25f), 8 to MapPos(86f, 36f), 9 to MapPos(86f, 47f), 10 to MapPos(86f, 58f),
    // ПК 11-14 — правая колонка (середина)
    11 to MapPos(86f, 73f), 12 to MapPos(86f, 84f), 13 to MapPos(86f, 95f), 14 to MapPos(86f, 106f),
    // ПК 15-17 — правая колонка (низ)
    15 to MapPos(86f, 121f), 16 to MapPos(86f, 132f), 17 to MapPos(86f, 143f),
    // Консоли 18-21 — средняя колонка (снизу вверх)
    21 to MapPos(44f, 73f), 20 to MapPos(44f, 84f), 19 to MapPos(44f, 95f), 18 to MapPos(44f, 106f),
    // VIP 22 — возле тех.помещения
    22 to MapPos(31f, 60f),
    // PS5 комнаты (большие)
    26 to MapPos(12f, 2f, 18f, 14f),    // 4 комната — верхний левый угол
    25 to MapPos(22f, 118f, 18f, 14f),  // 3 комната
    24 to MapPos(22f, 134f, 18f, 14f),  // 2 комната
    23 to MapPos(22f, 150f, 18f, 14f),  // 1 комната
)

private const val MAP_HEIGHT_UNITS = 182f

@Composable
fun BookingScreen(me: UserProfile?) {
    val stations by StationRepo.stationsFlow().collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Station?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize().summerBackground()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("🎮 Выберите компьютер или PS5",
                fontSize = 22.sp, fontWeight = FontWeight.Black, color = VenomWhite)
            Spacer(Modifier.height(8.dp))
            Legend()
            Spacer(Modifier.height(12.dp))
            ClubMap(stations) { selected = it }
            Spacer(Modifier.height(24.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    selected?.let { st ->
        BookingDialog(st, onDismiss = { selected = null }, onConfirm = { start, hours ->
            scope.launch {
                if (me != null) {
                    StationRepo.requestBooking(st, me, start, hours)
                    snackbar.showSnackbar("Заявка отправлена! Ждём подтверждения админа 🤙")
                }
                selected = null
            }
        })
    }
}

/** Карта клуба: станции на своих местах + статичные объекты (WC, тех.помещение, вход, админ). */
@Composable
private fun ClubMap(stations: List<Station>, onStationClick: (Station) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val u = maxWidth / 100f
        Box(Modifier.fillMaxWidth().height(u * MAP_HEIGHT_UNITS)) {

            // Статичные объекты
            StaticRoom("🚻\nWC", MapPos(4f, 40f, 14f, 12f), u, BrokenGray)
            StaticRoom("⛔\nТех.\nпомещение", MapPos(16f, 56f, 14f, 14f), u, Color(0xFF7A2B2B))
            StaticRoom("👤\nАДМИН", MapPos(38f, 166f, 18f, 12f), u, BrokenGray)
            StaticRoom("🚪\nВход", MapPos(80f, 166f, 14f, 12f), u, Color(0xFF8A6B4A))

            stations.forEach { st ->
                val pos = stationPos[st.number] ?: return@forEach
                StationCell(st, pos, u, onClick = { onStationClick(st) })
            }
        }
    }
}

@Composable
private fun StaticRoom(label: String, pos: MapPos, u: androidx.compose.ui.unit.Dp, tint: Color) {
    Box(
        Modifier
            .offset(x = u * pos.x, y = u * pos.y)
            .size(u * pos.w, u * pos.h)
            .clip(RoundedCornerShape(10.dp))
            .background(VenomSurface)
            .border(1.dp, tint.copy(alpha = .6f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 9.sp, color = VenomWhite.copy(alpha = .8f),
            textAlign = TextAlign.Center, lineHeight = 11.sp)
    }
}

@Composable
private fun StationCell(st: Station, pos: MapPos, u: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val color by animateColorAsState(statusColor(st), tween(400), label = "stColor")
    val big = pos.w > 12f
    Column(
        Modifier
            .offset(x = u * pos.x, y = u * pos.y)
            .size(u * pos.w, u * pos.h)
            .bouncyClickable(enabled = st.status == StationStatus.FREE.name, onClick = onClick)
            .clip(RoundedCornerShape(if (big) 12.dp else 10.dp))
            .background(VenomSurface)
            .border(2.dp, color, RoundedCornerShape(if (big) 12.dp else 10.dp))
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (big) {
            Text("PS 5", color = VenomWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(st.title, color = VenomWhite.copy(alpha = .7f), fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("${st.number}", color = color.copy(alpha = .7f), fontSize = 9.sp)
        } else {
            Text("${st.number}", color = VenomWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        when {
            st.status == StationStatus.BOOKED.name && st.bookedUntil != null ->
                Text("до ${timeFmt.format(st.bookedUntil.toDate())}", fontSize = 7.sp,
                    color = BookedBlue, lineHeight = 8.sp, textAlign = TextAlign.Center)
            st.status == StationStatus.BROKEN.name ->
                Text("🔧", fontSize = 8.sp, lineHeight = 9.sp)
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Свободен" to FreeGreen, "Занят" to BusyRed, "Бронь" to BookedBlue, "Не работает" to BrokenGray)
            .forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Spacer(Modifier.width(4.dp))
                    Text(label, fontSize = 11.sp, color = VenomWhite.copy(alpha = .7f))
                }
            }
    }
}

@Composable
fun BookingDialog(st: Station, onDismiss: () -> Unit, onConfirm: (Timestamp, Int) -> Unit) {
    var hours by remember { mutableStateOf(1) }
    var delayMin by remember { mutableStateOf(0) } // через сколько минут прийти
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VenomSurface,
        title = { Text("Бронь: ${st.title.ifBlank { "№${st.number}" }}", color = VenomWhite) },
        text = {
            Column {
                Text("Когда придёшь?", color = VenomWhite.copy(alpha = .8f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Сейчас", 30 to "+30 мин", 60 to "+1 час").forEach { (m, label) ->
                        FilterChip(selected = delayMin == m, onClick = { delayMin = m }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("На сколько часов?", color = VenomWhite.copy(alpha = .8f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { h ->
                        FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text("$h ч") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Бронь встанет после подтверждения администратором.",
                    fontSize = 12.sp, color = BrokenGray)
            }
        },
        confirmButton = {
            TextButton({ onConfirm(Timestamp(Date(System.currentTimeMillis() + delayMin * 60_000L)), hours) }) {
                Text("Забронировать", color = VenomGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена", color = BrokenGray) } }
    )
}
