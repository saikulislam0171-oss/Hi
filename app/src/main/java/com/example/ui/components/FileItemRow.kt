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
            .padding(vertical = 4.dp)
            .testTag("file_item_${file.id}")
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onFileClick(file) },
                onLongClick = { onLongClick?.invoke(file) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            val (badgeColor, iconVector) = when (file.fileType) {
                FileType.AUDIO -> StorageAudioPurple to Icons.Default.MusicNote
                FileType.VIDEO -> StorageVideoRed to Icons.Default.Movie
                FileType.IMAGE -> StorageImageEmerald to Icons.Default.Image
                FileType.DOCUMENT -> StorageDocAmber to Icons.Default.Description
                FileType.FOLDER -> PrimaryBlue to Icons.Default.Folder
                FileType.ARCHIVE -> AccentCyan to Icons.Default.FolderZip
                FileType.OTHER -> Color.Gray to Icons.Default.InsertDriveFile
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = file.fileType.name,
                    tint = badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (file.fileType == FileType.AUDIO && !file.artist.isNullOrEmpty()) {
                        Text(
                            text = " • ${file.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (file.fileType == FileType.VIDEO && !file.formattedDuration.isEmpty()) {
                        Text(
                            text = " • ${file.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (file.source != StorageSource.LOCAL) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StorageCloudSky.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Cloud",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = StorageCloudSky
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = if (file.fileType == FileType.AUDIO) Icons.Default.PlayArrow else Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Cloud Upload Quick Action
            if (onUploadToCloud != null && file.source == StorageSource.LOCAL) {
                IconButton(
                    onClick = { onUploadToCloud(file) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Upload to Cloud",
                        tint = StorageCloudSky,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = { onFavoriteToggle(file) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) StorageDocAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
