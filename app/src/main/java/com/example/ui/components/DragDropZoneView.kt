package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DragDropTargetFolder
import com.example.data.model.MediaFileItem
import com.example.data.model.StorageSource
import com.example.ui.theme.*

@Composable
fun DragDropOrganizerView(
    availableFiles: List<MediaFileItem>,
    draggedItem: MediaFileItem?,
    onStartDrag: (MediaFileItem) -> Unit,
    onCancelDrag: () -> Unit,
    onDropToTarget: (MediaFileItem, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTargetFolder by remember { mutableStateOf<DragDropTargetFolder?>(null) }

    val targetFolders = listOf(
        DragDropTargetFolder("target_gdrive", "Google Drive", "Google Drive (Cloud)", "cloud", StorageSource.CLOUD_DRIVE),
        DragDropTargetFolder("target_dropbox", "Dropbox Drive", "Dropbox (Cloud)", "cloud", StorageSource.CLOUD_DROPBOX),
        DragDropTargetFolder("target_fav", "Favorites Folder", "Favorites Folder", "star", StorageSource.LOCAL),
        DragDropTargetFolder("target_vault", "Media Vault", "Media Vault Folder", "folder", StorageSource.LOCAL)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("drag_drop_organizer")
    ) {
        // Banner Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SecondaryTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DriveFileMove,
                        contentDescription = "Drag Drop",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Drag & Drop File Organizer 🚀",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select any file and drop it into cloud storage or favorite folders easily",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Drag Preview Banner
        AnimatedVisibility(visible = draggedItem != null) {
            draggedItem?.let { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Dragging",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Dragged File: ${file.name}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Tap any folder below to confirm drop",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }

                        TextButton(onClick = onCancelDrag) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }

        Text(
            text = "1. Select Target Drop Zone:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Drop Target Grid Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            targetFolders.forEach { target ->
                val isSelected = selectedTargetFolder?.id == target.id
                val scale by animateFloatAsState(if (isSelected) 1.05f else 1.0f, label = "scale")

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedTargetFolder = target
                            if (draggedItem != null) {
                                val provider = if (target.targetType != StorageSource.LOCAL) target.name else null
                                onDropToTarget(draggedItem, target.name, provider)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (target.iconType) {
                                    "cloud" -> Icons.Outlined.CloudUpload
                                    "star" -> Icons.Outlined.Star
                                    else -> Icons.Outlined.FolderZip
                                },
                                contentDescription = target.name,
                                tint = if (isSelected) Color.White else PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = target.nameBn,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "2. Select File to Drag & Drop:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(availableFiles, key = { it.id }) { file ->
                val isBeingDragged = draggedItem?.id == file.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (draggedItem == null) {
                                onStartDrag(file)
                            } else if (selectedTargetFolder != null) {
                                val target = selectedTargetFolder!!
                                val provider = if (target.targetType != StorageSource.LOCAL) target.name else null
                                onDropToTarget(file, target.name, provider)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBeingDragged) StorageCloudSky.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag Handle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Text(
                                text = "${file.formattedSize} • ${file.fileType.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedTargetFolder != null) {
                                    val target = selectedTargetFolder!!
                                    val provider = if (target.targetType != StorageSource.LOCAL) target.name else null
                                    onDropToTarget(file, target.name, provider)
                                } else {
                                    onStartDrag(file)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (selectedTargetFolder != null) "Drop 📥" else "Drag 👆",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
