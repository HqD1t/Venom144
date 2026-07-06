package com.venom.club.booking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun BookingScreen(me: UserProfile?) {
    val stations by StationRepo.stationsFlow().collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Station?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize().summerBackground()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Бронирование", fontSize = 24.sp, fontWeight = FontWeight.Black, color = VenomWhite)
            Spacer(Modifier.height(8.dp))
            Legend()
            Spacer(Modifier.height(12.dp))

            val zones = stations.groupBy { it.zone }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                zones.forEach { (zone, list) ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                        Text(zone, color = SummerYellow, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(list, key = { it.id }) { st ->
                        StationCell(st) { selected = st }
                    }
                }
            }
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
private fun StationCell(st: Station, onClick: () -> Unit) {
    val color by animateColorAsState(statusColor(st), tween(500), label = "stColor")
    // Пульсация для свободных — приглашает нажать
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, if (st.status == StationStatus.FREE.name) 1.05f else 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulseV"
    )
    Column(
        Modifier
            .scale(pulse)
            .clip(RoundedCornerShape(14.dp))
            .background(VenomSurface)
            .border(2.dp, color, RoundedCornerShape(14.dp))
            .clickable(enabled = st.status == StationStatus.FREE.name, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (st.type == StationType.PS5.name) Icons.Filled.SportsEsports else Icons.Filled.Computer,
            null, tint = color, modifier = Modifier.size(26.dp)
        )
        Text(st.title.ifBlank { "#${st.number}" }, color = VenomWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        when {
            st.status == StationStatus.BOOKED.name && st.bookedUntil != null ->
                Text("до ${timeFmt.format(st.bookedUntil.toDate())}\n${st.bookedBy}",
                    fontSize = 9.sp, color = BookedBlue, lineHeight = 10.sp)
            st.status == StationStatus.BROKEN.name -> Text("сломан", fontSize = 9.sp, color = BrokenGray)
            st.status == StationStatus.MAINTENANCE.name -> Text("тех.работы", fontSize = 9.sp, color = BrokenGray)
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
                Text("Забронировать", color = SunsetOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена", color = BrokenGray) } }
    )
}
