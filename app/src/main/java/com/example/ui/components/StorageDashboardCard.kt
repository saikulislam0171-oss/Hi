package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileType
import com.example.ui.theme.*

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "0 GB"
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) {
        String.format("%.1f GB", gb)
    } else {
        val mb = bytes.toDouble() / (1024 * 1024)
        String.format("%.1f MB", mb)
    }
}

@Composable
fun StorageDashboardCard(
    audioCount: Int,
    audioBytes: Long,
    videoCount: Int,
    videoBytes: Long,
    imageCount: Int,
    imageBytes: Long,
    docCount: Int,
    docBytes: Long,
    cloudCount: Int,
    totalStorageBytes: Long = 0L,
    usedStorageBytes: Long = 0L,
    usedPercentage: Int = 0,
    selectedFilter: FileType?,
    onFilterSelected: (FileType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = "Storage",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ফোন স্টোরেজ বিবরণ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        val storageDetailText = if (totalStorageBytes > 0) {
                            "Internal Storage (${formatStorageBytes(usedStorageBytes)} / ${formatStorageBytes(totalStorageBytes)})"
                        } else {
                            "Internal Storage"
                        }
                        Text(
                            text = storageDetailText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "$usedPercentage% ব্যবহৃত",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-color Progress Bar
            val safeTotal = if (totalStorageBytes > 0) totalStorageBytes.toFloat() else 1f
            val audioWeight = (audioBytes.toFloat() / safeTotal).coerceIn(0.005f, 0.8f)
            val videoWeight = (videoBytes.toFloat() / safeTotal).coerceIn(0.005f, 0.8f)
            val imageWeight = (imageBytes.toFloat() / safeTotal).coerceIn(0.005f, 0.8f)
            val docWeight = (docBytes.toFloat() / safeTotal).coerceIn(0.005f, 0.8f)
            val remainingWeight = (1f - (audioWeight + videoWeight + imageWeight + docWeight)).coerceAtLeast(0.1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (audioBytes > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(audioWeight)
                            .background(StorageAudioPurple)
                    )
                }
                if (videoBytes > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(videoWeight)
                            .background(StorageVideoRed)
                    )
                }
                if (imageBytes > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(imageWeight)
                            .background(StorageImageEmerald)
                    )
                }
                if (docBytes > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(docWeight)
                            .background(StorageDocAmber)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(remainingWeight)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Category Chips Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryChipItem(
                    title = "অডিও",
                    countText = "$audioCount ফাইল",
                    color = StorageAudioPurple,
                    icon = Icons.Default.MusicNote,
                    isSelected = selectedFilter == FileType.AUDIO,
                    onClick = {
                        onFilterSelected(if (selectedFilter == FileType.AUDIO) null else FileType.AUDIO)
                    }
                )

                CategoryChipItem(
                    title = "ভিডিও",
                    countText = "$videoCount ফাইল",
                    color = StorageVideoRed,
                    icon = Icons.Default.Movie,
                    isSelected = selectedFilter == FileType.VIDEO,
                    onClick = {
                        onFilterSelected(if (selectedFilter == FileType.VIDEO) null else FileType.VIDEO)
                    }
                )

                CategoryChipItem(
                    title = "ছবি",
                    countText = "$imageCount ফাইল",
                    color = StorageImageEmerald,
                    icon = Icons.Default.Image,
                    isSelected = selectedFilter == FileType.IMAGE,
                    onClick = {
                        onFilterSelected(if (selectedFilter == FileType.IMAGE) null else FileType.IMAGE)
                    }
                )

                CategoryChipItem(
                    title = "ডকুমেন্ট",
                    countText = "$docCount ফাইল",
                    color = StorageDocAmber,
                    icon = Icons.Default.Description,
                    isSelected = selectedFilter == FileType.DOCUMENT,
                    onClick = {
                        onFilterSelected(if (selectedFilter == FileType.DOCUMENT) null else FileType.DOCUMENT)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    title: String,
    countText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
