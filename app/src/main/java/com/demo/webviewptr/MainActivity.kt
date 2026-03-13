package com.demo.webviewptr

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    var isModalOpen by remember { mutableStateOf(false) }
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
                    isWebViewAtTop = isWebViewAtTop,
                    isModalOpen = isModalOpen,
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
                    },
                    onModalStateChanged = { open ->
                        isModalOpen = open
                        Log.d(TAG, "Modal state changed - open: $open")
                        statusText =
                            "modalOpen=$open | atTop=$isWebViewAtTop | refreshes=$refreshCount"
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun WebViewContent(
    isWebViewAtTop: Boolean,
    isModalOpen: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onScrollChanged: (Boolean) -> Unit,
    onEditorFocusChanged: (Boolean) -> Unit,
    onModalStateChanged: (Boolean) -> Unit
) {
    // NestedScrollDispatcher를 사용하여 WebView 터치 이벤트를 Compose 네스티드 스크롤 체인에 전달
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {}
    }

    // 상태를 클로저 밖에서 추적하기 위한 ref
    val atTopRef = remember { mutableStateOf(true) }
    atTopRef.value = isWebViewAtTop
    val modalOpenRef = remember { mutableStateOf(false) }
    modalOpenRef.value = isModalOpen

    AndroidView(
        factory = { context ->
            val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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

                        @JavascriptInterface
                        fun onModalStateChanged(isOpen: Boolean) {
                            onModalStateChanged(isOpen)
                        }
                    },
                    "AndroidBridge"
                )

                // WebView 터치 이벤트를 가로채서 Compose 네스티드 스크롤로 전달
                var startY = 0f
                var lastY = 0f
                var isPulling = false

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startY = event.y
                            lastY = event.y
                            isPulling = false
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaY = event.y - lastY
                            val totalDelta = event.y - startY

                            if (!isPulling && atTopRef.value && !modalOpenRef.value && totalDelta > touchSlop && deltaY > 0) {
                                // WebView가 최상단이고 아래로 드래그 시작 → pull 모드 진입
                                isPulling = true
                                // WebView의 터치 처리를 취소
                                val cancelEvent = MotionEvent.obtain(event).apply {
                                    action = MotionEvent.ACTION_CANCEL
                                }
                                v.onTouchEvent(cancelEvent)
                                cancelEvent.recycle()
                            }

                            if (isPulling) {
                                // Compose 네스티드 스크롤 체인에 스크롤 델타 전달
                                // pullRefresh는 onPostScroll에서 pull을 시작하므로 둘 다 호출해야 함
                                val available = Offset(0f, deltaY)
                                val preConsumed = nestedScrollDispatcher.dispatchPreScroll(
                                    available,
                                    NestedScrollSource.Drag
                                )
                                val postAvailable = available - preConsumed
                                nestedScrollDispatcher.dispatchPostScroll(
                                    preConsumed,
                                    postAvailable,
                                    NestedScrollSource.Drag
                                )
                                lastY = event.y
                                true // 이벤트 소비
                            } else {
                                lastY = event.y
                                false // WebView가 처리
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isPulling) {
                                isPulling = false
                                // fling velocity 0으로 전달하여 pull-to-refresh가 결과를 판단하게 함
                                CoroutineScope(Dispatchers.Main).launch {
                                    nestedScrollDispatcher.dispatchPreFling(Velocity.Zero)
                                    nestedScrollDispatcher.dispatchPostFling(Velocity.Zero, Velocity.Zero)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }

                loadUrl("file:///android_asset/editor.html")
                onWebViewCreated(this)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
    )
}

private const val TAG = "WebViewPTR"
