package com.venom.club.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.venom.club.data.ChatRepo
import com.venom.club.data.model.ChatMessage
import com.venom.club.data.model.UserProfile
import com.venom.club.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm", Locale("ru"))

/** Чат пользователя с администрацией. Для админа переиспользуется через chatUid. */
@Composable
fun ChatScreen(me: UserProfile?, chatUid: String? = null, asAdmin: Boolean = false) {
    val uid = chatUid ?: me?.uid ?: return
    val messages by ChatRepo.messagesFlow(uid).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    var actionMsg by remember { mutableStateOf<ChatMessage?>(null) }   // меню по долгому нажатию
    var editMsg by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        pendingImage = it
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize().summerBackground()) {
        Surface(color = VenomSurface) {
            Text(
                if (asAdmin) "Чат с игроком" else "Чат с администрацией VENOM",
                Modifier.fillMaxWidth().padding(16.dp),
                color = VenomWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp
            )
        }
        LazyColumn(Modifier.weight(1f), state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { m ->
                val mine = if (asAdmin) m.fromAdmin else !m.fromAdmin
                MessageBubble(m, mine = mine, onLongPress = { if (mine) actionMsg = m })
            }
        }
        pendingImage?.let {
            Row(Modifier.background(VenomSurface).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(it, null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Фото прикреплено", color = AquaPool, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton({ pendingImage = null }) { Text("✕", color = BrokenGray) }
            }
        }
        Row(Modifier.fillMaxWidth().background(VenomSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton({ pickImage.launch("image/*") }) {
                Icon(Icons.Filled.Image, "Фото", tint = AquaPool)
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Сообщение...") },
                shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if ((text.isBlank() && pendingImage == null) || me == null) return@IconButton
                val t = text.trim(); val img = pendingImage
                text = ""; pendingImage = null
                scope.launch { ChatRepo.send(uid, me, t, img, asAdmin) }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = VenomGreen) }
        }
    }

    // Меню действий над своим сообщением
    actionMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { actionMsg = null },
            containerColor = VenomSurface,
            title = { Text("Сообщение", color = VenomWhite) },
            text = { Text(m.text.take(80), color = BrokenGray) },
            confirmButton = {
                Row {
                    if (m.text.isNotBlank()) TextButton({ editMsg = m; actionMsg = null }) {
                        Text("Изменить", color = VenomGreen)
                    }
                    TextButton({
                        scope.launch { ChatRepo.deleteMessage(uid, m.id) }
                        actionMsg = null
                    }) { Text("Удалить", color = BusyRed) }
                }
            },
            dismissButton = { TextButton({ actionMsg = null }) { Text("Отмена", color = BrokenGray) } }
        )
    }

    // Редактирование сообщения
    editMsg?.let { m ->
        var newText by remember(m.id) { mutableStateOf(m.text) }
        AlertDialog(
            onDismissRequest = { editMsg = null },
            containerColor = VenomSurface,
            title = { Text("Изменить сообщение", color = VenomWhite) },
            text = { OutlinedTextField(newText, { newText = it }, Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = newText.isNotBlank(), onClick = {
                    scope.launch { ChatRepo.editMessage(uid, m.id, newText.trim()) }
                    editMsg = null
                }) { Text("Сохранить", color = VenomGreen) }
            },
            dismissButton = { TextButton({ editMsg = null }) { Text("Отмена", color = BrokenGray) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(m: ChatMessage, mine: Boolean, onLongPress: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (mine) 16.dp else 4.dp,
                    bottomEnd = if (mine) 4.dp else 16.dp))
                .background(if (mine) VenomGreen.copy(alpha = .9f) else VenomSurface)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(10.dp)
        ) {
            if (m.imageUrl.isNotBlank()) {
                AsyncImage(m.imageUrl, null,
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(10.dp)))
                if (m.text.isNotBlank()) Spacer(Modifier.height(6.dp))
            }
            if (m.text.isNotBlank())
                Text(m.text, color = if (mine) VenomBlack else VenomWhite, fontSize = 15.sp)
            Text(m.createdAt?.toDate()?.let { timeFmt.format(it) } ?: "",
                fontSize = 10.sp,
                color = (if (mine) VenomBlack else VenomWhite).copy(alpha = .5f),
                modifier = Modifier.align(Alignment.End))
        }
    }
}
