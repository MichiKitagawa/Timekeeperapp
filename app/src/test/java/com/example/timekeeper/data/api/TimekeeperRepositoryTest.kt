package com.example.timekeeper.data.api

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.common.truth.Truth.assertThat

/**
 * TimekeeperRepository の単体テスト
 */
class TimekeeperRepositoryTest {
    
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
    fun `confirmLicense - 成功レスポンスを正しく処理する`() = runTest {
        // Given
        val deviceId = "test-device-id"
        val purchaseToken = "test-purchase-token"
        val expectedResponse = LicenseConfirmResponse(status = "ok")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(expectedResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Success::class.java)
        val successResult = result as ApiResponse.Success
        assertThat(successResult.data.status).isEqualTo("ok")
        
        // リクエストの検証
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/license/confirm")
        assertThat(request.method).isEqualTo("POST")
        
        val requestBody = Gson().fromJson(request.body.readUtf8(), LicenseConfirmRequest::class.java)
        assertThat(requestBody.device_id).isEqualTo(deviceId)
        assertThat(requestBody.purchase_token).isEqualTo(purchaseToken)
    }
    
    @Test
    fun `confirmLicense - エラーレスポンスを正しく処理する`() = runTest {
        // Given
        val deviceId = "test-device-id"
        val purchaseToken = "invalid-token"
        val errorResponse = ErrorResponse(
            error = "invalid_purchase_token",
            message = "purchase_token が不正です"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(Gson().toJson(errorResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Error::class.java)
        val errorResult = result as ApiResponse.Error
        assertThat(errorResult.error).isEqualTo("invalid_purchase_token")
        assertThat(errorResult.message).isEqualTo("purchase_token が不正です")
    }
    
    @Test
    fun `confirmLicense - デバイス未登録エラーを正しく処理する`() = runTest {
        // Given
        val deviceId = "unregistered-device-id"
        val purchaseToken = "test-purchase-token"
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
        
        // When
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Error::class.java)
        val errorResult = result as ApiResponse.Error
        assertThat(errorResult.error).isEqualTo("device_not_found")
        assertThat(errorResult.message).isEqualTo("device_id が未登録です")
    }
    
    @Test
    fun `unlockDaypass - 成功レスポンスを正しく処理する`() = runTest {
        // Given
        val deviceId = "test-device-id"
        val purchaseToken = "test-purchase-token"
        val expectedResponse = DaypassUnlockResponse(
            status = "ok",
            unlock_count = 4,
            last_unlock_date = "2025-05-25"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Gson().toJson(expectedResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = repository.unlockDaypass(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Success::class.java)
        val successResult = result as ApiResponse.Success
        assertThat(successResult.data.status).isEqualTo("ok")
        assertThat(successResult.data.unlock_count).isEqualTo(4)
        assertThat(successResult.data.last_unlock_date).isEqualTo("2025-05-25")
        
        // リクエストの検証
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/unlock/daypass")
        assertThat(request.method).isEqualTo("POST")
        
        val requestBody = Gson().fromJson(request.body.readUtf8(), DaypassUnlockRequest::class.java)
        assertThat(requestBody.device_id).isEqualTo(deviceId)
        assertThat(requestBody.purchase_token).isEqualTo(purchaseToken)
    }
    
    @Test
    fun `unlockDaypass - エラーレスポンスを正しく処理する`() = runTest {
        // Given
        val deviceId = "test-device-id"
        val purchaseToken = "invalid-token"
        val errorResponse = ErrorResponse(
            error = "payment_verification_failed",
            message = "決済検証に失敗しました"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody(Gson().toJson(errorResponse))
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = repository.unlockDaypass(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Error::class.java)
        val errorResult = result as ApiResponse.Error
        assertThat(errorResult.error).isEqualTo("payment_verification_failed")
        assertThat(errorResult.message).isEqualTo("決済検証に失敗しました")
    }
    
    @Test
    fun `ネットワークエラー時に例外レスポンスを返す`() = runTest {
        // Given
        val deviceId = "test-device-id"
        val purchaseToken = "test-purchase-token"
        
        // サーバーを停止してネットワークエラーを発生させる
        mockWebServer.shutdown()
        
        // When
        val result = repository.confirmLicense(deviceId, purchaseToken)
        
        // Then
        assertThat(result).isInstanceOf(ApiResponse.Exception::class.java)
    }
} 