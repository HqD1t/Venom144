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
    val uid: String? get() = if (Demo.enabled) "demo" else Firebase.auth.currentUser?.uid

    fun profileFlow(uid: String): Flow<UserProfile?> =
        if (Demo.enabled) Demo.profile
        else db.document("users/$uid").snapshots().map { it.toObject<UserProfile>()?.copy(uid = uid) }

    suspend fun get(uid: String): UserProfile? =
        if (Demo.enabled) Demo.profile.value
        else db.document("users/$uid").get().await().toObject<UserProfile>()?.copy(uid = uid)

    suspend fun ensureCreated(phone8: String) {
        if (Demo.enabled) return
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
        if (Demo.enabled) return
        db.document("users/${uid!!}").update("pinHash", sha256(pin)).await()
    }

    suspend fun acceptTerms() {
        if (Demo.enabled) return
        db.document("users/${uid!!}").update("acceptedTermsAt", Timestamp.now()).await()
    }

    suspend fun updateNickname(name: String) {
        if (Demo.enabled) { Demo.profile.value = Demo.profile.value.copy(nickname = name); return }
        db.document("users/${uid!!}").update("nickname", name).await()
    }

    suspend fun uploadAvatar(local: Uri): String {
        if (Demo.enabled) {
            Demo.profile.value = Demo.profile.value.copy(avatarUrl = local.toString())
            return local.toString()
        }
        val ref = storage.reference.child("avatars/${uid!!}.jpg")
        ref.putFile(local).await()
        val url = ref.downloadUrl.await().toString()
        db.document("users/${uid!!}").update("avatarUrl", url).await()
        return url
    }

    fun promoCodesFlow(): Flow<List<PromoCode>> =
        if (Demo.enabled) Demo.promos
        else db.collection("promocodes").snapshots().map { it.toObjects<PromoCode>() }

    /** Для админа: база пользователей */
    fun allUsersFlow(): Flow<List<UserProfile>> =
        if (Demo.enabled) Demo.users
        else db.collection("users").orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<UserProfile>()?.copy(uid = d.id) } }

    suspend fun adminUpdateUser(uid: String, fields: Map<String, Any>) {
        if (Demo.enabled) {
            Demo.users.value = Demo.users.value.map { u ->
                if (u.uid != uid) u else u.copy(
                    favorite = fields["favorite"] as? Boolean ?: u.favorite,
                    adminNote = fields["adminNote"] as? String ?: u.adminNote,
                )
            }
            return
        }
        db.document("users/$uid").update(fields).await()
    }
}

object NewsRepo {
    fun postsFlow(): Flow<List<Post>> =
        if (Demo.enabled) Demo.posts
        else db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Post>()?.copy(id = d.id) } }

    suspend fun toggleLike(postId: String, uid: String, liked: Boolean) {
        if (Demo.enabled) {
            Demo.posts.value = Demo.posts.value.map { p ->
                if (p.id != postId) p else p.copy(likes = if (liked) p.likes - uid else p.likes + uid)
            }
            return
        }
        db.document("posts/$postId").update(
            "likes", if (liked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        ).await()
    }

    fun commentsFlow(postId: String): Flow<List<Comment>> =
        if (Demo.enabled) Demo.comments.map { it[postId].orEmpty() }
        else db.collection("posts/$postId/comments").orderBy("createdAt")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Comment>()?.copy(id = d.id) } }

    suspend fun addComment(postId: String, c: Comment) {
        if (Demo.enabled) {
            val nc = c.copy(id = Demo.newId(), createdAt = Demo.now())
            Demo.comments.value = Demo.comments.value + (postId to (Demo.comments.value[postId].orEmpty() + nc))
            Demo.posts.value = Demo.posts.value.map { p ->
                if (p.id != postId) p else p.copy(commentCount = p.commentCount + 1)
            }
            return
        }
        db.collection("posts/$postId/comments").add(c.copy(createdAt = Timestamp.now())).await()
        db.document("posts/$postId").update("commentCount", FieldValue.increment(1)).await()
    }

    suspend fun toggleCommentLike(postId: String, commentId: String, uid: String, liked: Boolean) {
        if (Demo.enabled) {
            Demo.comments.value = Demo.comments.value + (postId to Demo.comments.value[postId].orEmpty().map { c ->
                if (c.id != commentId) c else c.copy(likes = if (liked) c.likes - uid else c.likes + uid)
            })
            return
        }
        db.document("posts/$postId/comments/$commentId").update(
            "likes", if (liked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        ).await()
    }

    /** Админ: создать пост (image опционально) */
    suspend fun createPost(text: String, image: Uri?) {
        if (Demo.enabled) {
            Demo.posts.value = listOf(
                Post(id = Demo.newId(), text = text, imageUrl = image?.toString() ?: "", createdAt = Demo.now())
            ) + Demo.posts.value
            return
        }
        var url = ""
        if (image != null) {
            val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
            ref.putFile(image).await()
            url = ref.downloadUrl.await().toString()
        }
        db.collection("posts").add(Post(text = text, imageUrl = url, createdAt = Timestamp.now())).await()
    }

    suspend fun deletePost(postId: String) {
        if (Demo.enabled) {
            Demo.posts.value = Demo.posts.value.filterNot { it.id == postId }
            return
        }
        db.document("posts/$postId").delete().await()
    }
}

object StationRepo {
    fun stationsFlow(): Flow<List<Station>> =
        if (Demo.enabled) Demo.stations
        else db.collection("stations").orderBy("number")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Station>()?.copy(id = d.id) } }

    suspend fun requestBooking(st: Station, me: UserProfile, startAt: Timestamp, hours: Int) {
        if (Demo.enabled) {
            Demo.bookings.value = Demo.bookings.value + Booking(
                id = Demo.newId(), stationId = st.id, stationTitle = st.title, uid = me.uid,
                nickname = me.nickname, phone = me.phone, startAt = startAt, hours = hours,
                createdAt = Demo.now())
            return
        }
        db.collection("bookings").add(
            Booking(stationId = st.id, stationTitle = st.title, uid = me.uid,
                nickname = me.nickname, phone = me.phone,
                startAt = startAt, hours = hours, createdAt = Timestamp.now())
        ).await()
    }

    fun myBookingsFlow(uid: String): Flow<List<Booking>> =
        if (Demo.enabled) Demo.bookings.map { l -> l.filter { it.uid == uid } }
        else db.collection("bookings").whereEqualTo("uid", uid)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Booking>()?.copy(id = d.id) }
                .sortedByDescending { it.createdAt } }

    fun pendingBookingsFlow(): Flow<List<Booking>> =
        if (Demo.enabled) Demo.bookings.map { l -> l.filter { it.status == BookingStatus.PENDING.name } }
        else db.collection("bookings").whereEqualTo("status", BookingStatus.PENDING.name)
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<Booking>()?.copy(id = d.id) } }

    /** Админ: подтвердить бронь — станция помечается BOOKED с временем и ником */
    suspend fun approve(b: Booking) {
        val until = Timestamp(b.startAt!!.seconds + b.hours * 3600L, 0)
        if (Demo.enabled) {
            Demo.bookings.value = Demo.bookings.value.map {
                if (it.id == b.id) it.copy(status = BookingStatus.APPROVED.name) else it
            }
            Demo.stations.value = Demo.stations.value.map {
                if (it.id != b.stationId) it
                else it.copy(status = StationStatus.BOOKED.name, bookedUntil = until, bookedBy = b.nickname)
            }
            return
        }
        db.document("bookings/${b.id}").update("status", BookingStatus.APPROVED.name).await()
        db.document("stations/${b.stationId}").update(
            mapOf("status" to StationStatus.BOOKED.name, "bookedUntil" to until, "bookedBy" to b.nickname)
        ).await()
    }

    suspend fun reject(b: Booking) {
        if (Demo.enabled) {
            Demo.bookings.value = Demo.bookings.value.map {
                if (it.id == b.id) it.copy(status = BookingStatus.REJECTED.name) else it
            }
            return
        }
        db.document("bookings/${b.id}").update("status", BookingStatus.REJECTED.name).await()
    }

    /** Админ: смена статуса станции (сломан / тех.работы / свободен ...) */
    suspend fun setStatus(stationId: String, status: StationStatus, note: String = "") {
        if (Demo.enabled) {
            Demo.stations.value = Demo.stations.value.map {
                if (it.id != stationId) it
                else it.copy(status = status.name, statusNote = note, bookedUntil = null, bookedBy = "")
            }
            return
        }
        db.document("stations/$stationId").update(
            mapOf("status" to status.name, "statusNote" to note,
                "bookedUntil" to null, "bookedBy" to "")
        ).await()
    }
}

object ChatRepo {
    fun messagesFlow(chatUid: String): Flow<List<ChatMessage>> =
        if (Demo.enabled) Demo.chatMessages.map { it[chatUid].orEmpty() }
        else db.collection("chats/$chatUid/messages").orderBy("createdAt")
            .snapshots().map { s -> s.documents.mapNotNull { d -> d.toObject<ChatMessage>()?.copy(id = d.id) } }

    suspend fun send(chatUid: String, me: UserProfile, text: String, image: Uri?, asAdmin: Boolean) {
        if (Demo.enabled) {
            val msg = ChatMessage(id = Demo.newId(), fromUid = me.uid, fromAdmin = asAdmin,
                text = text, imageUrl = image?.toString() ?: "", createdAt = Demo.now())
            Demo.chatMessages.value =
                Demo.chatMessages.value + (chatUid to (Demo.chatMessages.value[chatUid].orEmpty() + msg))
            Demo.chatHeads.value = Demo.chatHeads.value.map { h ->
                if (h.uid != chatUid) h
                else h.copy(lastMessage = text.ifBlank { "📷 Фото" }, lastAt = Demo.now(),
                    unreadForAdmin = if (asAdmin) 0 else h.unreadForAdmin + 1)
            }
            return
        }
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
        if (Demo.enabled) Demo.chatHeads
        else db.collection("chats").orderBy("lastAt", Query.Direction.DESCENDING)
            .snapshots().map { it.toObjects<ChatHead>() }

    suspend fun setChatMeta(uid: String, favorite: Boolean? = null, note: String? = null) {
        if (Demo.enabled) {
            Demo.chatHeads.value = Demo.chatHeads.value.map { h ->
                if (h.uid != uid) h else h.copy(favorite = favorite ?: h.favorite, note = note ?: h.note)
            }
            return
        }
        val m = buildMap<String, Any> {
            favorite?.let { put("favorite", it) }
            note?.let { put("note", it) }
        }
        if (m.isNotEmpty()) db.document("chats/$uid").update(m).await()
    }

    /** Настраиваемые шаблоны ответов админа (adminTemplates/{id}) */
    fun templatesFlow(): Flow<List<Pair<String, String>>> =
        if (Demo.enabled) Demo.templates
        else db.collection("adminTemplates").snapshots()
            .map { s -> s.documents.map { it.id to (it.getString("text") ?: "") } }

    suspend fun saveTemplate(id: String?, text: String) {
        if (Demo.enabled) {
            Demo.templates.value =
                if (id == null) Demo.templates.value + (Demo.newId() to text)
                else Demo.templates.value.map { if (it.first == id) id to text else it }
            return
        }
        if (id == null) db.collection("adminTemplates").add(mapOf("text" to text)).await()
        else db.document("adminTemplates/$id").update("text", text).await()
    }

    suspend fun deleteTemplate(id: String) {
        if (Demo.enabled) {
            Demo.templates.value = Demo.templates.value.filterNot { it.first == id }
            return
        }
        db.document("adminTemplates/$id").delete().await()
    }
}
