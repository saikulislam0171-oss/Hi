package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.example.data.local.CloudFileEntity
import com.example.data.local.FavoriteFileEntity
import com.example.data.local.FileManagerDao
import com.example.data.local.VirtualFolderEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class DeviceStorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercentage: Int = 0
)

class FileManagerRepository(
    private val context: Context,
    private val dao: FileManagerDao
) {

    val favoriteFiles: Flow<List<FavoriteFileEntity>> = dao.getAllFavorites()
    val cloudFilesFromDb: Flow<List<CloudFileEntity>> = dao.getAllCloudFiles()
    val virtualFolders: Flow<List<VirtualFolderEntity>> = dao.getAllVirtualFolders()

    fun getDeviceStorageInfo(): DeviceStorageInfo {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = (total - free).coerceAtLeast(0L)
            val percentage = if (total > 0) ((used.toDouble() / total.toDouble()) * 100).toInt() else 0

            DeviceStorageInfo(
                totalBytes = total,
                usedBytes = used,
                freeBytes = free,
                usedPercentage = percentage
            )
        } catch (e: Exception) {
            DeviceStorageInfo()
        }
    }

    suspend fun toggleFavorite(file: MediaFileItem) = withContext(Dispatchers.IO) {
        val isFav = dao.isFavorite(file.id)
        if (isFav) {
            dao.removeFavorite(file.id)
        } else {
            dao.addFavorite(
                FavoriteFileEntity(
                    id = file.id,
                    name = file.name,
                    path = file.path,
                    uriString = file.uriString,
                    sizeBytes = file.sizeBytes,
                    mimeType = file.mimeType,
                    fileTypeString = file.fileType.name
                )
            )
        }
    }

    suspend fun isFavorite(fileId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavorite(fileId)
    }

    suspend fun uploadToCloud(file: MediaFileItem, targetProvider: String): MediaFileItem = withContext(Dispatchers.IO) {
        val cloudEntity = CloudFileEntity(
            id = "cloud_${System.currentTimeMillis()}_${file.id}",
            provider = targetProvider,
            name = file.name,
            sizeBytes = file.sizeBytes,
            mimeType = file.mimeType,
            fileTypeString = file.fileType.name,
            cloudUrl = "https://cloud.storage.app/files/${file.name}",
            syncStatus = "SYNCED"
        )
        dao.insertCloudFile(cloudEntity)

        file.copy(
            cloudSyncStatus = CloudSyncStatus.SYNCED,
            source = when (targetProvider) {
                "Google Drive" -> StorageSource.CLOUD_DRIVE
                "Dropbox" -> StorageSource.CLOUD_DROPBOX
                "OneDrive" -> StorageSource.CLOUD_ONEDRIVE
                else -> StorageSource.CLOUD_CUSTOM
            },
            cloudPath = cloudEntity.cloudUrl
        )
    }

    // Query Device Audio Files via MediaStore
    suspend fun fetchDeviceAudioFiles(): List<MediaFileItem> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<MediaFileItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Audio_$id"
                    val path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "audio/mp3" else "audio/mp3"
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val artist = if (artistColumn != -1) cursor.getString(artistColumn) else "Unknown Artist"
                    val album = if (albumColumn != -1) cursor.getString(albumColumn) else "Unknown Album"

                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    audioList.add(
                        MediaFileItem(
                            id = "audio_$id",
                            name = name,
                            path = path.ifEmpty { "/storage/emulated/0/Music/$name" },
                            uriString = contentUri.toString(),
                            sizeBytes = size,
                            dateModified = date,
                            mimeType = mime,
                            fileType = FileType.AUDIO,
                            durationMs = duration,
                            artist = artist,
                            album = album
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audioList
    }

    // Query Device Video Files via MediaStore
    suspend fun fetchDeviceVideoFiles(): List<MediaFileItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<MediaFileItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION
        )

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "video/mp4" else "video/mp4"
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val res = if (resolutionColumn != -1) cursor.getString(resolutionColumn) else "1080p"

                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videoList.add(
                        MediaFileItem(
                            id = "video_$id",
                            name = name,
                            path = path.ifEmpty { "/storage/emulated/0/Movies/$name" },
                            uriString = contentUri.toString(),
                            sizeBytes = size,
                            dateModified = date,
                            mimeType = mime,
                            fileType = FileType.VIDEO,
                            durationMs = duration,
                            resolution = res
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    // Fetch Images
    suspend fun fetchDeviceImageFiles(): List<MediaFileItem> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<MediaFileItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image_$id"
                    val path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "image/jpeg" else "image/jpeg"

                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    imageList.add(
                        MediaFileItem(
                            id = "img_$id",
                            name = name,
                            path = path.ifEmpty { "/storage/emulated/0/DCIM/$name" },
                            uriString = contentUri.toString(),
                            sizeBytes = size,
                            dateModified = date,
                            mimeType = mime,
                            fileType = FileType.IMAGE
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageList
    }

    // Fetch Documents & Archives from MediaStore.Files
    suspend fun fetchDeviceDocuments(): List<MediaFileItem> = withContext(Dispatchers.IO) {
        val docList = mutableListOf<MediaFileItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"

        val selectionArgs = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument%",
            "text/%",
            "application/zip",
            "application/rar"
        )

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Doc_$id"
                    val path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000
                    val mime = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "application/pdf" else "application/pdf"

                    val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                    docList.add(
                        MediaFileItem(
                            id = "doc_$id",
                            name = name,
                            path = path.ifEmpty { "/storage/emulated/0/Documents/$name" },
                            uriString = contentUri.toString(),
                            sizeBytes = size,
                            dateModified = date,
                            mimeType = mime,
                            fileType = if (mime.contains("zip") || mime.contains("rar") || mime.contains("tar")) FileType.ARCHIVE else FileType.DOCUMENT
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        docList
    }
}
