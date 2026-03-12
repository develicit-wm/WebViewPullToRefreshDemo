package com.demo.webviewptr

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var webView: WebView
    private lateinit var tvStatus: TextView

    private var isWebViewAtTop = true
    private var isEditorFocused = false
    private var refreshCount = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "게시글 작성"
        toolbar.subtitle = "Pull-to-Refresh 테스트"

        tvStatus = findViewById(R.id.tvStatus)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupSwipeRefresh()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // JavaScript -> Native bridge
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        webView.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Core: only allow pull-to-refresh when WebView is scrolled to the very top
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0
        }

        swipeRefresh.setOnRefreshListener {
            refreshCount++
            Log.d(TAG, "Pull-to-Refresh triggered (#$refreshCount)")

            updateStatus("Refreshing... (#$refreshCount)")

            // Notify WebView about refresh
            webView.evaluateJavascript("onRefreshed()", null)

            // Simulate nettwork delay
            webView.postDelayed({
                swipeRefresh.isRefreshing = false
                updateStatus("Refreshed (#$refreshCount). isAtTop=$isWebViewAtTop, editorFocused=$isEditorFocused")
            }, 1500)
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread {
            tvStatus.text = msg
        }
    }

    /**
     * JavaScript Interface - receives scroll/focus events from WebView content
     */
    inner class WebAppInterface {

        @JavascriptInterface
        fun onScrollChanged(atTop: Boolean) {
            isWebViewAtTop = atTop
            Log.d(TAG, "WebView scroll - atTop: $atTop")
            updateStatus("scrollY atTop=$atTop | editorFocused=$isEditorFocused | refreshes=$refreshCount")
        }

        @JavascriptInterface
        fun onEditorFocusChanged(focused: Boolean) {
            isEditorFocused = focused
            Log.d(TAG, "Editor focus changed - focused: $focused")
            updateStatus("editorFocused=$focused | atTop=$isWebViewAtTop | refreshes=$refreshCount")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "WebViewPTR"
    }
}
