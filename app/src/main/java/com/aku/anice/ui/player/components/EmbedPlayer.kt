package com.aku.anice.ui.player.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    val initialHost = Uri.parse(url).host ?: ""

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                webViewClient = object : WebViewClient() {
                    
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val newUrl = request?.url?.toString() ?: ""
                        val newHost = request?.url?.host ?: ""
                        
                        val allowedHosts = listOf(
                            initialHost,
                            "dailymotion.com",
                            "ok.ru",
                            "archive.org",
                            "blogger.com",
                            "google.com",
                            "googlevideo.com",
                            "streamwish",
                            "filemoon",
                            "mp4upload"
                        )
                        
                        return if (allowedHosts.any { newHost.contains(it) } || newUrl.contains("googlevideo")) {
                            false 
                        } else {
                            android.util.Log.d("AdBlock", "Blocked Redirect to: $newUrl")
                            true 
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString()?.lowercase() ?: ""
                        
                        val adKeywords = listOf(
                            "adsystem", "adservice", "popads", "popcash", "doubleclick",
                            "onclickads", "propellerads", "juicyads", "exoclick", "clksite",
                            "highperformancegate", "detector.js", "betting", "casino", "poker"
                        )
                        
                        if (adKeywords.any { reqUrl.contains(it) }) {
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                        }
                        
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            """
                            (function() {
                                window.open = function() { return null; };
                                var ads = document.querySelectorAll('[id*="pop"], [class*="pop"], [id*="ad-"], [class*="ad-"]');
                                for (var i = 0; i < ads.length; i++) {
                                    ads[i].remove();
                                }
                            })();
                            """.trimIndent(), 
                            null
                        )
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        return false 
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier
    )
}
