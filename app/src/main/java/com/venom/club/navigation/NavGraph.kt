package com.venom.club.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.venom.club.admin.AdminScreen
import com.venom.club.auth.AuthFlow
import com.venom.club.booking.BookingScreen
import com.venom.club.chat.ChatScreen
import com.venom.club.data.ProfileRepo
import com.venom.club.news.CommentsScreen
import com.venom.club.news.NewsScreen
import com.venom.club.profile.ProfileScreen
import com.venom.club.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class TabItem(val label: String, val icon: ImageVector)

private val userTabs = listOf(
    TabItem("Новости", Icons.Filled.Newspaper),
    TabItem("Бронь", Icons.Filled.SportsEsports),
    TabItem("Чат", Icons.Filled.ChatBubble),
    TabItem("Профиль", Icons.Filled.Person),
)
private val adminTab = TabItem("Админ", Icons.Filled.AdminPanelSettings)

@Composable
fun VenomNavGraph(activity: Activity) {
    var authed by remember { mutableStateOf(false) }
    if (!authed) {
        AuthFlow(activity, onDone = { authed = true })
        return
    }

    val me by remember(authed) {
        ProfileRepo.uid?.let { ProfileRepo.profileFlow(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    // Разрешение на уведомления запрашиваем явно (Android 13+)
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isAdmin = me?.isAdmin == true
    val tabs = if (isAdmin) userTabs + adminTab else userTabs
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Комментарии — оверлей поверх вкладок
    var commentsFor by remember { mutableStateOf<String?>(null) }
    var shownPostId by remember { mutableStateOf("") }
    if (commentsFor != null) shownPostId = commentsFor!!

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = VenomBlack,
            bottomBar = {
                NavigationBar(containerColor = VenomSurface) {
                    tabs.forEachIndexed { i, tab ->
                        val selected = pagerState.currentPage == i
                        val iconScale by animateFloatAsState(
                            if (selected) 1.15f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tabScale"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                            icon = { Icon(tab.icon, tab.label, Modifier.scale(iconScale)) },
                            label = {
                                Text(tab.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VenomGreen, selectedTextColor = VenomGreen,
                                indicatorColor = VenomGreen.copy(alpha = .12f),
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        ) { pad ->
            // Свайп между вкладками
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(pad),
                key = { it }
            ) { page ->
                when (page) {
                    0 -> NewsScreen(me) { id -> commentsFor = id }
                    1 -> BookingScreen(me)
                    2 -> ChatScreen(me)
                    3 -> ProfileScreen(me)
                    4 -> AdminScreen(me)
                }
            }
        }

        // Экран комментариев выезжает справа
        AnimatedVisibility(
            visible = commentsFor != null,
            enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)),
            exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
        ) {
            BackHandler { commentsFor = null }
            CommentsScreen(shownPostId, me) { commentsFor = null }
        }
    }
}
