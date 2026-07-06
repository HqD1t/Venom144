package com.venom.club.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.venom.club.chat.ChatScreen
import com.venom.club.data.*
import com.venom.club.data.model.*
import com.venom.club.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val dtFmt = SimpleDateFormat("dd.MM HH:mm", Locale("ru"))

@Composable
fun AdminScreen(me: UserProfile?) {
    if (me?.isAdmin != true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Доступ только для администраторов", color = BrokenGray)
        }
        return
    }
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().summerBackground()) {
        ScrollableTabRow(selectedTabIndex = tab, containerColor = VenomSurface, edgePadding = 8.dp) {
            listOf("Посты", "Брони", "Юзеры", "ПК", "Чаты").forEachIndexed { i, t ->
                Tab(tab == i, { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> AdminPosts()
            1 -> AdminBookings()
            2 -> AdminUsers()
            3 -> AdminStations()
            4 -> AdminChats(me)
        }
    }
}

/* ---------- Посты ---------- */
@Composable
private fun AdminPosts() {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }
    val posts by NewsRepo.postsFlow().collectAsState(initial = emptyList())

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Новый пост", fontWeight = FontWeight.Bold, color = VenomWhite)
                    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Текст поста...") })
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ pick.launch("image/*") }) {
                            Icon(Icons.Filled.Image, null, tint = AquaPool)
                            Text(if (image != null) " Фото ✔" else " Фото", color = AquaPool)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            enabled = text.isNotBlank() && !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    NewsRepo.createPost(text.trim(), image)
                                    text = ""; image = null; busy = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VenomGreen)
                        ) { Text("Опубликовать", color = VenomBlack) }
                    }
                }
            }
        }
        items(posts, key = { it.id }) { p ->
            Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.text.take(80), color = VenomWhite, fontSize = 13.sp)
                        Text("${p.createdAt?.toDate()?.let { dtFmt.format(it) }} · ❤ ${p.likes.size} · 💬 ${p.commentCount}",
                            fontSize = 11.sp, color = BrokenGray)
                    }
                    IconButton({ scope.launch { NewsRepo.deletePost(p.id) } }) {
                        Icon(Icons.Filled.Delete, "Удалить", tint = BusyRed)
                    }
                }
            }
        }
    }
}

/* ---------- Заявки на бронь ---------- */
@Composable
private fun AdminBookings() {
    val scope = rememberCoroutineScope()
    val pending by StationRepo.pendingBookingsFlow().collectAsState(initial = emptyList())
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (pending.isEmpty()) item { Text("Нет новых заявок 😎", color = BrokenGray) }
        items(pending, key = { it.id }) { b ->
            Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${b.stationTitle} · ${b.hours} ч", fontWeight = FontWeight.Bold, color = SummerYellow)
                    Text("${b.nickname} · ${b.phone}", color = VenomWhite, fontSize = 14.sp)
                    Text("Начало: ${b.startAt?.toDate()?.let { dtFmt.format(it) }}", color = BrokenGray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ scope.launch { StationRepo.approve(b) } },
                            colors = ButtonDefaults.buttonColors(containerColor = FreeGreen)) {
                            Text("Подтвердить", color = VenomBlack)
                        }
                        OutlinedButton({ scope.launch { StationRepo.reject(b) } }) {
                            Text("Отклонить", color = BusyRed)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- База пользователей с фильтрами ---------- */
@Composable
private fun AdminUsers() {
    val users by ProfileRepo.allUsersFlow().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var onlyFav by remember { mutableStateOf(false) }
    var onlyAdmins by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var editNote by remember { mutableStateOf<UserProfile?>(null) }

    val filtered = users.filter { u ->
        (query.isBlank() || u.nickname.contains(query, true) || u.phone.contains(query)) &&
            (!onlyFav || u.favorite) && (!onlyAdmins || u.isAdmin)
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск: ник или телефон") },
            leadingIcon = { Icon(Icons.Filled.Search, null) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(onlyFav, { onlyFav = !onlyFav }, label = { Text("⭐ Избранные") })
            FilterChip(onlyAdmins, { onlyAdmins = !onlyAdmins }, label = { Text("Админы") })
            Text("${filtered.size} чел.", color = BrokenGray, modifier = Modifier.align(Alignment.CenterVertically))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.uid }) { u ->
                Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                    shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(u.avatarUrl.ifBlank { null }, null,
                            Modifier.size(40.dp).clip(CircleShape).background(SummerGradient))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.nickname, fontWeight = FontWeight.Bold, color = VenomWhite)
                            Text(u.phone, fontSize = 12.sp, color = BrokenGray)
                            if (u.adminNote.isNotBlank())
                                Text("📝 ${u.adminNote}", fontSize = 12.sp, color = AquaPool)
                        }
                        IconButton({ scope.launch {
                            ProfileRepo.adminUpdateUser(u.uid, mapOf("favorite" to !u.favorite))
                        } }) {
                            Icon(if (u.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                "Избранное", tint = SummerYellow)
                        }
                        IconButton({ editNote = u }) { Icon(Icons.Filled.EditNote, "Заметка", tint = BrokenGray) }
                    }
                }
            }
        }
    }

    editNote?.let { u ->
        var note by remember(u.uid) { mutableStateOf(u.adminNote) }
        AlertDialog(
            onDismissRequest = { editNote = null }, containerColor = VenomSurface,
            title = { Text("Заметка: ${u.nickname}", color = VenomWhite) },
            text = { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton({
                    scope.launch { ProfileRepo.adminUpdateUser(u.uid, mapOf("adminNote" to note)) }
                    editNote = null
                }) { Text("Сохранить", color = VenomGreen) }
            },
            dismissButton = { TextButton({ editNote = null }) { Text("Отмена", color = BrokenGray) } }
        )
    }
}

/* ---------- Состояние станций ---------- */
@Composable
private fun AdminStations() {
    val stations by StationRepo.stationsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stations, key = { it.id }) { st ->
            Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st.title.ifBlank { "№${st.number}" }, fontWeight = FontWeight.Bold,
                            color = VenomWhite, modifier = Modifier.weight(1f))
                        Text(st.status, fontSize = 12.sp,
                            color = com.venom.club.booking.statusColor(st))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)) {
                        listOf(
                            "Свободен" to StationStatus.FREE,
                            "Занят" to StationStatus.BUSY,
                            "Сломан" to StationStatus.BROKEN,
                            "Тех.работы" to StationStatus.MAINTENANCE,
                        ).forEach { (label, s) ->
                            FilterChip(
                                selected = st.status == s.name,
                                onClick = { scope.launch { StationRepo.setStatus(st.id, s) } },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Чаты с пользователями + шаблоны ---------- */
@Composable
private fun AdminChats(me: UserProfile) {
    val heads by ChatRepo.chatHeadsFlow().collectAsState(initial = emptyList())
    var openChat by remember { mutableStateOf<ChatHead?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    openChat?.let { head ->
        Column {
            Row(Modifier.fillMaxWidth().background(VenomSurface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton({ openChat = null }) { Text("← Чаты", color = VenomGreen) }
                Spacer(Modifier.weight(1f))
                IconButton({ scope.launch {
                    ChatRepo.setChatMeta(head.uid, favorite = !head.favorite)
                    openChat = head.copy(favorite = !head.favorite)
                } }) {
                    Icon(if (head.favorite) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = SummerYellow)
                }
            }
            Box(Modifier.weight(1f)) { ChatScreen(me, chatUid = head.uid, asAdmin = true) }
        }
        return
    }

    Column {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Чаты", fontWeight = FontWeight.Black, color = VenomWhite, fontSize = 18.sp,
                modifier = Modifier.weight(1f))
            TextButton({ showTemplates = true }) { Text("Шаблоны", color = AquaPool) }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(heads.sortedByDescending { it.favorite }, key = { it.uid }) { h ->
                Card(colors = CardDefaults.cardColors(containerColor = VenomSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { openChat = h }) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(h.avatarUrl.ifBlank { null }, null,
                            Modifier.size(42.dp).clip(CircleShape).background(SummerGradient))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                if (h.favorite) Text("⭐ ", fontSize = 13.sp)
                                Text(h.nickname, fontWeight = FontWeight.Bold, color = VenomWhite)
                            }
                            Text(h.lastMessage.take(40), fontSize = 12.sp, color = BrokenGray)
                            if (h.note.isNotBlank()) Text("📝 ${h.note}", fontSize = 11.sp, color = AquaPool)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(h.lastAt?.toDate()?.let { dtFmt.format(it) } ?: "",
                                fontSize = 11.sp, color = BrokenGray)
                            if (h.unreadForAdmin > 0)
                                Badge(containerColor = SunsetCoral) { Text("${h.unreadForAdmin}") }
                        }
                    }
                }
            }
        }
    }

    if (showTemplates) TemplatesDialog { showTemplates = false }
}

@Composable
private fun TemplatesDialog(onClose: () -> Unit) {
    val templates by ChatRepo.templatesFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose, containerColor = VenomSurface,
        title = { Text("Шаблоны ответов", color = VenomWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                templates.forEach { (id, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text, color = VenomWhite, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton({ scope.launch { ChatRepo.deleteTemplate(id) } }) {
                            Icon(Icons.Filled.Delete, null, tint = BusyRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                OutlinedTextField(newText, { newText = it }, placeholder = { Text("Новый шаблон...") })
                TextButton(enabled = newText.isNotBlank(), onClick = {
                    scope.launch { ChatRepo.saveTemplate(null, newText.trim()); newText = "" }
                }) { Text("+ Добавить", color = VenomGreen) }
            }
        },
        confirmButton = { TextButton(onClose) { Text("Готово", color = VenomGreen) } }
    )
}
