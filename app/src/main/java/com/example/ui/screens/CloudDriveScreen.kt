package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudAccountInfo
import com.example.data.model.MediaFileItem
import com.example.ui.components.CloudAccountCard
import com.example.ui.components.FileItemRow

@Composable
fun CloudDriveScreen(
    cloudAccounts: List<CloudAccountInfo>,
    cloudFiles: List<MediaFileItem>,
    favoriteIds: Set<String>,
    onToggleConnection: (String) -> Unit,
    onFileClick: (MediaFileItem) -> Unit,
    onFavoriteToggle: (MediaFileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cloud_drive_screen")
    ) {
        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Cloud",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ক্লাউড স্টোরেজ ড্রাইভ ☁️",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "গুগল ড্রাইভ, ড্রপবক্স এবং অনড্রাইভ ফাইল সিঙ্ক সার্ভিস",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "সংযুক্ত ক্লাউড একাউন্টসমূহ:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(cloudAccounts, key = { it.id }) { account ->
                CloudAccountCard(
                    account = account,
                    onToggleConnection = onToggleConnection
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "সিঙ্ক করা ক্লাউড ফাইলসমূহ (${cloudFiles.size}):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (cloudFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "No files",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "এখনো কোনো ক্লাউড ফাইল নেই",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "‘ড্র্যাগ & ড্রপ’ ট্যাব থেকে যেকোনো ফাইল ক্লাউডে আপলোড করুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(cloudFiles, key = { it.id }) { cloudFile ->
                    FileItemRow(
                        file = cloudFile,
                        isFavorite = favoriteIds.contains(cloudFile.id),
                        onFileClick = onFileClick,
                        onFavoriteToggle = onFavoriteToggle
                    )
                }
            }
        }
    }
}
