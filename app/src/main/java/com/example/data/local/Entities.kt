package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_files")
data class FavoriteFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val uriString: String,
    val sizeBytes: Long,
    val mimeType: String,
    val fileTypeString: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "cloud_files")
data class CloudFileEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val fileTypeString: String,
    val cloudUrl: String,
    val dateUploaded: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "virtual_folders")
data class VirtualFolderEntity(
    @PrimaryKey val id: String,
    val folderName: String,
    val folderNameBn: String,
    val colorHex: Long,
    val iconName: String,
    val isCloudFolder: Boolean = false
)
