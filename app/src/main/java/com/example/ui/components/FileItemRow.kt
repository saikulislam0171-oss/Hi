package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileType
import com.example.data.model.MediaFileItem
import com.example.data.model.StorageSource
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: MediaFileItem,
    isFavorite: Boolean,
    onFileClick: (MediaFileItem) -> Unit,
    onFavoriteToggle: (MediaFileItem) -> Unit,
    onLongClick: ((MediaFileItem) -> Unit)? = null,
    onUploadToCloud: ((MediaFileItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("file_item_${file.id}")
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onFileClick(file) },
                onLongClick = { onLongClick?.invoke(file) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B) // Slate 800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            val (badgeColor, iconVector) = when (file.fileType) {
                FileType.AUDIO -> Color(0xFFA855F7) to Icons.Default.MusicNote
                FileType.VIDEO -> Color(0xFFEF4444) to Icons.Default.Movie
                FileType.IMAGE -> Color(0xFF10B981) to Icons.Default.Image
                FileType.DOCUMENT -> Color(0xFFF59E0B) to Icons.Default.Description
                FileType.FOLDER -> Color(0xFF38BDF8) to Icons.Default.Folder
                FileType.ARCHIVE -> Color(0xFF06B6D4) to Icons.Default.FolderZip
                FileType.OTHER -> Color.Gray to Icons.Default.InsertDriveFile
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = file.fileType.name,
                    tint = badgeColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    if (file.fileType == FileType.AUDIO && !file.artist.isNullOrEmpty()) {
                        Text(
                            text = " • ${file.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (file.fileType == FileType.VIDEO && !file.formattedDuration.isEmpty()) {
                        Text(
                            text = " • ${file.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    if (file.source != StorageSource.LOCAL) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cloud",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // Quick Play/Action for Audio or Video
            if (file.fileType == FileType.AUDIO || file.fileType == FileType.VIDEO) {
                IconButton(
                    onClick = { onFileClick(file) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (file.fileType == FileType.AUDIO) Icons.Default.PlayArrow else Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Cloud Upload Quick Action
            if (onUploadToCloud != null && file.source == StorageSource.LOCAL) {
                IconButton(
                    onClick = { onUploadToCloud(file) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Upload to Cloud",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = { onFavoriteToggle(file) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
