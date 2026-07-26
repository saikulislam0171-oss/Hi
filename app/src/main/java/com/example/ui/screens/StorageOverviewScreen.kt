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
            .background(Color(0xFF0F172A))
            .testTag("storage_overview_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Storage",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                )
                Text(
                    text = "Internal & SD Card",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            
            IconButton(
                onClick = { /* Settings */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Storage Permission Request Banner
                AnimatedVisibility(visible = !hasStoragePermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permission",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Grant storage access to scan files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Button(
                                onClick = onRequestStoragePermission,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Grant", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            item {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search your files...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
            }

            item {
                // Header for Files List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedFilter) {
                            FileType.AUDIO -> "Audio Files"
                            FileType.VIDEO -> "Videos"
                            FileType.IMAGE -> "Images"
                            FileType.DOCUMENT -> "Documents"
                            else -> "Recent Files"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )

                    if (selectedFilter != null) {
                        TextButton(onClick = { onFilterSelected(null) }) {
                            Text("Clear", color = Color(0xFF38BDF8))
                        }
                    } else {
                        Text(
                            text = "${allFiles.size} items",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Files List
            if (allFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Empty",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No files found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            } else {
                items(allFiles, key = { it.id }) { file ->
                    FileItemRow(
                        file = file,
                        isFavorite = favoriteIds.contains(file.id),
                        onFileClick = onFileClick,
                        onFavoriteToggle = onFavoriteToggle,
                        onUploadToCloud = onUploadToCloud,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}
