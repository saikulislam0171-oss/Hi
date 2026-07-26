package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

private val AD_HOST_KEYWORDS = listOf(
    "popads", "popcash", "adsterra", "exoclick", "juicyads", "bet365", "doubleclick",
    "googlesyndication", "adservice", "propellerads", "monetag", "clickadu", "hilltopads",
    "a-ads", "ad-delivery", "adform", "adnxs", "adroll", "adsystem", "amazon-adsystem",
    "outbrain", "taboola", "mgid", "zeroredirect", "popunder", "banner", "popup",
    "tracking", "telemetry", "analytics", "statcounter", "histats", "ad-maven", "syndication"
)

private fun isAdUrl(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    val lower = url.lowercase()
    return AD_HOST_KEYWORDS.any { keyword -> lower.contains(keyword) }
}

private fun isInternalOrAllowedHost(url: String?): Boolean {
    if (url.isNullOrEmpty()) return true
    val lower = url.lowercase()
    if (lower.startsWith("javascript:") || lower.startsWith("about:") || lower.startsWith("data:")) return true

    val host = try {
        android.net.Uri.parse(url).host?.lowercase() ?: ""
    } catch (e: Exception) {
        ""
    }
    if (host.isEmpty()) return true

    val allowedHosts = listOf(
        "hdmoviescloud.com",
        "mcloud",
        "megacloud",
        "vidplay",
        "vidsrc",
        "filemoon",
        "streamtape",
        "doodstream",
        "mixdrop",
        "upstream"
    )
    return allowedHosts.any { host.contains(it) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HdMoviesScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var blockedAdCount by remember { mutableIntStateOf(0) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val hideCustomViewLambda = remember {
        {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        }
    }

    // Intercept back gesture:
    // 1. Exit HTML5 fullscreen mode if active
    // 2. Or go back in web history
    // 3. Or exit HD Movies screen
    BackHandler {
        if (customView != null) {
            hideCustomViewLambda()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onExit()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("hd_movies_fullscreen_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar with AdBlock Status and Actions (visible when not in HTML5 fullscreen)
            if (customView == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "AdBlock Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HD Movies Cloud (AdBlocker Active: $blockedAdCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { webView?.reload() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Page",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onExit() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Movies View",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // WebView Box
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    // Inject script to clean ad banners
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var selector = 'iframe[src*="ad"], div[id*="ad"], div[class*="ad"], div[id*="pop"], div[class*="pop"]';
                                            var els = document.querySelectorAll(selector);
                                            for(var i=0; i<els.length; i++){
                                                els[i].style.display='none';
                                            }
                                        })();
                                        """.trimIndent(), null
                                    )
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val url = request?.url?.toString()
                                    if (isAdUrl(url)) {
                                        post { blockedAdCount++ }
                                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString()
                                    if (isAdUrl(url)) {
                                        post { blockedAdCount++ }
                                        return true
                                    }
                                    if (!isInternalOrAllowedHost(url)) {
                                        post { pendingExternalUrl = url }
                                        return true
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (customView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    customView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    hideCustomViewLambda()
                                }

                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val result = view?.hitTestResult
                                    val extra = result?.extra
                                    if (!extra.isNullOrEmpty()) {
                                        if (isAdUrl(extra)) {
                                            post { blockedAdCount++ }
                                        } else if (!isInternalOrAllowedHost(extra)) {
                                            post { pendingExternalUrl = extra }
                                        } else {
                                            post { webView?.loadUrl(extra) }
                                        }
                                    } else {
                                        post { blockedAdCount++ }
                                    }
                                    return false
                                }
                            }

                            loadUrl("https://hdmoviescloud.com")
                            webView = this
                        }
                    },
                    update = { view ->
                        webView = view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // HTML5 Fullscreen Overlay (when user clicks full screen on a movie player)
                customView?.let { cView ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .testTag("hd_movies_html5_fullscreen_overlay")
                    ) {
                        AndroidView(
                            factory = {
                                (cView.parent as? ViewGroup)?.removeView(cView)
                                cView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating exit button to exit custom video fullscreen
                        SmallFloatingActionButton(
                            onClick = { hideCustomViewLambda() },
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Fullscreen",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Loading Indicator
                if (isLoading && customView == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                // Non-intrusive floating popup at the bottom for external links
                pendingExternalUrl?.let { extUrl ->
                    val domainName = remember(extUrl) {
                        try {
                            android.net.Uri.parse(extUrl).host ?: extUrl
                        } catch (e: Exception) {
                            extUrl
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E2C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .fillMaxWidth()
                            .testTag("external_link_popup_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "External Link",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open external link?",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = domainName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF90CAF9),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { pendingExternalUrl = null },
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("Cancel", fontSize = 12.sp, color = Color.LightGray)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        val urlToLoad = extUrl
                                        pendingExternalUrl = null
                                        webView?.loadUrl(urlToLoad)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2196F3),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
