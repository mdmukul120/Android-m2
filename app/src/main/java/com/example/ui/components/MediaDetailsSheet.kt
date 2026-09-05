package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.ApiService
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    media: MediaItem,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onPlayMedia: (title: String, streamUrl: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExtracting by remember { mutableStateOf(false) }
    var extractionError by remember { mutableStateOf("") }
    var currentItem by remember { mutableStateOf(media) }
    var episodes by remember { mutableStateOf(media.episodes) }
    var selectedQualityIndex by remember { mutableStateOf(0) }

    // If item is from extractor and hasn't fetched info yet, fetch it
    LaunchedEffect(media) {
        if (media.provider.isNotEmpty() && media.link.isNotEmpty() && media.qualities.isEmpty()) {
            isExtracting = true
            try {
                val info = ApiService.fetchExtractorInfo(media.provider, media.link)
                if (info != null) {
                    currentItem = currentItem.copy(
                        title = if (info.title.isNotEmpty()) info.title else currentItem.title,
                        description = if (info.synopsis.isNotEmpty()) info.synopsis else currentItem.description,
                        posterUrl = if (info.image.isNotEmpty()) info.image else currentItem.posterUrl,
                        qualities = info.qualities
                    )
                    if (info.episodesLink.isNotEmpty()) {
                        val epList = ApiService.fetchExtractorEpisodes(media.provider, info.episodesLink)
                        episodes = epList
                    }
                }
            } catch (e: Exception) {
                extractionError = e.message ?: "Extraction failed"
            } finally {
                isExtracting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF13141F),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().testTag("media_details_sheet")) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header Backdrop / Poster
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        AsyncImage(
                            model = currentItem.backdropUrl.ifEmpty { currentItem.posterUrl },
                            contentDescription = currentItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x33000000),
                                            Color(0xBB13141F),
                                            Color(0xFF13141F)
                                        )
                                    )
                                )
                        )

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Details block
                item {
                    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                        Text(
                            text = currentItem.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentItem.rating,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = "•", color = MukulTextSecondary)
                            Text(text = currentItem.year, color = MukulTextSecondary, fontSize = 12.sp)
                            Text(text = "•", color = MukulTextSecondary)
                            Text(text = currentItem.duration, color = MukulTextSecondary, fontSize = 12.sp)
                            Text(text = "•", color = MukulTextSecondary)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MukulRedPrimary)
                            ) {
                                Text(
                                    text = currentItem.label,
                                    color = MukulRedPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Synopsis
                        if (currentItem.description.isNotEmpty()) {
                            Text(
                                text = currentItem.description,
                                color = MukulTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Quality selector if available
                        if (currentItem.qualities.isNotEmpty()) {
                            Text(
                                text = if (isBangla) "কোয়ালিটি নির্বাচন করুন:" else "Select Quality:",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentItem.qualities.forEachIndexed { idx, q ->
                                    val isSelected = selectedQualityIndex == idx
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MukulRedPrimary else Color(0xFF222436),
                                        modifier = Modifier.clickable { selectedQualityIndex = idx }
                                    ) {
                                        Text(
                                            text = q.quality.ifEmpty { "HD" },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Primary Action Buttons: Play Now & In-App Download
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Watch Now Button
                            Button(
                                onClick = {
                                    if (currentItem.streamUrl.isNotEmpty()) {
                                        onPlayMedia(currentItem.title, currentItem.streamUrl)
                                        onDismiss()
                                    } else if (currentItem.provider.isNotEmpty()) {
                                        // Resolve stream asynchronously
                                        coroutineScope.launch {
                                            isExtracting = true
                                            val direct = currentItem.qualities.getOrNull(selectedQualityIndex)?.directLink
                                                ?: currentItem.link
                                            val servers = ApiService.extractStreamServers(currentItem.provider, direct)
                                            isExtracting = false
                                            if (servers.isNotEmpty()) {
                                                val streamLink = servers.firstOrNull { it.server.contains("CF Worker", true) }?.link
                                                    ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                    ?: servers[0].link
                                                onPlayMedia(currentItem.title, streamLink)
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, if (isBangla) "স্ট্রিম সার্ভার প্রস্তুত হচ্ছে, পুনরায় চেষ্টা করুন" else "Stream server unavailable, please try again", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("details_watch_now_button")
                            ) {
                                if (isExtracting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "এখন দেখুন" else "Watch Now",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            // Download Inside App Button
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (currentItem.downloadUrl.isNotEmpty()) {
                                            DownloadHelper.downloadMovie(context, currentItem.title, currentItem.downloadUrl)
                                        } else if (currentItem.streamUrl.isNotEmpty()) {
                                            DownloadHelper.downloadMovie(context, currentItem.title, currentItem.streamUrl)
                                        } else if (currentItem.provider.isNotEmpty()) {
                                            isExtracting = true
                                            val direct = currentItem.qualities.getOrNull(selectedQualityIndex)?.directLink
                                                ?: currentItem.link
                                            val servers = ApiService.extractStreamServers(currentItem.provider, direct)
                                            isExtracting = false
                                            if (servers.isNotEmpty()) {
                                                val downloadLink = servers.firstOrNull { it.server.contains("GDrive", true) }?.link
                                                    ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                    ?: servers[0].link
                                                DownloadHelper.downloadMovie(context, currentItem.title, downloadLink)
                                            } else {
                                                Toast.makeText(context, if (isBangla) "ডাউনলোড লিংক প্রস্তুত করা সম্ভব হয়নি" else "Download link unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MukulRedPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("details_download_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = MukulRedPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "ডাউনলোড" else "Download",
                                        color = MukulRedPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Episodes list if Series
                if (episodes.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Text(
                                text = if (isBangla) "পর্বসমূহ (${episodes.size})" else "Episodes (${episodes.size})",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(episodes) { ep ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1D2C),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        if (ep.streamUrl.isNotEmpty()) {
                                            onPlayMedia("${currentItem.title} - ${ep.title}", ep.streamUrl)
                                            onDismiss()
                                        } else if (ep.provider.isNotEmpty() && ep.link.isNotEmpty()) {
                                            isExtracting = true
                                            val servers = ApiService.extractStreamServers(ep.provider, ep.link)
                                            isExtracting = false
                                            if (servers.isNotEmpty()) {
                                                val streamLink = servers.firstOrNull { it.server.contains("CF Worker", true) }?.link
                                                    ?: servers[0].link
                                                onPlayMedia("${currentItem.title} - ${ep.title}", streamLink)
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MukulRedPrimary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ep.duration,
                                        color = MukulTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
