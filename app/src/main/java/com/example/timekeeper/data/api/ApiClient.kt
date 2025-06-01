package com.example.timekeeper.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API クライアント設定クラス
 */
object ApiClient {
    
    private const val BASE_URL = "https://timekeeper-backend-827754096486.asia-northeast1.run.app"
    
    /**
     * Gson インスタンス
     */
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    /**
     * OkHttp クライアント
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit インスタンス
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * API サービスインスタンス
     */
    val apiService: TimekeeperApiService by lazy {
        retrofit.create(TimekeeperApiService::class.java)
    }
} 