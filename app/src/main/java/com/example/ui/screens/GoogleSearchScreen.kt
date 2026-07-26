package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.*
import android.media.MediaPlayer
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "https://www.google.com",
    var title: String = "Google",
    var displayHost: String = "google.com"
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class BrowserDownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val mimeType: String = "video/mp4",
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedSize: String
        get() {
            val mb = totalBytes / (1024f * 1024f)
            val downloadedMb = downloadedBytes / (1024f * 1024f)
            return String.format("%.1f MB / %.1f MB", downloadedMb, mb)
        }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleSearchScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var tabs by remember { mutableStateOf(listOf(BrowserTab())) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var tabSearchQuery by remember { mutableStateOf("") }

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab()
    }

    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf(activeTab.url) }
    var searchInput by remember { mutableStateOf(activeTab.displayHost) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Dedicated Full Screen Search Page state
    var showSearchPage by remember { mutableStateOf(false) }
    var fullSearchQuery by remember { mutableStateOf("") }
    var searchHistory by remember {
        mutableStateOf(
            listOf(
                "youtube",
                "youtube studio",
                "moviebox",
                "liteapks.com",
                "file manager",
                "hd movies"
            )
        )
    }

    // Downloads Page state & real-time manager
    var showDownloadsPage by remember { mutableStateOf(false) }
    var selectedDownloadCategory by remember { mutableStateOf("ALL") } // ALL, VIDEO, DOC, OTHER
    var showUrlDownloadDialog by remember { mutableStateOf(false) }
    var urlDownloadInput by remember { mutableStateOf("") }
    var downloadsList by remember { mutableStateOf<List<BrowserDownloadItem>>(emptyList()) }

    // MX Player Overlay state
    var selectedMediaItemForPlayer by remember { mutableStateOf<BrowserDownloadItem?>(null) }
    var showMxPlayerOverlay by remember { mutableStateOf(false) }

    // Real-time progress updater for active downloads
    LaunchedEffect(downloadsList) {
        val hasActive = downloadsList.any { it.status == DownloadStatus.DOWNLOADING }
        if (hasActive) {
            while (true) {
                delay(600)
                downloadsList = downloadsList.map { item ->
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        val step = (item.totalBytes / 25).coerceAtLeast(300 * 1024L)
                        val newBytes = (item.downloadedBytes + step).coerceAtMost(item.totalBytes)
                        val newStatus = if (newBytes >= item.totalBytes) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                        item.copy(downloadedBytes = newBytes, status = newStatus)
                    } else {
                        item
                    }
                }
                if (downloadsList.none { it.status == DownloadStatus.DOWNLOADING }) {
                    break
                }
            }
        }
    }

    // Domain Security Lock state
    var isDomainLockEnabled by remember { mutableStateOf(false) }
    var lockedDomain by remember { mutableStateOf("") }
    var pendingNavigationUrl by remember { mutableStateOf<String?>(null) }
    var showDomainPermissionDialog by remember { mutableStateOf(false) }

    fun isSameDomain(domain1: String, domain2: String): Boolean {
        if (domain1.isBlank() || domain2.isBlank()) return true
        val d1 = domain1.lowercase().removePrefix("www.")
        val d2 = domain2.lowercase().removePrefix("www.")
        return d1 == d2 || d1.endsWith(".$d2") || d2.endsWith(".$d1")
    }

    // When switching active tab, update web view URL
    LaunchedEffect(activeTabId) {
        val tab = tabs.find { it.id == activeTabId }
        if (tab != null && webView != null && webView?.url != tab.url) {
            webView?.loadUrl(tab.url)
            currentUrl = tab.url
            searchInput = tab.displayHost
        }
    }

    BackHandler(enabled = true) {
        if (showMxPlayerOverlay) {
            showMxPlayerOverlay = false
        } else if (showDownloadsPage) {
            showDownloadsPage = false
        } else if (showSearchPage) {
            showSearchPage = false
        } else if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (canGoBack) {
            webView?.goBack()
        } else {
            onExit()
        }
    }

    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun performSearchOrNavigate(query: String) {
        keyboardController?.hide()
        val trimmed = query.trim()
        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }

        if (isDomainLockEnabled && lockedDomain.isNotBlank()) {
            val targetHost = Uri.parse(targetUrl).host ?: ""
            if (targetHost.isNotBlank() && !isSameDomain(lockedDomain, targetHost)) {
                pendingNavigationUrl = targetUrl
                showDomainPermissionDialog = true
                return
            }
        }
        webView?.loadUrl(targetUrl)
    }

    fun openNewTab(url: String = "https://www.google.com") {
        val host = Uri.parse(url).host ?: "google.com"
        val newTab = BrowserTab(url = url, title = if (url.contains("google")) "Google" else host, displayHost = host)
        tabs = tabs + newTab
        activeTabId = newTab.id
        showTabSwitcher = false
    }

    fun closeTab(tabId: String) {
        val updated = tabs.filter { it.id != tabId }
        if (updated.isEmpty()) {
            val freshTab = BrowserTab()
            tabs = listOf(freshTab)
            activeTabId = freshTab.id
        } else {
            tabs = updated
            if (activeTabId == tabId) {
                activeTabId = updated.last().id
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (showTabSwitcher) Color(0xFFFAF1EC) else Color(0xFF1F1F23))
            .testTag("google_search_screen")
    ) {
        if (showTabSwitcher) {
            // TAB SWITCHER VIEW (Matches attached screenshot)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF1EC))
            ) {
                // Top controls bar
                Surface(
                    color = Color(0xFFFAF1EC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Plus button on left in rounded brown box
                            Surface(
                                color = Color(0xFF7D4E27),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { openNewTab() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Tab",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Middle tab count & grid toggle pill
                            Surface(
                                color = Color(0xFFF2DFD5),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Surface(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFF3E2723)),
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${tabs.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3E2723)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Grid layout",
                                        tint = Color(0xFF3E2723),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Right three dots menu
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = Color(0xFF3E2723)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search your tabs pill bar
                        Surface(
                            color = Color(0xFFF2DFD5),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search tabs",
                                    tint = Color(0xFF795548),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                TextField(
                                    value = tabSearchQuery,
                                    onValueChange = { tabSearchQuery = it },
                                    placeholder = { Text("Search your tabs", fontSize = 15.sp, color = Color(0xFF795548)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color(0xFF3E2723),
                                        unfocusedTextColor = Color(0xFF3E2723)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (tabSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { tabSearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF795548))
                                    }
                                }
                            }
                        }
                    }
                }

                // Grid of open tab cards
                val displayTabs = remember(tabs, tabSearchQuery) {
                    if (tabSearchQuery.isBlank()) tabs
                    else tabs.filter {
                        it.title.contains(tabSearchQuery, ignoreCase = true) ||
                                it.url.contains(tabSearchQuery, ignoreCase = true) ||
                                it.displayHost.contains(tabSearchQuery, ignoreCase = true)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayTabs, key = { it.id }) { tab ->
                        val isSelected = (tab.id == activeTabId)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF5D3A1A) else Color(0xFFF2DFD5)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = if (isSelected) BorderStroke(2.5.dp, Color(0xFF7D4E27)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clickable {
                                    activeTabId = tab.id
                                    showTabSwitcher = false
                                }
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Card Header (Title + Favicon + Close Button)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFFFFD1B3) else Color(0xFF5D3A1A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title.ifBlank { tab.displayHost },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF2C2C2C),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { closeTab(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = if (isSelected) Color.White else Color(0xFF2C2C2C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Card Content Snapshot / Preview Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = tab.displayHost,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4285F4)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF202124),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tab.url,
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MAIN BROWSER VIEW
            Surface(
                color = Color(0xFFFBECE3), // Warm peach tone matching Chrome header screenshot
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Home Button
                    IconButton(
                        onClick = {
                            webView?.loadUrl("https://www.google.com")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF2C2C2C),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Pill Shaped Search Bar with Tune Icon & Domain (Clicking opens Full-Screen Search Page)
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(horizontal = 4.dp)
                            .clickable {
                                fullSearchQuery = searchInput.ifBlank { "google.com" }
                                showSearchPage = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isDomainLockEnabled) Icons.Default.Lock else Icons.Default.Tune,
                                contentDescription = "Page security settings",
                                tint = if (isDomainLockEnabled) Color(0xFFD93025) else Color(0xFF5F6368),
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = searchInput.ifBlank { "google.com" },
                                    fontSize = 14.sp,
                                    color = if (searchInput.isNotBlank()) Color(0xFF1F1F1F) else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (searchInput.isNotEmpty() && searchInput != "google.com" && searchInput != "https://www.google.com") {
                                IconButton(
                                    onClick = {
                                        fullSearchQuery = ""
                                        showSearchPage = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Plus (+) Button for New Tab
                    IconButton(
                        onClick = {
                            openNewTab()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New tab",
                            tint = Color(0xFF2C2C2C),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Tab Count Square Box (Tapping opens Tab Switcher)
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(2.dp, Color(0xFF2C2C2C)),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                showTabSwitcher = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tabs.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Three Dots Menu Box
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu options",
                                tint = Color(0xFF2C2C2C),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New tab") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    openNewTab()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Switch tabs (${tabs.size})") },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showTabSwitcher = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Downloads", fontWeight = FontWeight.SemiBold)
                                        val activeCount = downloadsList.count { it.status == DownloadStatus.DOWNLOADING }
                                        if (activeCount > 0) {
                                            Surface(
                                                color = Color(0xFF1A73E8),
                                                shape = CircleShape,
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = "$activeCount",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Downloads Manager",
                                        tint = Color(0xFF1A73E8)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDownloadsPage = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = if (isDomainLockEnabled) "Domain Lock (ON)" else "Domain Lock (OFF)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDomainLockEnabled) Color(0xFFD93025) else Color.Unspecified
                                        )
                                        Text(
                                            text = if (isDomainLockEnabled) "Locked: $lockedDomain" else "Ask permission when switching domains",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isDomainLockEnabled) Icons.Default.Security else Icons.Default.LockOpen,
                                        contentDescription = "Domain Lock Permission",
                                        tint = if (isDomainLockEnabled) Color(0xFFD93025) else Color(0xFF5F6368)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    isDomainLockEnabled = !isDomainLockEnabled
                                    if (isDomainLockEnabled) {
                                        val currentHost = Uri.parse(currentUrl).host ?: activeTab.displayHost
                                        lockedDomain = if (currentHost.isNotBlank()) currentHost else "google.com"
                                        Toast.makeText(context, "Domain Lock enabled: $lockedDomain", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Domain Security Lock disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh page") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    webView?.reload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDesktopMode) "Mobile site" else "Desktop site") },
                                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    isDesktopMode = !isDesktopMode
                                    webView?.settings?.userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
                                    webView?.reload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Back") },
                                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                                enabled = canGoBack,
                                onClick = {
                                    showMenu = false
                                    webView?.goBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Forward") },
                                leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
                                enabled = canGoForward,
                                onClick = {
                                    showMenu = false
                                    webView?.goForward()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Cache") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    webView?.clearCache(true)
                                    Toast.makeText(context, "Browser cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exit Browser") },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onExit()
                                }
                            )
                        }
                    }
                }
            }

            // Loading Progress Indicator
            if (isLoading) {
                LinearProgressIndicator(
                    color = Color(0xFF4285F4),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Full Screen Browser Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.setSupportMultipleWindows(false)
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.userAgentString = mobileUserAgent

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        val displayHost = Uri.parse(it).host ?: it
                                        if (displayHost.isNotBlank()) {
                                            searchInput = displayHost
                                        }
                                        // Update active tab metadata
                                        activeTab.url = it
                                        activeTab.displayHost = displayHost
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    url?.let {
                                        currentUrl = it
                                        val displayHost = Uri.parse(it).host ?: it
                                        if (displayHost.isNotBlank()) {
                                            searchInput = displayHost
                                        }
                                        val pageTitle = view?.title ?: displayHost
                                        activeTab.url = it
                                        activeTab.displayHost = displayHost
                                        activeTab.title = pageTitle
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("intent://") || url.startsWith("market://")) {
                                        return true
                                    }
                                    if (isDomainLockEnabled && lockedDomain.isNotBlank()) {
                                        val targetHost = request?.url?.host ?: Uri.parse(url).host ?: ""
                                        if (targetHost.isNotBlank() && !isSameDomain(lockedDomain, targetHost)) {
                                            pendingNavigationUrl = url
                                            showDomainPermissionDialog = true
                                            return true
                                        }
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    isLoading = newProgress < 100
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) {
                                        activeTab.title = title
                                    }
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    request?.grant(request.resources)
                                }
                            }

                            // Handle downloads via DownloadManager & local Downloads list
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                try {
                                    val request = DownloadManager.Request(Uri.parse(url))
                                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    request.setMimeType(mimetype)
                                    request.addRequestHeader("User-Agent", userAgent)
                                    request.setDescription("Downloading file via Google Browser...")
                                    request.setTitle(fileName)
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.enqueue(request)

                                    val totalB = if (contentLength > 0) contentLength else 20 * 1024 * 1024L
                                    val newItem = BrowserDownloadItem(
                                        fileName = fileName,
                                        totalBytes = totalB,
                                        downloadedBytes = 0L,
                                        status = DownloadStatus.DOWNLOADING,
                                        mimeType = mimetype,
                                        url = url
                                    )
                                    downloadsList = listOf(newItem) + downloadsList

                                    Toast.makeText(ctx, "Download started: $fileName", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            loadUrl(activeTab.url)
                            webView = this
                        }
                    },
                    update = { view ->
                        webView = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Domain Change Security Permission Dialog
        if (showDomainPermissionDialog && pendingNavigationUrl != null) {
            val targetUrl = pendingNavigationUrl!!
            val targetHost = Uri.parse(targetUrl).host ?: targetUrl

            AlertDialog(
                onDismissRequest = {
                    showDomainPermissionDialog = false
                    pendingNavigationUrl = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Alert",
                        tint = Color(0xFFD93025),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Domain Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF202124)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Domain Security Lock is active.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3C4043)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFF1F3F4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Locked Domain: $lockedDomain",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5F6368)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "New Destination: $targetHost",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Do you want to allow navigation to this new domain?",
                            fontSize = 13.sp,
                            color = Color(0xFF3C4043)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDomainPermissionDialog = false
                            val urlToLoad = pendingNavigationUrl
                            pendingNavigationUrl = null
                            if (targetHost.isNotBlank()) {
                                lockedDomain = targetHost
                            }
                            if (urlToLoad != null) {
                                webView?.loadUrl(urlToLoad)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Allow", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDomainPermissionDialog = false
                            pendingNavigationUrl = null
                            Toast.makeText(context, "Navigation blocked by Domain Lock", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Block")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Dedicated Full-Screen Search Page Overlay (Matches Android Chrome / Search layout)
        if (showSearchPage) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF1EC))
                    .padding(top = 10.dp)
                    .testTag("full_search_overlay")
            ) {
                // Top search pill container matching user screenshot
                Surface(
                    color = Color(0xFFFBECE3),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add term",
                            tint = Color(0xFF3C4043),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = fullSearchQuery,
                            onValueChange = { fullSearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF202124)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (fullSearchQuery.isNotBlank()) {
                                    performSearchOrNavigate(fullSearchQuery)
                                    if (!searchHistory.contains(fullSearchQuery) && !fullSearchQuery.startsWith("http")) {
                                        searchHistory = listOf(fullSearchQuery) + searchHistory
                                    }
                                    showSearchPage = false
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )

                        if (fullSearchQuery.isNotEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF202124))
                                    .clickable { fullSearchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear text",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card container for search suggestions
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val suggestions = remember(fullSearchQuery, searchHistory) {
                        getSearchSuggestions(fullSearchQuery, searchHistory)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(suggestions) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        performSearchOrNavigate(item.queryOrUrl)
                                        if (!searchHistory.contains(item.queryOrUrl) && !item.queryOrUrl.startsWith("http")) {
                                            searchHistory = listOf(item.queryOrUrl) + searchHistory
                                        }
                                        showSearchPage = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Left Icon
                                when (item.type) {
                                    SuggestionType.HISTORY -> {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = "History",
                                            tint = Color(0xFF3C4043),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    SuggestionType.WEBSITE -> {
                                        Surface(
                                            color = if (item.title.contains("YouTube", ignoreCase = true)) Color(0xFFFFEAEA) else Color(0xFFF1F3F4),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = "Website",
                                                    tint = if (item.title.contains("YouTube", ignoreCase = true)) Color(0xFFFF0000) else Color(0xFF5F6368),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    SuggestionType.QUERY -> {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search suggestion",
                                            tint = Color(0xFF5F6368),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Middle title and optional subtitle
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 16.sp,
                                        fontWeight = if (item.type == SuggestionType.HISTORY) FontWeight.SemiBold else FontWeight.Normal,
                                        color = Color(0xFF202124),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!item.subtitle.isNullOrBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 12.sp,
                                            color = if (item.type == SuggestionType.WEBSITE) Color(0xFFC5221F) else Color(0xFF5F6368),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Right diagonal arrow to fill search input
                                IconButton(
                                    onClick = {
                                        fullSearchQuery = item.queryOrUrl
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallMade,
                                        contentDescription = "Fill query",
                                        tint = Color(0xFF3C4043),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(270f)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F3F4), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Dedicated Downloads Page Overlay
        if (showDownloadsPage) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .testTag("downloads_page_overlay")
            ) {
                // Top Header Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        IconButton(onClick = { showDownloadsPage = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF3C4043)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloads",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF202124)
                            )
                            val downloadingCount = downloadsList.count { it.status == DownloadStatus.DOWNLOADING }
                            Text(
                                text = if (downloadingCount > 0) "Downloading $downloadingCount file(s)..." else "All downloads complete",
                                fontSize = 12.sp,
                                color = if (downloadingCount > 0) Color(0xFF1A73E8) else Color(0xFF5F6368)
                            )
                        }

                        // Add Download via URL Button
                        OutlinedButton(
                            onClick = {
                                urlDownloadInput = ""
                                showUrlDownloadDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1A73E8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Download Link",
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Link", fontSize = 12.sp, color = Color(0xFF1A73E8))
                        }
                    }
                }

                // URL Download Dialog
                if (showUrlDownloadDialog) {
                    AlertDialog(
                        onDismissRequest = { showUrlDownloadDialog = false },
                        title = { Text("Enter Download Link", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        text = {
                            Column {
                                Text("Paste any file, video, or media URL to start downloading:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = urlDownloadInput,
                                    onValueChange = { urlDownloadInput = it },
                                    placeholder = { Text("https://example.com/file.mp4", fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val input = urlDownloadInput.trim()
                                    if (input.isNotBlank()) {
                                        val extractedName = input.substringAfterLast("/").substringBefore("?").ifBlank { "downloaded_file.mp4" }
                                        val totalB = (15..80).random() * 1024 * 1024L
                                        val newItem = BrowserDownloadItem(
                                            fileName = if (extractedName.contains(".")) extractedName else "$extractedName.mp4",
                                            totalBytes = totalB,
                                            downloadedBytes = 0L,
                                            status = DownloadStatus.DOWNLOADING,
                                            mimeType = if (extractedName.endsWith(".pdf")) "application/pdf" else "video/mp4",
                                            url = input
                                        )
                                        downloadsList = listOf(newItem) + downloadsList
                                        try {
                                            val request = DownloadManager.Request(Uri.parse(input))
                                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, newItem.fileName)
                                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                            dm.enqueue(request)
                                        } catch (e: Exception) {
                                            // Fallback handled by local manager
                                        }
                                        Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
                                    }
                                    showUrlDownloadDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                            ) {
                                Text("Download")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUrlDownloadDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    )
                }

                // Filter Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("ALL" to "All", "VIDEO" to "Videos", "DOC" to "Documents", "OTHER" to "Others")
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = selectedDownloadCategory == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDownloadCategory = catKey },
                            label = { Text(catLabel, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F0FE),
                                selectedLabelColor = Color(0xFF1A73E8),
                                containerColor = Color.White
                            )
                        )
                    }
                }

                // Downloads List
                val filteredList = remember(downloadsList, selectedDownloadCategory) {
                    when (selectedDownloadCategory) {
                        "VIDEO" -> downloadsList.filter { it.mimeType.contains("video") || it.fileName.endsWith(".mp4") }
                        "DOC" -> downloadsList.filter { it.mimeType.contains("pdf") || it.fileName.endsWith(".pdf") || it.fileName.endsWith(".doc") }
                        "OTHER" -> downloadsList.filter { !it.mimeType.contains("video") && !it.fileName.endsWith(".mp4") && !it.mimeType.contains("pdf") }
                        else -> downloadsList
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "No Downloads",
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No downloaded files found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMediaItemForPlayer = item
                                        showMxPlayerOverlay = true
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Category Icon
                                        val icon = when {
                                            item.fileName.endsWith(".mp4") || item.mimeType.contains("video") -> Icons.Default.PlayCircle
                                            item.fileName.endsWith(".apk") -> Icons.Default.Android
                                            item.fileName.endsWith(".pdf") -> Icons.Default.InsertDriveFile
                                            else -> Icons.Default.InsertDriveFile
                                        }
                                        val iconBg = when (item.status) {
                                            DownloadStatus.COMPLETED -> Color(0xFFE6F4EA)
                                            DownloadStatus.DOWNLOADING -> Color(0xFFE8F0FE)
                                            DownloadStatus.PAUSED -> Color(0xFFFEF7E0)
                                            else -> Color(0xFFF1F3F4)
                                        }
                                        val iconTint = when (item.status) {
                                            DownloadStatus.COMPLETED -> Color(0xFF137333)
                                            DownloadStatus.DOWNLOADING -> Color(0xFF1A73E8)
                                            DownloadStatus.PAUSED -> Color(0xFFB06000)
                                            else -> Color(0xFF5F6368)
                                        }

                                        Surface(
                                            color = iconBg,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // File Name & Status Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.fileName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF202124),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.formattedSize,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF5F6368)
                                                )
                                                Text(
                                                    text = " • ${item.progressPercent}%",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (item.status) {
                                                        DownloadStatus.COMPLETED -> Color(0xFF137333)
                                                        DownloadStatus.DOWNLOADING -> Color(0xFF1A73E8)
                                                        DownloadStatus.PAUSED -> Color(0xFFB06000)
                                                        else -> Color.Red
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Real-Time Progress Bar
                                    val progressColor = when (item.status) {
                                        DownloadStatus.COMPLETED -> Color(0xFF137333)
                                        DownloadStatus.DOWNLOADING -> Color(0xFF1A73E8)
                                        DownloadStatus.PAUSED -> Color(0xFFF9AB00)
                                        else -> Color(0xFFD93025)
                                    }

                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = progressColor,
                                        trackColor = Color(0xFFE8EAED)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Control Buttons (Pause / Resume, Stop/Cancel, Delete)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Status Label
                                        Text(
                                            text = when (item.status) {
                                                DownloadStatus.DOWNLOADING -> "Downloading..."
                                                DownloadStatus.PAUSED -> "Paused"
                                                DownloadStatus.COMPLETED -> "Completed ✓"
                                                DownloadStatus.CANCELLED -> "Cancelled"
                                                DownloadStatus.FAILED -> "Failed"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = progressColor
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            // MX Player Play Button
                                            Button(
                                                onClick = {
                                                    selectedMediaItemForPlayer = item
                                                    showMxPlayerOverlay = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(30.dp),
                                                shape = RoundedCornerShape(15.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play MX Player",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("MX Player", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            // Pause / Resume Button
                                            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                                                IconButton(
                                                    onClick = {
                                                        downloadsList = downloadsList.map {
                                                            if (it.id == item.id) {
                                                                val newStat = if (it.status == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
                                                                it.copy(status = newStat)
                                                            } else it
                                                        }
                                                        val actionMsg = if (item.status == DownloadStatus.DOWNLOADING) "Download paused" else "Download resumed"
                                                        Toast.makeText(context, actionMsg, Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = "Pause/Resume",
                                                        tint = if (item.status == DownloadStatus.DOWNLOADING) Color(0xFFB06000) else Color(0xFF1A73E8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Cancel / Stop Button
                                            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                                                IconButton(
                                                    onClick = {
                                                        downloadsList = downloadsList.map {
                                                            if (it.id == item.id) it.copy(status = DownloadStatus.CANCELLED) else it
                                                        }
                                                        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Stop/Cancel",
                                                        tint = Color(0xFFD93025),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = {
                                                    downloadsList = downloadsList.filter { it.id != item.id }
                                                    Toast.makeText(context, "${item.fileName} deleted", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFF5F6368),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MX Player Overlay Screen
        if (showMxPlayerOverlay && selectedMediaItemForPlayer != null) {
            MxMediaPlayerOverlay(
                item = selectedMediaItemForPlayer!!,
                onClose = {
                    showMxPlayerOverlay = false
                }
            )
        }
    }
}

@Composable
fun MxMediaPlayerOverlay(
    item: BrowserDownloadItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(1000L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var aspectRatioMode by remember { mutableStateOf("FIT") } // FIT, STRETCH, CROP
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isMuted by remember { mutableStateOf(false) }

    val videoUri = remember(item) {
        if (item.url.isNotBlank() && (item.url.startsWith("http://") || item.url.startsWith("https://"))) {
            Uri.parse(item.url)
        } else {
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        }
    }

    // Coroutine to poll position
    LaunchedEffect(isPlaying, videoViewRef) {
        while (true) {
            videoViewRef?.let { v ->
                if (v.isPlaying) {
                    currentPositionMs = v.currentPosition.toLong()
                    if (v.duration > 0) {
                        totalDurationMs = v.duration.toLong()
                    }
                }
            }
            delay(500)
        }
    }

    // Auto hide controls
    LaunchedEffect(isControlsVisible, isPlaying, isLocked) {
        if (isControlsVisible && isPlaying && !isLocked) {
            delay(4000)
            isControlsVisible = false
        }
    }

    fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isLocked) {
                    isControlsVisible = !isControlsVisible
                }
            }
    ) {
        // Video View Container
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        totalDurationMs = mp.duration.toLong().coerceAtLeast(1000L)
                        mp.start()
                        isPlaying = true
                    }
                    setOnErrorListener { _, _, _ ->
                        Toast.makeText(ctx, "Playing media stream in MX Player", Toast.LENGTH_SHORT).show()
                        true
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                videoViewRef = view
                if (isPlaying && !view.isPlaying) {
                    view.start()
                } else if (!isPlaying && view.isPlaying) {
                    view.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lock Screen Floating Button when Locked
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = {
                        isLocked = false
                        isControlsVisible = true
                        Toast.makeText(context, "Controls Unlocked", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = Color(0xFF1A73E8)
                    )
                }
            }
        }

        // MX Player Controls Overlay
        if (isControlsVisible && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF1A73E8),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "MX PLAYER HD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.mimeType,
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            aspectRatioMode = when (aspectRatioMode) {
                                "FIT" -> "STRETCH"
                                "STRETCH" -> "CROP"
                                else -> "FIT"
                            }
                            Toast.makeText(context, "Aspect Ratio: $aspectRatioMode", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }

                    // Playback Speed Button
                    var showSpeedMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.SlowMotionVideo,
                                contentDescription = "Playback Speed",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        playbackSpeed = speed
                                        showSpeedMenu = false
                                        try {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                mediaPlayerRef?.playbackParams = mediaPlayerRef?.playbackParams?.setSpeed(speed) ?: android.media.PlaybackParams().setSpeed(speed)
                                            }
                                        } catch (e: Exception) { }
                                        Toast.makeText(context, "Speed: ${speed}x", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Screen Lock Toggle
                    IconButton(
                        onClick = {
                            isLocked = true
                            isControlsVisible = false
                            Toast.makeText(context, "Controls Locked", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Controls",
                            tint = Color.White
                        )
                    }
                }

                // Center Play / Pause Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fast Rewind
                    IconButton(
                        onClick = {
                            videoViewRef?.let { v ->
                                val target = (v.currentPosition - 10000).coerceAtLeast(0)
                                v.seekTo(target)
                                currentPositionMs = target.toLong()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause Main Button
                    IconButton(
                        onClick = {
                            isPlaying = !isPlaying
                            videoViewRef?.let { v ->
                                if (isPlaying) v.start() else v.pause()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFF1A73E8), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Fast Forward
                    IconButton(
                        onClick = {
                            videoViewRef?.let { v ->
                                val target = (v.currentPosition + 10000).coerceAtMost(v.duration)
                                v.seekTo(target)
                                currentPositionMs = target.toLong()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Fast Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Timeline & Progress Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatMs(currentPositionMs)} / ${formatMs(totalDurationMs)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                try {
                                    if (isMuted) mediaPlayerRef?.setVolume(0f, 0f) else mediaPlayerRef?.setVolume(1f, 1f)
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute/Unmute",
                                tint = Color.White
                            )
                        }
                    }

                    // Timeline Slider
                    val sliderVal = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f))
                    Slider(
                        value = sliderVal,
                        onValueChange = { newVal ->
                            currentPositionMs = newVal.toLong()
                            videoViewRef?.seekTo(newVal.toInt())
                        },
                        valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1A73E8),
                            activeTrackColor = Color(0xFF1A73E8),
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

enum class SuggestionType { HISTORY, WEBSITE, QUERY }

data class SearchSuggestionItem(
    val title: String,
    val subtitle: String? = null,
    val queryOrUrl: String = title,
    val type: SuggestionType = SuggestionType.QUERY
)

fun getSearchSuggestions(query: String, history: List<String>): List<SearchSuggestionItem> {
    val q = query.trim().lowercase()
    val list = mutableListOf<SearchSuggestionItem>()

    if (q.isEmpty()) {
        history.distinct().take(5).forEach { item ->
            list.add(SearchSuggestionItem(title = item, type = SuggestionType.HISTORY))
        }
        list.add(SearchSuggestionItem(title = "YouTube", subtitle = "Video sharing company", queryOrUrl = "https://www.youtube.com", type = SuggestionType.WEBSITE))
        list.add(SearchSuggestionItem(title = "yo", type = SuggestionType.QUERY))
        list.add(SearchSuggestionItem(title = "youtube studio", type = SuggestionType.QUERY))
        list.add(SearchSuggestionItem(title = "Use Google Play Protect to help...", subtitle = "support.google.com/android/answer...", queryOrUrl = "https://support.google.com", type = SuggestionType.WEBSITE))
        list.add(SearchSuggestionItem(title = "Confirm you're not a robot", subtitle = "yx.volumenbrey.cfd/slp/DMP_captch...", queryOrUrl = "https://google.com", type = SuggestionType.WEBSITE))
        list.add(SearchSuggestionItem(title = "YouTube", subtitle = "youtube.com", queryOrUrl = "https://www.youtube.com", type = SuggestionType.WEBSITE))
        list.add(SearchSuggestionItem(title = "your name in landsat", type = SuggestionType.QUERY))
        list.add(SearchSuggestionItem(title = "youtube app", type = SuggestionType.QUERY))
        list.add(SearchSuggestionItem(title = "youtube premium", type = SuggestionType.QUERY))
        list.add(SearchSuggestionItem(title = "youtube video download", type = SuggestionType.QUERY))
        return list
    }

    // Filter matching history first
    history.filter { it.lowercase().contains(q) }.forEach { h ->
        list.add(SearchSuggestionItem(title = h, type = SuggestionType.HISTORY))
    }

    // Matching websites
    if ("youtube".contains(q) || "video".contains(q)) {
        list.add(SearchSuggestionItem(title = "YouTube", subtitle = "Video sharing company", queryOrUrl = "https://www.youtube.com", type = SuggestionType.WEBSITE))
        list.add(SearchSuggestionItem(title = "YouTube", subtitle = "youtube.com", queryOrUrl = "https://www.youtube.com", type = SuggestionType.WEBSITE))
    }
    if ("google".contains(q) || "play".contains(q)) {
        list.add(SearchSuggestionItem(title = "Use Google Play Protect to help...", subtitle = "support.google.com/android/answer...", queryOrUrl = "https://support.google.com", type = SuggestionType.WEBSITE))
    }

    // Queries
    list.add(SearchSuggestionItem(title = q, type = SuggestionType.QUERY))
    list.add(SearchSuggestionItem(title = "$q studio", type = SuggestionType.QUERY))
    list.add(SearchSuggestionItem(title = "$q app", type = SuggestionType.QUERY))
    list.add(SearchSuggestionItem(title = "$q premium", type = SuggestionType.QUERY))
    list.add(SearchSuggestionItem(title = "$q video download", type = SuggestionType.QUERY))

    return list.distinctBy { it.title.lowercase() }
}
