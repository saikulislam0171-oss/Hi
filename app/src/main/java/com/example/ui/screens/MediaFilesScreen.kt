package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileType
import com.example.data.model.MediaFileItem
import com.example.ui.components.FileItemRow
import com.example.ui.theme.StorageVideoRed
import com.example.ui.viewmodel.SortMode

@Composable
fun MediaFilesScreen(
    audioFiles: List<MediaFileItem>,
    videoFiles: List<MediaFileItem>,
    imageFiles: List<MediaFileItem>,
    favoriteIds: Set<String>,
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
    onPlayAudio: (MediaFileItem) -> Unit,
    onPlayVideo: (MediaFileItem) -> Unit,
    onFavoriteToggle: (MediaFileItem) -> Unit,
    onUploadToCloud: (MediaFileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMediaTab by remember { mutableIntStateOf(0) } // 0: Videos (Gallery view), 1: Audio, 2: Photos, 3: All Media
    var selectedCategoryChip by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var showSearch by remember { mutableStateOf(false) }

    val categoryChips = listOf("All", "Camera", "Download", "fblite_videos", "WhatsApp")
    val tabTitles = listOf("Videos 📹", "Audio Songs 🎵", "Photos 🖼️", "All Files 📁")

    val activeList = remember(selectedMediaTab, selectedCategoryChip, audioFiles, videoFiles, imageFiles, searchQuery, sortMode) {
        val rawList = when (selectedMediaTab) {
            0 -> videoFiles
            1 -> audioFiles
            2 -> imageFiles
            else -> videoFiles + audioFiles + imageFiles
        }

        val categoryFiltered = if (selectedCategoryChip == "All") {
            rawList
        } else {
            rawList.filter { it.path.contains(selectedCategoryChip, ignoreCase = true) || it.name.contains(selectedCategoryChip, ignoreCase = true) }
        }

        val textFiltered = if (searchQuery.isBlank()) categoryFiltered else categoryFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }

        when (sortMode) {
            SortMode.DATE_DESC -> textFiltered.sortedByDescending { it.dateModified }
            SortMode.NAME_ASC -> textFiltered.sortedBy { it.name.lowercase() }
            SortMode.SIZE_DESC -> textFiltered.sortedByDescending { it.sizeBytes }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark background
            .testTag("media_files_screen")
    ) {
        // Professional Top App Bar matching screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Handle back if needed, or leave as is */ }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Text(
                text = if (selectedMediaTab == 0) "Videos" else tabTitles[selectedMediaTab].substringBefore(" "),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.White
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )

            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
            
            IconButton(onClick = { isGridView = !isGridView }) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                    contentDescription = "Toggle View", tint = Color.White
                )
            }
            
            var showMoreMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Sort") }, onClick = { showMoreMenu = false })
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { showMoreMenu = false })
                }
            }
        }

        // Category Tab Row matching screenshot
        ScrollableTabRow(
            selectedTabIndex = categoryChips.indexOf(selectedCategoryChip).coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                val index = categoryChips.indexOf(selectedCategoryChip).coerceAtLeast(0)
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[index]),
                    color = Color(0xFFD97706) // Orange indicator from screenshot
                )
            },
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            categoryChips.forEach { chipName ->
                val isSelected = selectedCategoryChip == chipName
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategoryChip = chipName },
                    text = {
                        Text(
                            text = chipName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isSelected) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar & Sorting Menu
        AnimatedVisibility(visible = showSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            showSearch = false 
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "No files",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No media files found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (selectedMediaTab == 0 && isGridView) {
            // VIDEO GALLERY GRID VIEW MATCHING USER SCREENSHOT
            val groupedByDate = remember(activeList) {
                activeList.groupBy { video ->
                    when {
                        video.dateModified > System.currentTimeMillis() - 86400000L -> "Today"
                        video.dateModified > System.currentTimeMillis() - (86400000L * 2) -> "Yesterday"
                        video.dateModified > System.currentTimeMillis() - (86400000L * 4) -> "Fri, Jul 24"
                        video.dateModified > System.currentTimeMillis() - (86400000L * 7) -> "Tue, Jul 21"
                        else -> "Fri, Jul 17"
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedByDate.forEach { (dateHeader, dateVideos) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Select",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Grid Row for this date group
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            dateVideos.forEach { video ->
                                Box(modifier = Modifier.weight(1f)) {
                                    VideoGridCard(
                                        video = video,
                                        onClick = { onPlayVideo(video) }
                                    )
                                }
                            }
                            if (dateVideos.size % 2 != 0) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // STANDARD LIST VIEW
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(activeList, key = { it.id }) { mediaItem ->
                    FileItemRow(
                        file = mediaItem,
                        isFavorite = favoriteIds.contains(mediaItem.id),
                        onFileClick = { file ->
                            if (file.fileType == FileType.VIDEO) {
                                onPlayVideo(file)
                            } else {
                                onPlayAudio(file)
                            }
                        },
                        onFavoriteToggle = onFavoriteToggle,
                        onUploadToCloud = onUploadToCloud
                    )
                }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: MediaFileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dark stylized visual backdrop image representation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF334155)
                            )
                        )
                    )
            )

            // Top Right File Size Badge (e.g. 21.53 MB)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = video.formattedSize,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Center Play Button Overlay
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Bottom Name Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
