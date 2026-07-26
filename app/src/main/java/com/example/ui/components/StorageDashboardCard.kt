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
import androidx.compose.ui.graphics.Brush
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val storageDetailText = if (totalStorageBytes > 0) {
                            "${formatStorageBytes(usedStorageBytes)} / ${formatStorageBytes(totalStorageBytes)}"
                        } else {
                            "Internal Storage"
                        }
                        Text(
                            text = storageDetailText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Used Storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$usedPercentage%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    if (audioBytes > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(audioWeight)
                                .background(Color(0xFFA855F7)) // Purple
                        )
                    }
                    if (videoBytes > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(videoWeight)
                                .background(Color(0xFFEF4444)) // Red
                        )
                    }
                    if (imageBytes > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(imageWeight)
                                .background(Color(0xFF10B981)) // Green
                        )
                    }
                    if (docBytes > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(docWeight)
                                .background(Color(0xFFF59E0B)) // Amber
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(remainingWeight)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Category Chips Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryChipItem(
                        title = "Audio",
                        countText = "$audioCount files",
                        color = Color(0xFFA855F7), // Purple
                        icon = Icons.Default.MusicNote,
                        isSelected = selectedFilter == FileType.AUDIO,
                        onClick = {
                            onFilterSelected(if (selectedFilter == FileType.AUDIO) null else FileType.AUDIO)
                        }
                    )

                    CategoryChipItem(
                        title = "Video",
                        countText = "$videoCount files",
                        color = Color(0xFFEF4444), // Red
                        icon = Icons.Default.Movie,
                        isSelected = selectedFilter == FileType.VIDEO,
                        onClick = {
                            onFilterSelected(if (selectedFilter == FileType.VIDEO) null else FileType.VIDEO)
                        }
                    )

                    CategoryChipItem(
                        title = "Image",
                        countText = "$imageCount files",
                        color = Color(0xFF10B981), // Green
                        icon = Icons.Default.Image,
                        isSelected = selectedFilter == FileType.IMAGE,
                        onClick = {
                            onFilterSelected(if (selectedFilter == FileType.IMAGE) null else FileType.IMAGE)
                        }
                    )

                    CategoryChipItem(
                        title = "Docs",
                        countText = "$docCount files",
                        color = Color(0xFFF59E0B), // Amber
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) color.copy(alpha = 0.3f) else color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = if (isSelected) color else Color.White
        )

        Text(
            text = countText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}
