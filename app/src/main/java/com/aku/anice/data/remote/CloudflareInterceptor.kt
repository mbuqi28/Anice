package com.aku.anice.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor(private val context: Context) : Interceptor {

    private val handler = Handler(Looper.getMainLooper())
    private var userAgent: String = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
    private val cookieManager = CookieManager.getInstance()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code == 403 || response.code == 503) {
            val html = response.peekBody(1024 * 1024).string()
            if (html.contains("cloudflare") || html.contains("cf-challenge") || html.contains("Turnstile")) {
                Log.d("CloudflareInterceptor", "Cloudflare detected, solving...")
                response.close()
                val cookies = solveChallenge(originalRequest.url.toString())
                if (cookies != null) {
                    val newRequest = originalRequest.newBuilder()
                        .header("Cookie", cookies)
                        .header("User-Agent", userAgent)
                        .build()
                    return chain.proceed(newRequest)
                }
            }
        }
        return response
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun solveChallenge(url: String): String? {
        val latch = CountDownLatch(1)
        var cookies: String? = null

        handler.post {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.userAgentString = userAgent
            webView.settings.domStorageEnabled = true
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val currentCookies = cookieManager.getCookie(url)
                    if (currentCookies != null && (currentCookies.contains("cf_clearance") || currentCookies.contains("cf_bm"))) {
                        cookies = currentCookies
                        Log.d("CloudflareInterceptor", "Challenge solved!")
                        latch.countDown()
                        webView.destroy()
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
            webView.loadUrl(url)
        }

        try {
            latch.await(30, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.e("CloudflareInterceptor", "Challenge timeout: ${e.message}")
        }

        return cookies
    }
}
