package com.venom.club.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.venom.club.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private val db get() = Firebase.firestore
private val storage get() = Firebase.storage

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

/** Нормализация телефона под формат клуба: 8XXXXXXXXXX */
fun normalizePhone(raw: String): String? {
    val d = raw.filter { it.isDigit() }
    return when {
        d.length == 11 && d.startsWith("8") -> d
        d.length == 11 && d.startsWith("7") -> "8" + d.drop(1)
        d.length == 10 && d.startsWith("9") -> "8$d"
        else -> null
    }
}

/** В E.164 для Firebase SMS: 8XXXXXXXXXX -> +7XXXXXXXXXX */
fun toE164(phone8: String): String = "+7" + phone8.drop(1)

object ProfileRepo {
    val uid: String? get() = Firebase.auth.currentUser?.uid

    fun profileFlow(uid: String): Flow<UserProfile?> =
        db.document("users/$uid").snapshots().map { it.toObject<UserProfile>()?.copy(uid = uid) }

    suspend fun get(uid: String): UserProfile? =
        db.document("users/$uid").get().await().toObject<UserProfile>()?.copy(uid = uid)

    suspend fun ensureCreated(phone8: String) {
        val u = uid ?: return
        val ref = db.document("users/$u")
        if (!ref.get().await().exists()) {
            ref.set(
                UserProfile(uid = u, phone = phone8, nickname = "Игрок ${phone8.takeLast(4)}",
                    createdAt = Timestamp.now())
            ).await()
        }
    }

    suspend fun setPin(pin: String) {
        db.document("users/${uid!!}").update("pinHash", sha256(pin)).await()
    }

    suspend fun acceptTerms() {
        db.document("users/${uid!!}").update("acceptedTermsAt", Timestamp.now()).await()
    }

    suspend fun updateNickname(name: String) {
        db.document("users/${uid!!}").update("nickname", name).await()
    }

    suspend fun uploadAvatar(local: Uri): String {
        val ref = storage.reference.child("avatars/${uid!!}.jpg")
        ref.putFile(local).await()
        val url = ref.downloadUrl.await().toString()
        db.document("users/${uid!!}").update("avatarUrl", url).await()
        return url
    }

    fun promoCodesFlow(): Flow<List<PromoCode>> =
        db.collection("promocodes").snapshots().map { it.toObjects<PromoCode>() }

    /** Для админа: база пользователей */
    fun allUsersFlow(): Flow<List<UserProfile>> =
        db.collection("users").orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<UserProfile>()?.copy(uid = d.id) } }

    suspend fun adminUpdateUser(uid: String, fields: Map<String, Any>) {
        db.document("users/$uid").update(fields).await()
    }
}

object NewsRepo {
    fun postsFlow(): Flow<List<Post>> =
        db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Post>()?.copy(id = d.id) } }

    suspend fun toggleLike(postId: String, uid: String, liked: Boolean) {
        db.document("posts/$postId").update(
            "likes", if (liked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        ).await()
    }

    fun commentsFlow(postId: String): Flow<List<Comment>> =
        db.collection("posts/$postId/comments").orderBy("createdAt")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Comment>()?.copy(id = d.id) } }

    suspend fun addComment(postId: String, c: Comment) {
        db.collection("posts/$postId/comments").add(c.copy(createdAt = Timestamp.now())).await()
        db.document("posts/$postId").update("commentCount", FieldValue.increment(1)).await()
    }

    suspend fun toggleCommentLike(postId: String, commentId: String, uid: String, liked: Boolean) {
        db.document("posts/$postId/comments/$commentId").update(
            "likes", if (liked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        ).await()
    }

    /** Админ: создать пост (image опционально) */
    suspend fun createPost(text: String, image: Uri?) {
        var url = ""
        if (image != null) {
            val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
            ref.putFile(image).await()
            url = ref.downloadUrl.await().toString()
        }
        db.collection("posts").add(Post(text = text, imageUrl = url, createdAt = Timestamp.now())).await()
    }

    suspend fun deletePost(postId: String) {
        db.document("posts/$postId").delete().await()
    }
}

object StationRepo {
    fun stationsFlow(): Flow<List<Station>> =
        db.collection("stations").orderBy("number")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Station>()?.copy(id = d.id) } }

    suspend fun requestBooking(st: Station, me: UserProfile, startAt: Timestamp, hours: Int) {
        db.collection("bookings").add(
            Booking(stationId = st.id, stationTitle = st.title, uid = me.uid,
                nickname = me.nickname, phone = me.phone,
                startAt = startAt, hours = hours, createdAt = Timestamp.now())
        ).await()
    }

    fun myBookingsFlow(uid: String): Flow<List<Booking>> =
        db.collection("bookings").whereEqualTo("uid", uid)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Booking>()?.copy(id = d.id) }
                .sortedByDescending { it.createdAt } }

    fun pendingBookingsFlow(): Flow<List<Booking>> =
        db.collection("bookings").whereEqualTo("status", BookingStatus.PENDING.name)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Booking>()?.copy(id = d.id) } }

    /** Админ: подтвердить бронь — станция помечается BOOKED с временем и ником */
    suspend fun approve(b: Booking) {
        val until = Timestamp(b.startAt!!.seconds + b.hours * 3600L, 0)
        db.document("bookings/${b.id}").update("status", BookingStatus.APPROVED.name).await()
        db.document("stations/${b.stationId}").update(
            mapOf("status" to StationStatus.BOOKED.name, "bookedUntil" to until, "bookedBy" to b.nickname)
        ).await()
    }

    suspend fun reject(b: Booking) {
        db.document("bookings/${b.id}").update("status", BookingStatus.REJECTED.name).await()
    }

    /** Админ: смена статуса станции (сломан / тех.работы / свободен ...) */
    suspend fun setStatus(stationId: String, status: StationStatus, note: String = "") {
        db.document("stations/$stationId").update(
            mapOf("status" to status.name, "statusNote" to note,
                "bookedUntil" to null, "bookedBy" to "")
        ).await()
    }
}

object ChatRepo {
    fun messagesFlow(chatUid: String): Flow<List<ChatMessage>> =
        db.collection("chats/$chatUid/messages").orderBy("createdAt")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<ChatMessage>()?.copy(id = d.id) } }

    suspend fun send(chatUid: String, me: UserProfile, text: String, image: Uri?, asAdmin: Boolean) {
        var url = ""
        if (image != null) {
            val ref = storage.reference.child("chat/$chatUid/${UUID.randomUUID()}.jpg")
            ref.putFile(image).await()
            url = ref.downloadUrl.await().toString()
        }
        db.collection("chats/$chatUid/messages").add(
            ChatMessage(fromUid = me.uid, fromAdmin = asAdmin, text = text, imageUrl = url,
                createdAt = Timestamp.now())
        ).await()
        val head = buildMap<String, Any> {
            put("uid", chatUid)
            put("lastMessage", text.ifBlank { "📷 Фото" })
            put("lastAt", Timestamp.now())
            if (asAdmin) put("unreadForAdmin", 0)
            else {
                // шапку чата подписываем данными пользователя, а не админа
                put("nickname", me.nickname); put("avatarUrl", me.avatarUrl)
                put("unreadForAdmin", FieldValue.increment(1))
            }
        }
        db.document("chats/$chatUid").set(head, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun chatHeadsFlow(): Flow<List<ChatHead>> =
        db.collection("chats").orderBy("lastAt", Query.Direction.DESCENDING)
            .snapshots().map { it.toObjects<ChatHead>() }

    suspend fun setChatMeta(uid: String, favorite: Boolean? = null, note: String? = null) {
        val m = buildMap<String, Any> {
            favorite?.let { put("favorite", it) }
            note?.let { put("note", it) }
        }
        if (m.isNotEmpty()) db.document("chats/$uid").update(m).await()
    }

    /** Настраиваемые шаблоны ответов админа (adminTemplates/{id}) */
    fun templatesFlow(): Flow<List<Pair<String, String>>> =
        db.collection("adminTemplates").snapshots()
            .map { s -> s.documents.map { it.id to (it.getString("text") ?: "") } }

    suspend fun saveTemplate(id: String?, text: String) {
        if (id == null) db.collection("adminTemplates").add(mapOf("text" to text)).await()
        else db.document("adminTemplates/$id").update("text", text).await()
    }

    suspend fun deleteTemplate(id: String) {
        db.document("adminTemplates/$id").delete().await()
    }
}
