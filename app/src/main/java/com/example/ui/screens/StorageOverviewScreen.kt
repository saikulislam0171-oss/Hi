package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileType
import com.example.data.model.MediaFileItem
import com.example.data.repository.DeviceStorageInfo
import com.example.ui.components.FileItemRow
import com.example.ui.components.StorageDashboardCard
import com.example.ui.theme.PrimaryBlue

@Composable
fun StorageOverviewScreen(
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    audioFiles: List<MediaFileItem>,
    videoFiles: List<MediaFileItem>,
    imageFiles: List<MediaFileItem>,
    docFiles: List<MediaFileItem>,
    cloudFiles: List<MediaFileItem>,
    favoriteIds: Set<String>,
    deviceStorageInfo: DeviceStorageInfo = DeviceStorageInfo(),
    selectedFilter: FileType?,
    searchQuery: String,
    onFilterSelected: (FileType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (MediaFileItem) -> Unit,
    onFavoriteToggle: (MediaFileItem) -> Unit,
    onUploadToCloud: (MediaFileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFiles = remember(audioFiles, videoFiles, imageFiles, docFiles, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            FileType.AUDIO -> audioFiles
            FileType.VIDEO -> videoFiles
            FileType.IMAGE -> imageFiles
            FileType.DOCUMENT -> docFiles
            else -> audioFiles + videoFiles + imageFiles + docFiles
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("storage_overview_screen")
    ) {
        // Storage Permission Request Banner
        AnimatedVisibility(visible = !hasStoragePermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মোবাইল স্টোরেজ পারমিশন প্রয়োজন",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "আপনার ফোনের সকল ভিডিও ও অডিও ফাইল এক্টিভলি স্ক্যান করতে পারমিশন দিন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onRequestStoragePermission,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("অনুমতি দিন", fontSize = 12.sp)
                    }
                }
            }
        }

        // Storage Usage Dashboard
        StorageDashboardCard(
            audioCount = audioFiles.size,
            audioBytes = audioFiles.sumOf { it.sizeBytes },
            videoCount = videoFiles.size,
            videoBytes = videoFiles.sumOf { it.sizeBytes },
            imageCount = imageFiles.size,
            imageBytes = imageFiles.sumOf { it.sizeBytes },
            docCount = docFiles.size,
            docBytes = docFiles.sumOf { it.sizeBytes },
            cloudCount = cloudFiles.size,
            totalStorageBytes = deviceStorageInfo.totalBytes,
            usedStorageBytes = deviceStorageInfo.usedBytes,
            usedPercentage = deviceStorageInfo.usedPercentage,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("ফাইল অনুসন্ধান করুন (যেমন: mp3, video, doc)...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Header for Files List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedFilter) {
                    FileType.AUDIO -> "সকল অডিও ফাইল (${allFiles.size})"
                    FileType.VIDEO -> "সকল ভিডিও ফাইল (${allFiles.size})"
                    FileType.IMAGE -> "সকল ছবি (${allFiles.size})"
                    FileType.DOCUMENT -> "সকল ডকুমেন্ট (${allFiles.size})"
                    else -> "সাম্প্রতিক মিডিয়া ফাইলসমুহ (${allFiles.size})"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (selectedFilter != null) {
                TextButton(onClick = { onFilterSelected(null) }) {
                    Text("সকল ফিল্টার মুছুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Files List
        if (allFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "কোনো ফাইল পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(allFiles, key = { it.id }) { file ->
                    FileItemRow(
                        file = file,
                        isFavorite = favoriteIds.contains(file.id),
                        onFileClick = onFileClick,
                        onFavoriteToggle = onFavoriteToggle,
                        onUploadToCloud = onUploadToCloud
                    )
                }
            }
        }
    }
}
