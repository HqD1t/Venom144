package com.venom.club.data

import com.google.firebase.Timestamp
import com.venom.club.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import java.util.UUID

/**
 * Демо-режим для тестов без регистрации и Firebase.
 * Все данные живут в памяти: лайки, брони, чат и админка полностью кликабельны.
 */
object Demo {
    var enabled = false

    private fun ts(minusMin: Long = 0) = Timestamp(Date(System.currentTimeMillis() - minusMin * 60_000))

    val me = UserProfile(
        uid = "demo", phone = "89000000000", nickname = "Демо Игрок",
        isAdmin = true, acceptedTermsAt = ts(), createdAt = ts(60 * 24 * 30)
    )

    val profile = MutableStateFlow(me)

    val posts = MutableStateFlow(listOf(
        Post(id = "p1", text = "🔥 ЛЕТНИЙ ТУРНИР ПО CS2! Суббота 19:00, призовой фонд 10 000 ₽. Регистрация на ресепшн или в этом чате!", createdAt = ts(120), likes = listOf("u1", "u2", "u3"), commentCount = 2),
        Post(id = "p2", text = "☀️ Летняя акция: с 10:00 до 14:00 час на PRO-ряду по цене обычного. Только в будни!", createdAt = ts(60 * 24), likes = listOf("u1"), commentCount = 1),
        Post(id = "p3", text = "Обновили видеозал PS5 — теперь 4 джойстика на каждой точке и новые релизы 🎮", createdAt = ts(60 * 48), likes = emptyList(), commentCount = 0),
    ))

    val comments = MutableStateFlow(mapOf(
        "p1" to listOf(
            Comment(id = "c1", uid = "u1", nickname = "xXShadowXx", text = "Записывайте нашу пятёрку!", createdAt = ts(100), likes = listOf("u2")),
            Comment(id = "c2", uid = "u2", nickname = "Kira", text = "А зрителям можно?", replyTo = "c1", replyToNickname = "xXShadowXx", createdAt = ts(90)),
        ),
        "p2" to listOf(
            Comment(id = "c3", uid = "u3", nickname = "ProGamer55", text = "Топ акция 👍", createdAt = ts(60 * 20)),
        ),
    ))

    val stations = MutableStateFlow(buildList {
        for (n in 1..10) add(Station(id = "st$n", number = n, title = "PC $n", type = "PC", zone = "Общий зал",
            status = if (n in listOf(3, 7)) StationStatus.BUSY.name else StationStatus.FREE.name))
        for (n in 11..14) add(Station(id = "st$n", number = n, title = "PC $n PRO", type = "PC", zone = "PRO ряд",
            status = if (n == 12) StationStatus.BOOKED.name else StationStatus.FREE.name,
            bookedUntil = if (n == 12) ts(-90) else null, bookedBy = if (n == 12) "Kira" else ""))
        for (n in 15..17) add(Station(id = "st$n", number = n, title = "PC $n", type = "PC", zone = "Общий зал",
            status = if (n == 16) StationStatus.BROKEN.name else StationStatus.FREE.name,
            statusNote = if (n == 16) "ждём видеокарту" else ""))
        for (n in 18..21) add(Station(id = "st$n", number = n, title = "Консоль $n", type = "PS5", zone = "Консоли"))
        add(Station(id = "st22", number = 22, title = "VIP 22", type = "PC", zone = "VIP"))
        for (n in 23..26) add(Station(id = "st$n", number = n, title = "${n - 22} комната", type = "PS5", zone = "PS5 комнаты",
            status = if (n == 24) StationStatus.BUSY.name else StationStatus.FREE.name))
    })

    val bookings = MutableStateFlow(listOf(
        Booking(id = "b1", stationId = "st5", stationTitle = "PC 5", uid = "u1", nickname = "xXShadowXx",
            phone = "89001112233", startAt = ts(-30), hours = 2, createdAt = ts(5))
    ))

    val chatMessages = MutableStateFlow(mapOf(
        "demo" to listOf(
            ChatMessage(id = "m1", fromUid = "admin", fromAdmin = true, text = "Привет! Чем помочь? 😎", createdAt = ts(30)),
        ),
        "u1" to listOf(
            ChatMessage(id = "m2", fromUid = "u1", text = "Запишите на турнир: команда Shadow5", createdAt = ts(95)),
        ),
    ))

    val chatHeads = MutableStateFlow(listOf(
        ChatHead(uid = "demo", nickname = "Демо Игрок", lastMessage = "Привет! Чем помочь? 😎", lastAt = ts(30)),
        ChatHead(uid = "u1", nickname = "xXShadowXx", lastMessage = "Запишите на турнир: команда Shadow5",
            lastAt = ts(95), unreadForAdmin = 1, favorite = true, note = "капитан команды"),
    ))

    val promos = MutableStateFlow(listOf(
        PromoCode(code = "SUMMER25", description = "Скидка 25% на ночной пакет", activeUntil = ts(-60 * 24 * 20)),
        PromoCode(code = "VENOM144", description = "+1 час при покупке 3 часов", activeUntil = ts(-60 * 24 * 10), usedBy = listOf("demo")),
    ))

    val users = MutableStateFlow(listOf(
        me,
        UserProfile(uid = "u1", phone = "89001112233", nickname = "xXShadowXx", favorite = true, adminNote = "капитан Shadow5", createdAt = ts(60 * 24 * 90)),
        UserProfile(uid = "u2", phone = "89002223344", nickname = "Kira", createdAt = ts(60 * 24 * 45)),
        UserProfile(uid = "u3", phone = "89003334455", nickname = "ProGamer55", createdAt = ts(60 * 24 * 10)),
    ))

    val templates = MutableStateFlow(listOf(
        "t1" to "Здравствуйте! Сейчас проверю и отвечу.",
        "t2" to "Бронь подтверждена, ждём вас! 🤙",
    ))

    val stats = GizmoStats(totalSpent = 12500.0, totalHours = 342.5, sessionsCount = 87, balance = 250.0)

    fun newId() = UUID.randomUUID().toString()
    fun now(): Timestamp = Timestamp.now()
}
