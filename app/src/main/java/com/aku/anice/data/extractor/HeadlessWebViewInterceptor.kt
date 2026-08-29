package com.aku.anice.data.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class HeadlessWebViewInterceptor(private val context: Context) {

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isFound = AtomicBoolean(false)
    private val mobileUserAgent = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun intercept(url: String): VideoStream? = suspendCancellableCoroutine { continuation ->
        mainHandler.post {
            try {
                isFound.set(false)
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString = mobileUserAgent

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: ""
                            val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()

                            if (isStreamUrl(reqUrl) && !isFound.get()) {
                                isFound.set(true)
                                Log.d("Interceptor", "Found Stream: $reqUrl")
                                
                                // Ambil Cookie Domain
                                val cookies = CookieManager.getInstance().getCookie(reqUrl)
                                if (!cookies.isNullOrEmpty()) {
                                    headers["Cookie"] = cookies
                                }
                                
                                // Pastikan sinkron dengan mobile UA
                                headers["User-Agent"] = mobileUserAgent

                                val stream = VideoStream(
                                    url = reqUrl,
                                    headers = headers,
                                    isHls = reqUrl.contains(".m3u8") || reqUrl.contains("hls")
                                )
                                
                                mainHandler.postDelayed({
                                    cleanup()
                                    if (continuation.isActive) continuation.resume(stream)
                                }, 1000)
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Beberapa player butuh "klik" manual atau nunggu JS jalan
                            view?.evaluateJavascript("(function() { " +
                                    "var v = document.querySelector('video'); if(v) v.play(); " +
                                    "var b = document.querySelector('button'); if(b) b.click(); " +
                                    "})();", null)
                        }
                    }

                    loadUrl(url)
                }

                // Timeout 15 detik
                mainHandler.postDelayed({
                    if (!isFound.get()) {
                        Log.e("Interceptor", "Timeout reaching: $url")
                        cleanup()
                        if (continuation.isActive) continuation.resume(null)
                    }
                }, 15000)

            } catch (e: Exception) {
                Log.e("Interceptor", "Error: ${e.message}")
                cleanup()
                if (continuation.isActive) continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            mainHandler.post { cleanup() }
        }
    }

    private fun isStreamUrl(url: String): Boolean {
        return url.contains(".m3u8") || 
               url.contains(".mp4") || 
               url.contains("/hls/") || 
               url.contains("playlist.m3u8") ||
               (url.contains("googleusercontent.com") && url.contains("video"))
    }

    private fun cleanup() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
    }
}
