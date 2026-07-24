package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FileType
import com.example.data.model.MediaFileItem
import com.example.ui.components.AudioPlayerBar
import com.example.ui.components.DragDropOrganizerView
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.FileManagerViewModel
import com.example.ui.viewmodel.NavigationTab
import java.io.File

fun returnSelectedFileToCallingApp(activity: Activity?, file: MediaFileItem) {
    if (activity == null) return
    var uri: Uri? = null
    try {
        if (file.uriString.isNotEmpty() && file.uriString.startsWith("content://")) {
            uri = Uri.parse(file.uriString)
        } else if (file.path.isNotEmpty()) {
            val fileObj = File(file.path)
            if (fileObj.exists()) {
                uri = try {
                    FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        fileObj
                    )
                } catch (e: Exception) {
                    Uri.fromFile(fileObj)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (uri != null) {
        val resultIntent = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    } else {
        activity.setResult(Activity.RESULT_CANCELED)
        activity.finish()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check storage permission on startup
        checkInitialPermissions()

        setContent {
            MyApplicationTheme {
                MainFileManagerApp(viewModel = viewModel)
            }
        }
    }

    private fun checkInitialPermissions() {
        val context = this
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setStoragePermissionGranted(isGranted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFileManagerApp(viewModel: FileManagerViewModel) {
    val context = LocalContext.current

    val activity = context as? Activity
    var isPickerModeActive by remember {
        mutableStateOf(
            activity?.intent?.action in listOf(
                Intent.ACTION_GET_CONTENT,
                Intent.ACTION_OPEN_DOCUMENT,
                Intent.ACTION_PICK
            )
        )
    }

    val handleFileClick: (MediaFileItem) -> Unit = { file ->
        if (isPickerModeActive) {
            returnSelectedFileToCallingApp(activity, file)
        } else {
            if (file.fileType == FileType.VIDEO) {
                viewModel.playVideo(file)
            } else if (file.fileType == FileType.AUDIO) {
                viewModel.playAudio(file)
            } else {
                viewModel.showStatusMessage("ফাইল অ্যাক্সেস করা হয়েছে: ${file.name}")
            }
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val deviceStorageInfo by viewModel.deviceStorageInfo.collectAsStateWithLifecycle()

    val audioFiles by viewModel.allAudioFiles.collectAsStateWithLifecycle()
    val videoFiles by viewModel.allVideoFiles.collectAsStateWithLifecycle()
    val imageFiles by viewModel.allImageFiles.collectAsStateWithLifecycle()
    val docFiles by viewModel.allDocuments.collectAsStateWithLifecycle()
    val cloudFiles by viewModel.cloudFiles.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteFileIds.collectAsStateWithLifecycle()
    val cloudAccounts by viewModel.cloudAccounts.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFileTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    val activeAudioItem by viewModel.activeAudioItem.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val audioProgress by viewModel.audioPlaybackProgress.collectAsStateWithLifecycle()

    val activeVideoItem by viewModel.activeVideoItem.collectAsStateWithLifecycle()
    val draggedItem by viewModel.draggedItem.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.any { it }
        viewModel.setStoragePermissionGranted(granted)
        if (granted) {
            viewModel.showStatusMessage("স্টোরেজ পারমিশন অনুমোদিত হয়েছে! ফাইল স্ক্যান করা হচ্ছে... 📂")
        }
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val requestPermissions = {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    val allMediaCombined = remember(audioFiles, videoFiles, imageFiles, docFiles) {
        audioFiles + videoFiles + imageFiles + docFiles
    }

    val favoriteFilesList = remember(allMediaCombined, cloudFiles, favoriteIds) {
        (allMediaCombined + cloudFiles).filter { favoriteIds.contains(it.id) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedTab != NavigationTab.YOUTUBE && selectedTab != NavigationTab.HD_MOVIES) {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderCopy,
                                        contentDescription = "App Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ফাইল ম্যানেজার",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Text(
                                        text = "ড্র্যাগ-ড্রপ & ক্লাউড ড্রাইভার",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshMediaFiles() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (isPickerModeActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ফাইল পাঠান 📤 (নির্বাচন করুন)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            activity?.setResult(Activity.RESULT_CANCELED)
                                            activity?.finish()
                                        }
                                    ) {
                                        Text("বাতিল", fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            isPickerModeActive = false
                                            viewModel.showStatusMessage("সাধারণ ফাইল ম্যানেজার মোড চালু হয়েছে")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss Picker Mode",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab != NavigationTab.YOUTUBE && selectedTab != NavigationTab.HD_MOVIES) {
                Column(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Persistent Audio Player Bar overlay
                    AudioPlayerBar(
                        audioItem = activeAudioItem,
                        isPlaying = isAudioPlaying,
                        progress = audioProgress,
                        onPlayPauseToggle = { viewModel.toggleAudioPlayPause() },
                        onClosePlayer = { viewModel.closeAudioPlayer() }
                    )

                    // Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.STORAGE_OVERVIEW,
                            onClick = { viewModel.selectTab(NavigationTab.STORAGE_OVERVIEW) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.STORAGE_OVERVIEW) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Overview"
                                )
                            },
                            label = { Text("স্টোরেজ", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_overview")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.AUDIO_VIDEO,
                            onClick = { viewModel.selectTab(NavigationTab.AUDIO_VIDEO) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.AUDIO_VIDEO) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                    contentDescription = "Audio Video"
                                )
                            },
                            label = { Text("ভিডিও-অডিও", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_media")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.HD_MOVIES,
                            onClick = { viewModel.selectTab(NavigationTab.HD_MOVIES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.HD_MOVIES) Icons.Filled.Movie else Icons.Outlined.Movie,
                                    contentDescription = "HD Movies",
                                    tint = if (selectedTab == NavigationTab.HD_MOVIES) Color(0xFFE50914) else LocalContentColor.current
                                )
                            },
                            label = { Text("এইচডি মুভিজ", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_hd_movies")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.CLOUD_DRIVE,
                            onClick = { viewModel.selectTab(NavigationTab.CLOUD_DRIVE) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.CLOUD_DRIVE) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                                    contentDescription = "Cloud"
                                )
                            },
                            label = { Text("ক্লাউড", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_cloud")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.FAVORITES,
                            onClick = { viewModel.selectTab(NavigationTab.FAVORITES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorites"
                                )
                            },
                            label = { Text("পছন্দসই", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_favorites")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.YOUTUBE,
                            onClick = { viewModel.selectTab(NavigationTab.YOUTUBE) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.YOUTUBE) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                                    contentDescription = "YouTube",
                                    tint = if (selectedTab == NavigationTab.YOUTUBE) Color(0xFFFF0000) else LocalContentColor.current
                                )
                            },
                            label = { Text("ইউটিউব", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_youtube")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.STORAGE_OVERVIEW -> {
                    StorageOverviewScreen(
                        hasStoragePermission = hasPermission,
                        onRequestStoragePermission = { requestPermissions() },
                        audioFiles = audioFiles,
                        videoFiles = videoFiles,
                        imageFiles = imageFiles,
                        docFiles = docFiles,
                        cloudFiles = cloudFiles,
                        favoriteIds = favoriteIds,
                        deviceStorageInfo = deviceStorageInfo,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onFilterSelected = { viewModel.setFileTypeFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onFileClick = { file -> handleFileClick(file) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onUploadToCloud = { file ->
                            viewModel.dropItemToTarget(file, "Google Drive", "Google Drive")
                        }
                    )
                }

                NavigationTab.AUDIO_VIDEO -> {
                    MediaFilesScreen(
                        audioFiles = audioFiles,
                        videoFiles = videoFiles,
                        imageFiles = imageFiles,
                        favoriteIds = favoriteIds,
                        sortMode = sortMode,
                        onSortModeChange = { viewModel.setSortMode(it) },
                        onPlayAudio = { handleFileClick(it) },
                        onPlayVideo = { handleFileClick(it) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onUploadToCloud = { file ->
                            viewModel.dropItemToTarget(file, "Google Drive", "Google Drive")
                        }
                    )
                }

                NavigationTab.HD_MOVIES -> {
                    HdMoviesScreen(
                        onExit = { viewModel.selectTab(NavigationTab.STORAGE_OVERVIEW) }
                    )
                }

                NavigationTab.CLOUD_DRIVE -> {
                    CloudDriveScreen(
                        cloudAccounts = cloudAccounts,
                        cloudFiles = cloudFiles,
                        favoriteIds = favoriteIds,
                        onToggleConnection = { viewModel.toggleCloudAccountConnection(it) },
                        onFileClick = { file -> handleFileClick(file) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                    )
                }

                NavigationTab.FAVORITES -> {
                    FavoritesScreen(
                        favoriteFiles = favoriteFilesList,
                        favoriteIds = favoriteIds,
                        onFileClick = { file -> handleFileClick(file) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                    )
                }

                NavigationTab.YOUTUBE -> {
                    YouTubeScreen(
                        onExit = { viewModel.selectTab(NavigationTab.STORAGE_OVERVIEW) }
                    )
                }
            }

            // Video Player Popup Modal
            if (activeVideoItem != null) {
                VideoPlayerModal(
                    videoItem = activeVideoItem,
                    onDismiss = { viewModel.closeVideoPlayer() }
                )
            }
        }
    }
}
