package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileManagerDao {

    // Favorite Files
    @Query("SELECT * FROM favorite_files ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<FavoriteFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteFileEntity)

    @Query("DELETE FROM favorite_files WHERE id = :fileId")
    suspend fun removeFavorite(fileId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE id = :fileId)")
    suspend fun isFavorite(fileId: String): Boolean

    // Cloud Files
    @Query("SELECT * FROM cloud_files ORDER BY dateUploaded DESC")
    fun getAllCloudFiles(): Flow<List<CloudFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCloudFile(cloudFile: CloudFileEntity)

    @Query("DELETE FROM cloud_files WHERE id = :fileId")
    suspend fun deleteCloudFile(fileId: String)

    // Virtual Folders
    @Query("SELECT * FROM virtual_folders")
    fun getAllVirtualFolders(): Flow<List<VirtualFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualFolder(folder: VirtualFolderEntity)
}
