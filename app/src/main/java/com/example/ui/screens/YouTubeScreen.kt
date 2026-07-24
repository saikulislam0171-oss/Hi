package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
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
    // 3. Or exit YouTube screen
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
            .testTag("youtube_fullscreen_container")
    ) {
        // Main YouTube WebView (kept active in composition so video state is maintained without reloading)
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
                    }

                    loadUrl("https://www.youtube.com")
                    webView = this
                }
            },
            update = { view ->
                webView = view
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for Fullscreen HTML5 Video Custom View (when YouTube full screen button is clicked)
        customView?.let { cView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("youtube_html5_fullscreen_overlay")
            ) {
                AndroidView(
                    factory = {
                        (cView.parent as? ViewGroup)?.removeView(cView)
                        cView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating exit button to exit custom fullscreen view
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

        // Loading indicator
        if (isLoading && customView == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xFFFF0000)
            )
        }

        // Floating exit button overlay for normal view mode
        if (customView == null) {
            SmallFloatingActionButton(
                onClick = { onExit() },
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 12.dp)
                    .size(36.dp)
                    .testTag("youtube_exit_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit YouTube",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
