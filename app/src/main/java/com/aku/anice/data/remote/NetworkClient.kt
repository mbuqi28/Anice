package com.aku.anice.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var client: OkHttpClient? = null

    fun getClient(context: Context): OkHttpClient {
        return client ?: synchronized(this) {
            val instance = OkHttpClient.Builder()
                .addInterceptor(CloudflareInterceptor(context.applicationContext))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            client = instance
            instance
        }
    }
    
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
}
