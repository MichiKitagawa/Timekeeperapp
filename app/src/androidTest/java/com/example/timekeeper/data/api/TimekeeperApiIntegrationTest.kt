package com.example.timekeeper.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.common.truth.Truth.assertThat

/**
 * Timekeeper API の統合テスト
 */
@RunWith(AndroidJUnit4::class)
class TimekeeperApiIntegrationTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TimekeeperApiService
    private lateinit var repository: TimekeeperRepository
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TimekeeperApiService::class.java)
        
        repository = TimekeeperRepository(apiService)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun testLicenseConfirmApiFlow() = runTest {
        // Given - 正常なライセンス確認レスポンスを設定
        val deviceId = "test-device-123"
        val purchaseToken = "cs_test_valid_token"
        val expectedResponse = LicenseConfirmResponse(status = "ok")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(expectedResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When - API を呼び出す
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then - 結果を検証
        assertThat(result).isInstanceOf(ApiResponse.Success::class.java)
        val successResult = result as ApiResponse.Success
        assertThat(successResult.data.status).isEqualTo("ok")
        
        // リクエストの詳細を検証
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.method).isEqualTo("POST")
        assertThat(recordedRequest.path).isEqualTo("/license/confirm")
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/json")
        
        // リクエストボディを検証
        val requestBody = Gson().fromJson(
            recordedRequest.body.readUtf8(),
            LicenseConfirmRequest::class.java
        )
        assertThat(requestBody.device_id).isEqualTo(deviceId)
        assertThat(requestBody.purchase_token).isEqualTo(purchaseToken)
    }
    
    @Test
    fun testDaypassUnlockApiFlow() = runTest {
        // Given - 正常なデイパス購入レスポンスを設定
        val deviceId = "test-device-456"
        val purchaseToken = "cs_test_daypass_token"
        val expectedResponse = UnlockDaypassResponse(
            status = "ok",
            unlock_count = 3,
            last_unlock_date = "2025-01-15"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(expectedResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When - API を呼び出す
        val result = repository.unlockDaypass(deviceId, purchaseToken)
        
        // Then - 結果を検証
        assertThat(result).isInstanceOf(ApiResponse.Success::class.java)
        val successResult = result as ApiResponse.Success
        assertThat(successResult.data.status).isEqualTo("ok")
        assertThat(successResult.data.unlock_count).isEqualTo(3)
        assertThat(successResult.data.last_unlock_date).isEqualTo("2025-01-15")
        
        // リクエストの詳細を検証
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.method).isEqualTo("POST")
        assertThat(recordedRequest.path).isEqualTo("/unlock/daypass")
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/json")
        
        // リクエストボディを検証
        val requestBody = Gson().fromJson(
            recordedRequest.body.readUtf8(),
            UnlockDaypassRequest::class.java
        )
        assertThat(requestBody.device_id).isEqualTo(deviceId)
        assertThat(requestBody.purchase_token).isEqualTo(purchaseToken)
    }
    
    @Test
    fun testApiErrorHandling() = runTest {
        // Given - エラーレスポンスを設定
        val deviceId = "invalid-device"
        val purchaseToken = "invalid-token"
        val errorResponse = ErrorResponse(
            error = "device_not_found",
            message = "device_id が未登録です"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody(Gson().toJson(errorResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When - API を呼び出す
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then - エラーが正しく処理されることを検証
        assertThat(result).isInstanceOf(ApiResponse.Error::class.java)
        val errorResult = result as ApiResponse.Error
        assertThat(errorResult.error).isEqualTo("device_not_found")
        assertThat(errorResult.message).isEqualTo("device_id が未登録です")
    }
    
    @Test
    fun testMultipleApiCalls() = runTest {
        // Given - 複数のレスポンスを設定
        val deviceId = "test-device-multi"
        
        // ライセンス確認のレスポンス
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(LicenseConfirmResponse(status = "ok")))
                .addHeader("Content-Type", "application/json")
        )
        
        // デイパス購入のレスポンス
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(UnlockDaypassResponse(
                    status = "ok",
                    unlock_count = 1,
                    last_unlock_date = "2025-01-15"
                )))
                .addHeader("Content-Type", "application/json")
        )
        
        // When - 複数のAPI を順次呼び出す
        val licenseResult = repository.confirmLicense(deviceId, "license-token")
        val daypassResult = repository.unlockDaypass(deviceId, "daypass-token")
        
        // Then - 両方の結果を検証
        assertThat(licenseResult).isInstanceOf(ApiResponse.Success::class.java)
        assertThat(daypassResult).isInstanceOf(ApiResponse.Success::class.java)
        
        val licenseSuccess = licenseResult as ApiResponse.Success
        val daypassSuccess = daypassResult as ApiResponse.Success
        
        assertThat(licenseSuccess.data.status).isEqualTo("ok")
        assertThat(daypassSuccess.data.status).isEqualTo("ok")
        assertThat(daypassSuccess.data.unlock_count).isEqualTo(1)
        
        // 2つのリクエストが送信されたことを確認
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }
} 