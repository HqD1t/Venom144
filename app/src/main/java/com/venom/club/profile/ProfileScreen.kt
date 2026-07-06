package com.venom.club.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.venom.club.data.GizmoClient
import com.venom.club.data.ProfileRepo
import com.venom.club.data.model.GizmoStats
import com.venom.club.data.model.UserProfile
import com.venom.club.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(me: UserProfile?) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<GizmoStats?>(null) }
    var statsError by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    // Статистика из Gizmo через мини-сервер клуба
    LaunchedEffect(me?.phone) {
        if (com.venom.club.data.Demo.enabled) { stats = com.venom.club.data.Demo.stats; return@LaunchedEffect }
        val phone = me?.phone ?: return@LaunchedEffect
        try {
            val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: return@LaunchedEffect
            stats = GizmoClient.api.statsByPhone(phone, "Bearer $token")
        } catch (_: Exception) { statsError = true }
    }

    Column(Modifier.fillMaxSize().summerBackground().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        // Аватар — клик меняет фото; перед загрузкой кропаем в квадрат и сжимаем
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    val prepared = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.venom.club.util.ImageUtils.prepareAvatar(ctx, it)
                    }
                    ProfileRepo.uploadAvatar(prepared)
                }
            }
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                me?.avatarUrl?.ifBlank { null }, null,
                Modifier.size(110.dp).clip(CircleShape).background(SummerGradient)
                    .clickable { pickAvatar.launch("image/*") }
            )
            Icon(Icons.Filled.PhotoCamera, "Сменить фото", tint = VenomWhite,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(VenomSurface).padding(5.dp))
        }
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(me?.nickname ?: "...", fontSize = 22.sp, fontWeight = FontWeight.Black, color = VenomWhite)
            IconButton({ editingName = true }) {
                Icon(Icons.Filled.Edit, "Изменить имя", tint = BrokenGray, modifier = Modifier.size(18.dp))
            }
        }
        Text(me?.phone ?: "", color = BrokenGray, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        // Статистика Gizmo
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Потрачено", stats?.let { "%.0f ₽".format(it.totalSpent) } ?: "—", Icons.Filled.Payments)
            StatCard("Часов", stats?.let { "%.0f".format(it.totalHours) } ?: "—", Icons.Filled.Timer)
            StatCard("Сессий", stats?.sessionsCount?.toString() ?: "—", Icons.Filled.SportsEsports)
        }
        if (statsError) Text("Статистика клуба недоступна (нет связи с сервером)",
            fontSize = 12.sp, color = BrokenGray, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))

        TabRow(selectedTabIndex = tab, containerColor = VenomSurface,
            modifier = Modifier.clip(RoundedCornerShape(14.dp))) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Промокоды") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Настройки") })
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> PromoTab(me)
            1 -> SettingsTab()
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(me?.nickname ?: "") }
        AlertDialog(
            onDismissRequest = { editingName = false },
            containerColor = VenomSurface,
            title = { Text("Новый ник", color = VenomWhite) },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = {
                TextButton({
                    scope.launch { ProfileRepo.updateNickname(name.trim()) }
                    editingName = false
                }) { Text("Сохранить", color = VenomGreen) }
            },
            dismissButton = { TextButton({ editingName = false }) { Text("Отмена", color = BrokenGray) } }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = VenomSurface)) {
        Column(Modifier.padding(14.dp).width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = VenomGreen)
            Text(value, fontWeight = FontWeight.Black, color = VenomWhite, fontSize = 17.sp)
            Text(label, fontSize = 11.sp, color = BrokenGray)
        }
    }
}

@Composable
private fun PromoTab(me: UserProfile?) {
    val promos by ProfileRepo.promoCodesFlow().collectAsState(initial = emptyList())
    val fmt = SimpleDateFormat("d MMMM", Locale("ru"))
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        // Ввод промокода вручную
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.uppercase(); result = null },
                placeholder = { Text("Ввести промокод") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = input.isNotBlank(),
                onClick = { scope.launch { result = ProfileRepo.applyPromo(input) } },
                colors = ButtonDefaults.buttonColors(containerColor = VenomGreen)
            ) { Text("ОК", color = VenomBlack, fontWeight = FontWeight.Bold) }
        }
        result?.let {
            Text(it, fontSize = 13.sp,
                color = if (it.startsWith("Промокод активирован")) FreeGreen else SunsetCoral)
        }

        if (promos.isEmpty()) Text("Активных промокодов нет", color = BrokenGray)
        promos.forEach { p ->
            val used = p.usedBy.contains(me?.uid)
            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VenomSurface)) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.code, fontWeight = FontWeight.Black, color = SummerYellow, fontSize = 16.sp)
                        Text(p.description, fontSize = 13.sp, color = VenomWhite)
                        p.activeUntil?.let { Text("до ${fmt.format(it.toDate())}", fontSize = 11.sp, color = BrokenGray) }
                    }
                    if (used) Text("Использован", color = FreeGreen, fontSize = 12.sp)
                    else TextButton({ scope.launch { result = ProfileRepo.applyPromo(p.code) } }) {
                        Text("Применить", color = VenomGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab() {
    var showTerms by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        // Пользовательское соглашение — всегда в открытом доступе
        ListItem(
            headlineContent = { Text("Пользовательское соглашение и оферта") },
            leadingContent = { Icon(Icons.Filled.Description, null, tint = SummerYellow) },
            colors = ListItemDefaults.colors(containerColor = VenomSurface),
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { showTerms = true }
        )
        ListItem(
            headlineContent = { Text("Выйти из аккаунта") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = BusyRed) },
            colors = ListItemDefaults.colors(containerColor = VenomSurface),
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                Firebase.auth.signOut()
                // Простой способ перезапуска потока авторизации — убить процесс UI-стека
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        )
        ListItem(
            headlineContent = { Text("Версия приложения") },
            supportingContent = { Text("1.0 · VENOM Club") },
            leadingContent = { Icon(Icons.Filled.Info, null, tint = AquaPool) },
            colors = ListItemDefaults.colors(containerColor = VenomSurface),
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
        )
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            containerColor = VenomSurface,
            title = { Text("Соглашение и оферта", color = VenomWhite) },
            text = {
                Column(Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
                    Text(com.venom.club.auth.TERMS_TEXT, color = VenomWhite.copy(alpha = .85f), fontSize = 13.sp)
                }
            },
            confirmButton = { TextButton({ showTerms = false }) { Text("Закрыть", color = VenomGreen) } }
        )
    }
}
