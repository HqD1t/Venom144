package com.venom.club.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.venom.club.auth.AuthFlow
import com.venom.club.booking.BookingScreen
import com.venom.club.chat.ChatScreen
import com.venom.club.news.CommentsScreen
import com.venom.club.news.NewsScreen
import com.venom.club.profile.ProfileScreen
import com.venom.club.admin.AdminScreen
import com.venom.club.data.ProfileRepo
import com.venom.club.data.model.UserProfile
import com.venom.club.ui.theme.*
import kotlinx.coroutines.flow.flowOf

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object News : Tab("news", "Новости", Icons.Filled.Newspaper)
    data object Booking : Tab("booking", "Бронь", Icons.Filled.SportsEsports)
    data object Chat : Tab("chat", "Чат", Icons.Filled.ChatBubble)
    data object Profile : Tab("profile", "Профиль", Icons.Filled.Person)
}

val tabs = listOf(Tab.News, Tab.Booking, Tab.Chat, Tab.Profile)

@Composable
fun VenomNavGraph(activity: Activity) {
    var authed by remember { mutableStateOf(false) }
    if (!authed) {
        AuthFlow(activity, onDone = { authed = true })
        return
    }

    val nav = rememberNavController()
    val me by remember(authed) {
        ProfileRepo.uid?.let { ProfileRepo.profileFlow(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    // Разрешение на уведомления запрашиваем явно (Android 13+), а не молча
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = VenomBlack,
        bottomBar = { VenomBottomBar(nav, me) }
    ) { pad ->
        NavHost(
            nav, startDestination = Tab.News.route,
            modifier = Modifier.padding(pad),
            enterTransition = { fadeIn(tween(250)) + scaleIn(initialScale = .96f) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Tab.News.route) { NewsScreen(me) { id -> nav.navigate("comments/$id") } }
            composable("comments/{postId}") { back ->
                CommentsScreen(back.arguments?.getString("postId") ?: "", me) { nav.popBackStack() }
            }
            composable(Tab.Booking.route) { BookingScreen(me) }
            composable(Tab.Chat.route) { ChatScreen(me) }
            composable(Tab.Profile.route) { ProfileScreen(me) }
            composable("admin") { AdminScreen(me) }
        }
    }
}

@Composable
private fun VenomBottomBar(nav: NavHostController, me: UserProfile?) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar(containerColor = VenomSurface) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            val scale by animateFloatAsState(if (selected) 1.2f else 1f, spring(dampingRatio = .5f), label = "tabScale")
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                icon = { Icon(tab.icon, tab.label, Modifier.scale(scale)) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VenomGreen, selectedTextColor = VenomGreen,
                    indicatorColor = VenomGreen.copy(alpha = .15f),
                    unselectedIconColor = BrokenGray, unselectedTextColor = BrokenGray
                )
            )
        }
        if (me?.isAdmin == true) {
            NavigationBarItem(
                selected = current == "admin",
                onClick = { nav.navigate("admin") { launchSingleTop = true } },
                icon = { Icon(Icons.Filled.AdminPanelSettings, "Админ") },
                label = { Text("Админ") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SummerYellow, selectedTextColor = SummerYellow,
                    indicatorColor = SummerYellow.copy(alpha = .15f),
                    unselectedIconColor = BrokenGray, unselectedTextColor = BrokenGray
                )
            )
        }
    }
}