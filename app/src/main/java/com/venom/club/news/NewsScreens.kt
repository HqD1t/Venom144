package com.venom.club.news

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.venom.club.data.NewsRepo
import com.venom.club.data.model.Comment
import com.venom.club.data.model.Post
import com.venom.club.data.model.UserProfile
import com.venom.club.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFmt = SimpleDateFormat("d MMMM, HH:mm", Locale("ru"))

@Composable
fun NewsScreen(me: UserProfile?, onOpenComments: (String) -> Unit) {
    val posts by NewsRepo.postsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize().summerBackground(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post, myUid = me?.uid ?: "",
                onLike = { scope.launch {
                    NewsRepo.toggleLike(post.id, me?.uid ?: return@launch, post.likes.contains(me.uid))
                } },
                onComments = { onOpenComments(post.id) }
            )
        }
    }
}

@Composable
fun PostCard(post: Post, myUid: String, onLike: () -> Unit, onComments: () -> Unit) {
    val ctx = LocalContext.current
    val liked = post.likes.contains(myUid)
    val likeScale by animateFloatAsState(
        if (liked) 1.25f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "like"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VenomSurface),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(SummerGradient))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(post.authorName, fontWeight = FontWeight.Bold, color = VenomWhite)
                    Text(post.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "",
                        fontSize = 12.sp, color = BrokenGray)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(post.text, color = VenomWhite, fontSize = 15.sp)
            if (post.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    post.imageUrl, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike, Modifier.scale(likeScale)) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Лайк", tint = if (liked) SunsetCoral else BrokenGray)
                }
                Text("${post.likes.size}", color = VenomWhite)
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = onComments) {
                    Icon(Icons.Filled.ModeComment, "Комментарии", tint = BrokenGray)
                }
                Text("${post.commentCount}", color = VenomWhite)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    // Репост в другие приложения (ВК, ТГ и т.д.) через системный share
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "🎮 VENOM CLUB:\n${post.text}")
                    }
                    ctx.startActivity(Intent.createChooser(share, "Поделиться"))
                }) { Icon(Icons.Filled.Share, "Поделиться", tint = BrokenGray) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(postId: String, me: UserProfile?, onBack: () -> Unit) {
    val comments by NewsRepo.commentsFlow(postId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Comment?>(null) }

    Column(Modifier.fillMaxSize().summerBackground()) {
        TopAppBar(
            title = { Text("Комментарии") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = VenomSurface,
                titleContentColor = VenomWhite, navigationIconContentColor = VenomWhite)
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(comments, key = { it.id }) { c ->
                CommentItem(c, me?.uid ?: "",
                    onLike = { scope.launch {
                        NewsRepo.toggleCommentLike(postId, c.id, me?.uid ?: return@launch, c.likes.contains(me.uid))
                    } },
                    onReply = { replyTo = c })
            }
        }
        replyTo?.let { r ->
            Row(Modifier.fillMaxWidth().background(VenomSurface).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Ответ для ${r.nickname}", color = AquaPool, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton({ replyTo = null }) { Text("✕", color = BrokenGray) }
            }
        }
        Row(Modifier.fillMaxWidth().background(VenomSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Комментарий...") },
                shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val t = text.trim()
                if (t.isEmpty() || me == null) return@IconButton
                scope.launch {
                    NewsRepo.addComment(postId, Comment(
                        uid = me.uid, nickname = me.nickname, avatarUrl = me.avatarUrl,
                        text = t, replyTo = replyTo?.id, replyToNickname = replyTo?.nickname))
                    text = ""; replyTo = null
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = SunsetOrange) }
        }
    }
}

@Composable
private fun CommentItem(c: Comment, myUid: String, onLike: () -> Unit, onReply: () -> Unit) {
    val liked = c.likes.contains(myUid)
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VenomSurface),
        modifier = Modifier.fillMaxWidth().padding(start = if (c.replyTo != null) 24.dp else 0.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(c.avatarUrl.ifBlank { null }, null,
                    Modifier.size(28.dp).clip(CircleShape).background(SummerGradient))
                Spacer(Modifier.width(8.dp))
                Text(c.nickname, fontWeight = FontWeight.SemiBold, color = VenomWhite, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(c.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "",
                    fontSize = 11.sp, color = BrokenGray)
            }
            c.replyToNickname?.let {
                Text("↪ $it", color = AquaPool, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(c.text, color = VenomWhite, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onLike, Modifier.size(32.dp)) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Лайк",
                        Modifier.size(16.dp), tint = if (liked) SunsetCoral else BrokenGray)
                }
                Text("${c.likes.size}", fontSize = 12.sp, color = VenomWhite)
                Spacer(Modifier.width(12.dp))
                TextButton(onReply) {
                    Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(14.dp), tint = BrokenGray)
                    Text(" Ответить", fontSize = 12.sp, color = BrokenGray)
                }
            }
        }
    }
}
