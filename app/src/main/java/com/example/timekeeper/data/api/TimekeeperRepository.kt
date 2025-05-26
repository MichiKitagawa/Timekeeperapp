package com.example.timekeeper.data.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Timekeeper API リポジトリ
 */
class TimekeeperRepository(
    private val apiService: TimekeeperApiService = ApiClient.apiService
) {
    
    /**
     * ライセンス購入確認API を呼び出す
     * 
     * @param deviceId デバイスID
     * @param purchaseToken 購入トークン
     * @return API レスポンス
     */
    suspend fun confirmLicense(
        deviceId: String,
        purchaseToken: String
    ): ApiResponse<LicenseConfirmResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LicenseConfirmRequest(
                device_id = deviceId,
                purchase_token = purchaseToken
            )
            
            val response = apiService.confirmLicense(request)
            handleResponse(response)
        } catch (e: Exception) {
            ApiResponse.Exception(e)
        }
    }
    
    /**
     * デイパス購入API を呼び出す
     * 
     * @param deviceId デバイスID
     * @param purchaseToken 購入トークン
     * @return API レスポンス
     */
    suspend fun unlockDaypass(
        deviceId: String,
        purchaseToken: String
    ): ApiResponse<DaypassUnlockResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DaypassUnlockRequest(
                device_id = deviceId,
                purchase_token = purchaseToken
            )
            
            val response = apiService.unlockDaypass(request)
            handleResponse(response)
        } catch (e: Exception) {
            ApiResponse.Exception(e)
        }
    }
    
    /**
     * Retrofit レスポンスを ApiResponse に変換する
     * 
     * @param response Retrofit レスポンス
     * @return API レスポンス
     */
    private fun <T> handleResponse(response: Response<T>): ApiResponse<T> {
        return if (response.isSuccessful) {
            response.body()?.let { body ->
                ApiResponse.Success(body)
            } ?: ApiResponse.Error("empty_response", "レスポンスが空です")
        } else {
            // エラーレスポンスをパース
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    ApiResponse.Error(errorResponse.error, errorResponse.message)
                } catch (e: Exception) {
                    ApiResponse.Error("parse_error", "エラーレスポンスの解析に失敗しました")
                }
            } else {
                ApiResponse.Error("unknown_error", "不明なエラーが発生しました")
            }
        }
    }
} 