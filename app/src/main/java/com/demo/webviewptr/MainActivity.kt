package com.demo.webviewptr

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WebViewPullToRefreshScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun WebViewPullToRefreshScreen() {
    var statusText by remember {
        mutableStateOf("Pull down to refresh. Scroll position and editor content will be tested.")
    }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var isWebViewAtTop by remember { mutableStateOf(true) }
    var isEditorFocused by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            refreshCount++
            isRefreshing = true
            Log.d(TAG, "Pull-to-Refresh triggered (#$refreshCount)")
            statusText = "Refreshing... (#$refreshCount)"

            webViewRef?.evaluateJavascript("onRefreshed()", null)

            scope.launch {
                delay(1500)
                isRefreshing = false
                statusText =
                    "Refreshed (#$refreshCount). isAtTop=$isWebViewAtTop, editorFocused=$isEditorFocused"
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("게시글 작성", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pull-to-Refresh 테스트",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status indicator
            Text(
                text = statusText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0))
                    .padding(8.dp),
                color = Color(0xFFE65100),
                fontSize = 13.sp
            )

            // PullRefresh wrapping WebView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                WebViewContent(
                    onWebViewCreated = { webView -> webViewRef = webView },
                    onScrollChanged = { atTop ->
                        isWebViewAtTop = atTop
                        Log.d(TAG, "WebView scroll - atTop: $atTop")
                        statusText =
                            "scrollY atTop=$atTop | editorFocused=$isEditorFocused | refreshes=$refreshCount"
                    },
                    onEditorFocusChanged = { focused ->
                        isEditorFocused = focused
                        Log.d(TAG, "Editor focus changed - focused: $focused")
                        statusText =
                            "editorFocused=$focused | atTop=$isWebViewAtTop | refreshes=$refreshCount"
                    }
                )

                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(
    onWebViewCreated: (WebView) -> Unit,
    onScrollChanged: (Boolean) -> Unit,
    onEditorFocusChanged: (Boolean) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                overScrollMode = WebView.OVER_SCROLL_NEVER
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onScrollChanged(atTop: Boolean) {
                            onScrollChanged(atTop)
                        }

                        @JavascriptInterface
                        fun onEditorFocusChanged(focused: Boolean) {
                            onEditorFocusChanged(focused)
                        }
                    },
                    "AndroidBridge"
                )

                loadUrl("file:///android_asset/editor.html")
                onWebViewCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private const val TAG = "WebViewPTR"
