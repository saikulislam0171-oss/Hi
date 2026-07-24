package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.DeviceStorageInfo
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class NavigationTab {
    STORAGE_OVERVIEW,
    AUDIO_VIDEO,
    DRAG_DROP_ORGANIZER,
    CLOUD_DRIVE,
    FAVORITES,
    YOUTUBE
}

enum class SortMode {
    DATE_DESC,
    NAME_ASC,
    SIZE_DESC
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileManagerRepository
    
    // UI Tab State
    private val _selectedTab = MutableStateFlow(NavigationTab.STORAGE_OVERVIEW)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    // Storage Info State
    private val _deviceStorageInfo = MutableStateFlow(DeviceStorageInfo())
    val deviceStorageInfo: StateFlow<DeviceStorageInfo> = _deviceStorageInfo.asStateFlow()

    // Permission State
    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // Files State
    private val _allAudioFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val allAudioFiles: StateFlow<List<MediaFileItem>> = _allAudioFiles.asStateFlow()

    private val _allVideoFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val allVideoFiles: StateFlow<List<MediaFileItem>> = _allVideoFiles.asStateFlow()

    private val _allImageFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val allImageFiles: StateFlow<List<MediaFileItem>> = _allImageFiles.asStateFlow()

    private val _allDocuments = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val allDocuments: StateFlow<List<MediaFileItem>> = _allDocuments.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val cloudFiles: StateFlow<List<MediaFileItem>> = _cloudFiles.asStateFlow()

    private val _favoriteFileIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteFileIds: StateFlow<Set<String>> = _favoriteFileIds.asStateFlow()

    // Active Category Filter for Media List
    private val _selectedFileTypeFilter = MutableStateFlow<FileType?>(null)
    val selectedFileTypeFilter: StateFlow<FileType?> = _selectedFileTypeFilter.asStateFlow()

    // Search and Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.DATE_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Audio Player State
    private val _activeAudioItem = MutableStateFlow<MediaFileItem?>(null)
    val activeAudioItem: StateFlow<MediaFileItem?> = _activeAudioItem.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _audioPlaybackProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val audioPlaybackProgress: StateFlow<Float> = _audioPlaybackProgress.asStateFlow()

    // Video Player Dialog State
    private val _activeVideoItem = MutableStateFlow<MediaFileItem?>(null)
    val activeVideoItem: StateFlow<MediaFileItem?> = _activeVideoItem.asStateFlow()

    // Cloud Accounts State
    private val _cloudAccounts = MutableStateFlow<List<CloudAccountInfo>>(
        listOf(
            CloudAccountInfo("gdrive", "Google Drive", "saikulislam0171@gmail.com", 15.0, 6.4, true, 0xFF4285F4),
            CloudAccountInfo("dropbox", "Dropbox", "saikulislam.work@dropbox.com", 5.0, 1.2, true, 0xFF0061FE),
            CloudAccountInfo("onedrive", "Microsoft OneDrive", "saikul.cloud@outlook.com", 10.0, 3.8, false, 0xFF0078D4),
            CloudAccountInfo("custom_cloud", "Personal Cloud Drive", "nas.home.local:8080", 100.0, 42.5, true, 0xFF0EA5E9)
        )
    )
    val cloudAccounts: StateFlow<List<CloudAccountInfo>> = _cloudAccounts.asStateFlow()

    // Drag and Drop & User Notice
    private val _draggedItem = MutableStateFlow<MediaFileItem?>(null)
    val draggedItem: StateFlow<MediaFileItem?> = _draggedItem.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FileManagerRepository(application, database.fileManagerDao())

        // Observe Room Database Favorites
        viewModelScope.launch {
            repository.favoriteFiles.collectLatest { favorites ->
                _favoriteFileIds.value = favorites.map { it.id }.toSet()
            }
        }

        // Observe Room Database Cloud Files
        viewModelScope.launch {
            repository.cloudFilesFromDb.collectLatest { cloudEntities ->
                _cloudFiles.value = cloudEntities.map { entity ->
                    MediaFileItem(
                        id = entity.id,
                        name = entity.name,
                        path = entity.cloudUrl,
                        uriString = entity.cloudUrl,
                        sizeBytes = entity.sizeBytes,
                        dateModified = entity.dateUploaded,
                        mimeType = entity.mimeType,
                        fileType = try { FileType.valueOf(entity.fileTypeString) } catch (e: Exception) { FileType.OTHER },
                        source = when (entity.provider) {
                            "Google Drive" -> StorageSource.CLOUD_DRIVE
                            "Dropbox" -> StorageSource.CLOUD_DROPBOX
                            "OneDrive" -> StorageSource.CLOUD_ONEDRIVE
                            else -> StorageSource.CLOUD_CUSTOM
                        },
                        cloudSyncStatus = CloudSyncStatus.SYNCED,
                        cloudPath = entity.cloudUrl
                    )
                }
            }
        }

        refreshMediaFiles()
    }

    fun setStoragePermissionGranted(granted: Boolean) {
        _hasStoragePermission.value = granted
        refreshMediaFiles()
    }

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
    }

    fun setFileTypeFilter(fileType: FileType?) {
        _selectedFileTypeFilter.value = fileType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun refreshMediaFiles() {
        viewModelScope.launch {
            _deviceStorageInfo.value = repository.getDeviceStorageInfo()
            val audio = repository.fetchDeviceAudioFiles()
            val video = repository.fetchDeviceVideoFiles()
            val images = repository.fetchDeviceImageFiles()
            val docs = repository.fetchDeviceDocuments()

            _allAudioFiles.value = audio
            _allVideoFiles.value = video
            _allImageFiles.value = images
            _allDocuments.value = docs
        }
    }

    fun toggleFavorite(file: MediaFileItem) {
        viewModelScope.launch {
            repository.toggleFavorite(file)
            showStatusMessage("পছন্দসই তালিকা আপডেট করা হয়েছে: ${file.name}")
        }
    }

    // Audio Player Controls
    fun playAudio(item: MediaFileItem) {
        if (_activeAudioItem.value?.id == item.id) {
            _isAudioPlaying.value = !_isAudioPlaying.value
        } else {
            _activeAudioItem.value = item
            _isAudioPlaying.value = true
            _audioPlaybackProgress.value = 0.1f
        }
    }

    fun pauseAudio() {
        _isAudioPlaying.value = false
    }

    fun toggleAudioPlayPause() {
        _isAudioPlaying.value = !_isAudioPlaying.value
    }

    fun closeAudioPlayer() {
        _activeAudioItem.value = null
        _isAudioPlaying.value = false
    }

    // Video Player Dialog
    fun playVideo(item: MediaFileItem) {
        _activeVideoItem.value = item
    }

    fun closeVideoPlayer() {
        _activeVideoItem.value = null
    }

    // Cloud Connection Toggles
    fun toggleCloudAccountConnection(accountId: String) {
        _cloudAccounts.value = _cloudAccounts.value.map { account ->
            if (account.id == accountId) {
                val updatedState = !account.isConnected
                val msg = if (updatedState) "${account.providerName} কানেক্ট করা হয়েছে" else "${account.providerName} ডিসকানেক্ট করা হয়েছে"
                showStatusMessage(msg)
                account.copy(isConnected = updatedState)
            } else account
        }
    }

    // Drag and Drop Handling
    fun startDrag(file: MediaFileItem) {
        _draggedItem.value = file
    }

    fun endDrag() {
        _draggedItem.value = null
    }

    fun dropItemToTarget(file: MediaFileItem, targetFolderName: String, targetProvider: String?) {
        viewModelScope.launch {
            if (targetProvider != null) {
                repository.uploadToCloud(file, targetProvider)
                showStatusMessage("‘${file.name}’ ক্লাউড স্টোরেজে (${targetProvider}) ড্র্যাগ & ড্রপ আপলোড সম্পন্ন হয়েছে! ☁️")
            } else {
                showStatusMessage("‘${file.name}’ ফাইলটি ‘${targetFolderName}’ ফোল্ডারে সরানো হয়েছে! 📁")
            }
            _draggedItem.value = null
        }
    }

    fun showStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
