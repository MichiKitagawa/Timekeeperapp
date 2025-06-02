package com.example.timekeeper.di

import com.example.timekeeper.data.api.TimekeeperApiService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                
                // リクエスト/レスポンスの詳細をログ出力
                android.util.Log.d("NetworkModule", "=== HTTP Request ===")
                android.util.Log.d("NetworkModule", "URL: ${request.url}")
                android.util.Log.d("NetworkModule", "Method: ${request.method}")
                android.util.Log.d("NetworkModule", "Headers: ${request.headers}")
                
                android.util.Log.d("NetworkModule", "=== HTTP Response ===")
                android.util.Log.d("NetworkModule", "Status: ${response.code}")
                android.util.Log.d("NetworkModule", "Headers: ${response.headers}")
                
                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .serializeNulls()
            .setPrettyPrinting()
            // ClassCastException対策のため基本的な設定のみ使用
            .create()

        return Retrofit.Builder()
            .baseUrl("https://timekeeper-backend-827754096486.asia-northeast1.run.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideTimekeeperApiService(retrofit: Retrofit): TimekeeperApiService {
        return retrofit.create(TimekeeperApiService::class.java)
    }
} 
