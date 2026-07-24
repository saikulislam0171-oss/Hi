package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileType
import com.example.data.model.MediaFileItem
import com.example.ui.components.FileItemRow
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
    var selectedMediaTab by remember { mutableIntStateOf(0) } // 0: All Media, 1: Audio, 2: Video, 3: Photos
    var searchQuery by remember { mutableStateOf("") }

    val tabTitles = listOf("সকল মিডিয়া 🎬", "অডিও গান 🎵", "ভিডিও ক্লিপ 📹", "ছবি 🖼️")

    val activeList = remember(selectedMediaTab, audioFiles, videoFiles, imageFiles, searchQuery, sortMode) {
        val rawList = when (selectedMediaTab) {
            1 -> audioFiles
            2 -> videoFiles
            3 -> imageFiles
            else -> audioFiles + videoFiles + imageFiles
        }

        val filtered = if (searchQuery.isBlank()) rawList else rawList.filter { it.name.contains(searchQuery, ignoreCase = true) }

        when (sortMode) {
            SortMode.DATE_DESC -> filtered.sortedByDescending { it.dateModified }
            SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortMode.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("media_files_screen")
    ) {
        // Banner Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "মোবাইলের সকল অডিও & ভিডিও ফাইল 📱",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "অডিও: ${audioFiles.size} টি • ভিডিও: ${videoFiles.size} টি",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = selectedMediaTab,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedMediaTab == index,
                    onClick = { selectedMediaTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedMediaTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search & Sorting Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ফাইল অনুসন্ধান করুন...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Sort Dropdown
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("সর্বশেষ পরিবর্তিত (Date)") },
                        onClick = {
                            onSortModeChange(SortMode.DATE_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("নাম অনুসারে (A - Z)") },
                        onClick = {
                            onSortModeChange(SortMode.NAME_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ফাইল সাইজ (Size)") },
                        onClick = {
                            onSortModeChange(SortMode.SIZE_DESC)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List
        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো মিডিয়া ফাইল পাওয়া যায়নি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
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
