package com.example.ui.screens.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class LocalMediaCopy(
    val fileUri: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationSeconds: Int,
    val resolution: String
)

private fun getPickedDisplayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
        }.orEmpty()
    }.getOrDefault("").ifBlank { "Selected media" }
}

private suspend fun copyMediaToPrivateStorage(
    context: Context,
    sourceUri: Uri,
    mediaType: MediaType
): LocalMediaCopy = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(sourceUri).orEmpty()
    val correctType = if (mediaType == MediaType.VIDEO) mime.startsWith("video/") else mime.startsWith("image/")
    require(correctType) { "The selected file does not match the chosen photo/video type." }

    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        ?: if (mediaType == MediaType.VIDEO) "mp4" else "jpg"
    val directory = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
    val destination = File(directory, "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
    try {
        val input = resolver.openInputStream(sourceUri) ?: error("Unable to open the selected media file.")
        input.use { source ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 250L * 1024L * 1024L) { "Media files must be 250 MB or smaller." }
                    output.write(buffer, 0, count)
                }
            }
        }
        require(destination.length() > 0L) { "The selected file is empty." }

        var durationSeconds = 0
        var resolution = ""
        if (mediaType == MediaType.VIDEO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(destination.absolutePath)
                durationSeconds = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.div(1000L) ?: 0L).toInt()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (width > 0 && height > 0) resolution = "${width}×${height}"
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) resolution = "${bounds.outWidth}×${bounds.outHeight}"
        }

        LocalMediaCopy(
            fileUri = Uri.fromFile(destination).toString(),
            displayName = getPickedDisplayName(context, sourceUri),
            sizeBytes = destination.length(),
            durationSeconds = durationSeconds,
            resolution = resolution
        )
    } catch (error: Exception) {
        destination.delete()
        throw error
    }
}

private suspend fun loadPhotoPreview(fileUri: String, maxDimension: Int = 1800): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(Uri.parse(fileUri).path ?: return@runCatching null)
        if (!file.exists()) return@runCatching null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()
}

private fun deleteLocalMediaFile(fileUri: String) {
    runCatching {
        val file = File(Uri.parse(fileUri).path ?: return)
        if (file.isFile && file.absolutePath.contains(File.separator + "media" + File.separator)) file.delete()
    }
}

private fun shareLocalMedia(context: Context, item: MediaItem) {
    runCatching {
        val file = File(Uri.parse(item.fileUri).path ?: error("Media file is unavailable."))
        require(file.exists()) { "Media file is no longer available on this device." }
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = if (item.type == MediaType.VIDEO) "video/*" else "image/*"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "K&B NET SERVICE: ${item.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share media"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaHubScreen(
    repository: NexusRepository,
    modifier: Modifier = Modifier
) {
    val mediaItems by repository.mediaItems.collectAsState()
    val mediaAlbums by repository.mediaAlbums.collectAsState()
    val customers by repository.customers.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Photos, 2: Videos, 3: Albums, 4: Customer Evidence, 5: Staff Uploads
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<MediaCategory?>(null) }

    // Dialog state
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedMediaForPreview by remember { mutableStateOf<MediaItem?>(null) }
    var selectedVideoForPlayback by remember { mutableStateOf<MediaItem?>(null) }

    // Metrics calculations
    val totalPhotos = mediaItems.count { it.type == MediaType.PHOTO }
    val totalVideos = mediaItems.count { it.type == MediaType.VIDEO }
    val totalBytes = mediaItems.sumOf { it.fileSizeBytes }
    val totalMb = totalBytes / (1024 * 1024.0)
    val pendingReviewCount = mediaItems.count { it.approvalStatus == MediaApprovalStatus.PENDING_REVIEW }

    val filteredList = mediaItems.filter { item ->
        val matchesTab = when (selectedTab) {
            0 -> true
            1 -> item.type == MediaType.PHOTO
            2 -> item.type == MediaType.VIDEO
            3 -> true
            4 -> item.customerId.isNotBlank()
            5 -> item.uploaderRole == UserRole.EMPLOYEE
            else -> true
        }
        val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.customerName.contains(searchQuery, ignoreCase = true) ||
                item.uploaderName.contains(searchQuery, ignoreCase = true) ||
                item.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesTab && matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("media_hub_screen_root")
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Photo & Video Media Hub",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Centralized Enterprise Proofs, Installations & Field Recordings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showUploadDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("upload_media_btn")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Storage & Metrics Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VividCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PermMedia, contentDescription = null, tint = VividCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Company Cloud Storage", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                text = "${String.format(Locale.ENGLISH, "%.1f", totalMb)} MB of 500 GB Secured Cloud Allocation",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (pendingReviewCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldenAmber.copy(alpha = 0.15f),
                            modifier = Modifier.border(1.dp, GoldenAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = "$pendingReviewCount Pending Review",
                                color = GoldenAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (totalMb / 500000.0).toFloat().coerceIn(0.01f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ElectricBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📸 $totalPhotos Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("🎥 $totalVideos Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("📁 ${mediaAlbums.size} Albums", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("👥 ${mediaItems.map { it.uploaderId }.distinct().size} Uploaders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar & View Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search photos, videos, tags, customers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("media_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = "List View",
                            tint = if (!isGridView) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = ElectricBlue,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("All Media (${mediaItems.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Photos ($totalPhotos)", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Videos ($totalVideos)", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Albums (${mediaAlbums.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Customer Evidence", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Staff Uploads", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Media Grid or List
        if (selectedTab == 3) {
            // ALBUMS VIEW
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mediaAlbums) { alb ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alb.name, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(alb.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("Created by ${alb.createdBy} • ${alb.itemCount} items", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PermMedia, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Media Found in this Category", fontWeight = FontWeight.Bold)
                    Text("Upload photos or videos using the button above", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    MediaCardGridItem(
                        item = item,
                        onClick = {
                            if (item.type == MediaType.VIDEO) {
                                selectedVideoForPlayback = item
                            } else {
                                selectedMediaForPreview = item
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    MediaCardListItem(
                        item = item,
                        onClick = {
                            if (item.type == MediaType.VIDEO) {
                                selectedVideoForPlayback = item
                            } else {
                                selectedMediaForPreview = item
                            }
                        }
                    )
                }
            }
        }
    }

    // ==================== UPLOAD DIALOG ====================
    if (showUploadDialog) {
        UploadMediaDialog(
            customers = customers,
            currentUser = currentUser,
            onDismiss = { showUploadDialog = false },
            onUpload = { title, type, category, desc, tags, custId, custName, album, fileUri, resolution, dur, bytes ->
                currentUser?.let { uploader ->
                    repository.uploadMediaItem(
                        title = title,
                        type = type,
                        category = category,
                        description = desc,
                        tags = tags,
                        uploader = uploader,
                        customerId = custId,
                        customerName = custName,
                        albumName = album,
                        durationSeconds = dur,
                        fileSizeBytes = bytes,
                        fileUri = fileUri,
                        resolution = resolution
                    )
                }
                showUploadDialog = false
            }
        )
    }

    // ==================== PHOTO FULL-SCREEN VIEWER ====================
    selectedMediaForPreview?.let { item ->
        PhotoPreviewDialog(
            item = item,
            onDismiss = { selectedMediaForPreview = null },
            onShare = { shareLocalMedia(context, item) },
            onDelete = {
                deleteLocalMediaFile(item.fileUri)
                repository.deleteMediaItem(item.id)
                selectedMediaForPreview = null
            }
        )
    }

    // ==================== VIDEO PLAYER DIALOG ====================
    selectedVideoForPlayback?.let { item ->
        VideoPlayerDialog(
            item = item,
            onDismiss = { selectedVideoForPlayback = null },
            onShare = { shareLocalMedia(context, item) }
        )
    }
}

// ==============================================================================
// MEDIA GRID CARD COMPONENT
// ==============================================================================
@Composable
fun MediaCardGridItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = item.fileUri) {
        value = if (item.type == MediaType.PHOTO && item.fileUri.isNotBlank()) loadPhotoPreview(item.fileUri, 520) else null
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("media_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Visual Preview Canvas / Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        when (item.category) {
                            MediaCategory.CUSTOMER_INSTALLATION -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                            MediaCategory.FIBER_SPLICING_WORK -> Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81)))
                            MediaCategory.ROUTER_ONT_SETUP -> Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF065F46)))
                            MediaCategory.NETWORK_EQUIPMENT -> Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8)))
                            else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF475569)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.type == MediaType.VIDEO) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(46.dp))
                } else {
                    when (item.visualAssetType) {
                        "ONT_POWER" -> Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = VividCyan, modifier = Modifier.size(44.dp))
                        "FIBER_TRAY" -> Icon(Icons.Default.Cable, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(44.dp))
                        "ROUTER_LABEL" -> Icon(Icons.Default.Router, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(44.dp))
                        "NOC_RACK" -> Icon(Icons.Default.Dns, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(44.dp))
                        "JOB_SHEET" -> Icon(Icons.Default.Description, contentDescription = null, tint = CoralRose, modifier = Modifier.size(44.dp))
                        else -> Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                    }
                }

                // Type Badge (PHOTO / VIDEO with duration)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.type == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.type == MediaType.VIDEO) "${item.durationSeconds / 60}:${String.format("%02d", item.durationSeconds % 60)}" else "HD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Title & Details
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.uploaderName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.customerName.isNotBlank()) {
                    Text(
                        text = "👤 ${item.customerName}",
                        fontSize = 10.sp,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==============================================================================
// MEDIA LIST ITEM COMPONENT
// ==============================================================================
@Composable
fun MediaCardListItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = item.fileUri) {
        value = if (item.type == MediaType.PHOTO && item.fileUri.isNotBlank()) loadPhotoPreview(item.fileUri, 160) else null
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.type == MediaType.VIDEO) Icons.Default.PlayCircle else Icons.Default.Image,
                        contentDescription = null,
                        tint = VividCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${item.category.name.replace("_", " ")} • ${String.format(Locale.ENGLISH, "%.1f MB", item.fileSizeBytes / (1024 * 1024.0))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "By ${item.uploaderName} • ${SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(item.uploadDateMillis))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

// ==============================================================================
// UPLOAD MEDIA DIALOG
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMediaDialog(
    customers: List<IspCustomer>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onUpload: (
        title: String,
        type: MediaType,
        category: MediaCategory,
        desc: String,
        tags: List<String>,
        custId: String,
        custName: String,
        album: String,
        fileUri: String,
        resolution: String,
        durationSec: Int,
        fileSizeBytes: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val uploadScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf(MediaType.PHOTO) }
    var selectedCategory by remember { mutableStateOf(MediaCategory.CUSTOMER_INSTALLATION) }
    var description by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<IspCustomer?>(null) }
    var albumName by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = getPickedDisplayName(context, uri)
            uploadError = null
            if (title.isBlank()) title = selectedFileName.substringBeforeLast('.', selectedFileName)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upload Media Evidence", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type Selector: Photo vs Video
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mediaType == MediaType.PHOTO,
                                onClick = { mediaType = MediaType.PHOTO; selectedUri = null; selectedFileName = ""; uploadError = null },
                                label = { Text("📷 Photo Upload", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = mediaType == MediaType.VIDEO,
                                onClick = { mediaType = MediaType.VIDEO; selectedUri = null; selectedFileName = ""; uploadError = null },
                                label = { Text("🎥 Video Upload", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Pick from device button
                    item {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        if (mediaType == MediaType.VIDEO) ActivityResultContracts.PickVisualMedia.VideoOnly else ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (mediaType == MediaType.VIDEO) "Select Video File from Device" else "Select Photo / Capture with Camera",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        if (selectedFileName.isNotBlank()) {
                            Text(
                                text = "Selected: $selectedFileName",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        uploadError?.let {
                            Text(it, color = CoralRose, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Media Title / Caption *") },
                            placeholder = { Text("e.g. Sector 4 ONT Optical Power Verification") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Category Dropdown
                    item {
                        Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                MediaCategory.CUSTOMER_INSTALLATION,
                                MediaCategory.FIBER_SPLICING_WORK,
                                MediaCategory.ROUTER_ONT_SETUP,
                                MediaCategory.TRAINING_TUTORIAL
                            ).forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.name.replace("_", " ").take(14), fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Detailed Description / Work Notes") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Tags (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Optional Customer Association
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Link with Customer (Optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    customers.take(3).forEach { c ->
                                        FilterChip(
                                            selected = selectedCustomer?.id == c.id,
                                            onClick = { selectedCustomer = if (selectedCustomer?.id == c.id) null else c },
                                            label = { Text(c.fullName.take(15), fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val sourceUri = selectedUri
                            if (sourceUri == null) {
                                uploadError = "Select a photo or video before saving."
                            } else {
                                uploadScope.launch {
                                    isUploading = true
                                    uploadError = null
                                    try {
                                        val copied = copyMediaToPrivateStorage(context, sourceUri, mediaType)
                                        val tagList = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        onUpload(
                                            title.ifBlank { copied.displayName },
                                            mediaType,
                                            selectedCategory,
                                            description,
                                            tagList,
                                            selectedCustomer?.id ?: "",
                                            selectedCustomer?.fullName ?: "",
                                            albumName.ifBlank { "General Uploads" },
                                            copied.fileUri,
                                            copied.resolution,
                                            copied.durationSeconds,
                                            copied.sizeBytes
                                        )
                                    } catch (error: Exception) {
                                        uploadError = error.message ?: "Unable to save this media file."
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            }
                        },
                        enabled = selectedUri != null && !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isUploading) "Saving…" else "Save to Media Library", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// PHOTO PREVIEW & INSPECTION DIALOG (WITH ZOOM, ROTATE, SHARE)
// ==============================================================================
@Composable
fun PhotoPreviewDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White, maxLines = 1)
                        Text(
                            text = "${item.category.name.replace("_", " ")} • ${item.resolution.ifBlank { "Resolution unavailable" }}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row {
                        IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = VividCyan)
                        }
                        IconButton(onClick = { zoomLevel = if (zoomLevel >= 2.0f) 1.0f else zoomLevel + 0.5f }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = VividCyan)
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val previewBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.fileUri) {
                    value = if (item.fileUri.isBlank()) null else loadPhotoPreview(item.fileUri)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF020617))
                        .rotate(rotationAngle),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomLevel
                                    scaleY = zoomLevel
                                }
                        )
                    } else {
                        Text(
                            text = if (item.fileUri.isBlank()) "No original photo file is attached." else "The original photo could not be opened on this device.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.description, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Uploaded by: ${item.uploaderName} (${item.uploaderRole.name})", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("Size: ${String.format(Locale.ENGLISH, "%.2f MB", item.fileSizeBytes / (1024 * 1024.0))}", color = VividCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (item.customerName.isNotBlank()) {
                            Text("Linked Customer: ${item.customerName} (${item.customerId})", color = GoldenAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// BUILT-IN VIDEO PLAYER DIALOG
// ==============================================================================
@Composable
fun VideoPlayerDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var playbackError by remember { mutableStateOf<String?>(null) }
    val videoViewRef = remember(item.fileUri) { mutableStateOf<VideoView?>(null) }

    DisposableEffect(item.fileUri) {
        onDispose {
            videoViewRef.value?.stopPlayback()
            videoViewRef.value = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0A0F1D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Video Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White, maxLines = 1)
                        Text(
                            text = "Video • ${item.resolution.ifBlank { "Resolution unavailable" }} • ${String.format(Locale.ENGLISH, "%.1f MB", item.fileSizeBytes / (1024 * 1024.0))} • ${if (item.durationSeconds > 0) "${item.durationSeconds}s" else "Duration unavailable"}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real local video playback with Android's media controller.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.fileUri.isBlank()) {
                        Text("The original video file is not attached.", color = Color.White)
                    } else {
                        AndroidView(
                            factory = { viewContext ->
                                VideoView(viewContext).also { videoView ->
                                    videoViewRef.value = videoView
                                    val controller = MediaController(viewContext)
                                    controller.setAnchorView(videoView)
                                    videoView.setMediaController(controller)
                                    videoView.setOnErrorListener { _, _, _ ->
                                        playbackError = "This video could not be played by the device."
                                        true
                                    }
                                    videoView.setOnPreparedListener { player ->
                                        player.isLooping = false
                                        videoView.start()
                                    }
                                    videoView.setVideoURI(Uri.parse(item.fileUri))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                playbackError?.let {
                    Text(it, color = CoralRose, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Description Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.description, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recorded by ${item.uploaderName} • Uploaded on ${SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date(item.uploadDateMillis))}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
