package com.example.data.model

import android.net.Uri

enum class FileType {
    AUDIO,
    VIDEO,
    IMAGE,
    DOCUMENT,
    FOLDER,
    ARCHIVE,
    OTHER
}

enum class StorageSource {
    LOCAL,
    CLOUD_DRIVE,
    CLOUD_DROPBOX,
    CLOUD_ONEDRIVE,
    CLOUD_CUSTOM
}

data class MediaFileItem(
    val id: String,
    val name: String,
    val path: String,
    val uriString: String,
    val sizeBytes: Long,
    val dateModified: Long,
    val mimeType: String,
    val fileType: FileType,
    val isFavorite: Boolean = false,
    val source: StorageSource = StorageSource.LOCAL,
    val durationMs: Long = 0L,
    val artist: String? = null,
    val album: String? = null,
    val resolution: String? = null,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.NONE,
    val cloudPath: String? = null
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
            sizeBytes >= 1024 * 1024 -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = durationMs / (1000 * 60 * 60)
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}

enum class CloudSyncStatus {
    NONE,
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED
}

data class CloudAccountInfo(
    val id: String,
    val providerName: String,
    val email: String,
    val totalStorageGb: Double,
    val usedStorageGb: Double,
    val isConnected: Boolean,
    val accountBadgeColor: Long
)

data class StorageCategoryInfo(
    val fileType: FileType,
    val title: String,
    val titleBn: String,
    val count: Int,
    val sizeBytes: Long,
    val colorHex: Long
)

data class DragDropTargetFolder(
    val id: String,
    val name: String,
    val nameBn: String,
    val iconType: String,
    val targetType: StorageSource
)
