package com.venom.club.data.model

import com.google.firebase.Timestamp

/** Профиль пользователя (Firestore: users/{uid}) */
data class UserProfile(
    val uid: String = "",
    val phone: String = "",          // формат 8XXXXXXXXXX — как в Gizmo
    val nickname: String = "",
    val avatarUrl: String = "",
    val isAdmin: Boolean = false,
    val gizmoUserId: Int? = null,
    val pinHash: String = "",
    val acceptedTermsAt: Timestamp? = null,
    val fcmToken: String = "",
    val createdAt: Timestamp? = null,
    val adminNote: String = "",      // заметка админа о пользователе
    val favorite: Boolean = false,   // избранный чат у админа
)

/** Пост в новостях (Firestore: posts/{id}) */
data class Post(
    val id: String = "",
    val authorName: String = "VENOM",
    val text: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val likes: List<String> = emptyList(),   // uid лайкнувших
    val commentCount: Int = 0,
)

/** Комментарий (posts/{postId}/comments/{id}); replyTo — id родительского комментария */
data class Comment(
    val id: String = "",
    val uid: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val text: String = "",
    val replyTo: String? = null,
    val replyToNickname: String? = null,
    val likes: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
)

enum class StationType { PC, PS5 }
enum class StationStatus { FREE, BUSY, BOOKED, BROKEN, MAINTENANCE }

/** Станция клуба (Firestore: stations/{id}); связана с Gizmo host по gizmoHostId */
data class Station(
    val id: String = "",
    val number: Int = 0,
    val title: String = "",
    val type: String = StationType.PC.name,
    val zone: String = "",            // "Общий зал", "Видеозал", "PS5 #1"...
    val gizmoHostId: Int? = null,
    val status: String = StationStatus.FREE.name,
    val statusNote: String = "",      // причина (сломан / тех.работы)
    val bookedUntil: Timestamp? = null,
    val bookedBy: String = "",        // nickname для показа другим
)

enum class BookingStatus { PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED }

/** Заявка на бронь (Firestore: bookings/{id}) */
data class Booking(
    val id: String = "",
    val stationId: String = "",
    val stationTitle: String = "",
    val uid: String = "",
    val nickname: String = "",
    val phone: String = "",
    val startAt: Timestamp? = null,
    val hours: Int = 1,
    val status: String = BookingStatus.PENDING.name,
    val createdAt: Timestamp? = null,
)

/** Сообщение чата (chats/{uid}/messages/{id}) */
data class ChatMessage(
    val id: String = "",
    val fromUid: String = "",
    val fromAdmin: Boolean = false,
    val text: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
)

/** Шапка чата для списка у админа (chats/{uid}) */
data class ChatHead(
    val uid: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val lastAt: Timestamp? = null,
    val unreadForAdmin: Int = 0,
    val favorite: Boolean = false,
    val note: String = "",
)

/** Промокод (promocodes/{code}) */
data class PromoCode(
    val code: String = "",
    val description: String = "",
    val activeUntil: Timestamp? = null,
    val usedBy: List<String> = emptyList(),
)

/** Статистика из Gizmo через мини-сервер */
data class GizmoStats(
    val totalSpent: Double = 0.0,
    val totalHours: Double = 0.0,
    val sessionsCount: Int = 0,
    val balance: Double = 0.0,
)
