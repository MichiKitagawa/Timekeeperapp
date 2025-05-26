package com.example.timekeeper.data.api

/**
 * API レスポンスの基底クラス
 */
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val error: String, val message: String) : ApiResponse<Nothing>()
    data class Exception(val exception: Throwable) : ApiResponse<Nothing>()
}

/**
 * ライセンス確認API のレスポンス
 */
data class LicenseConfirmResponse(
    val status: String
)

/**
 * デイパス購入API のレスポンス
 */
data class DaypassUnlockResponse(
    val status: String,
    val unlock_count: Int,
    val last_unlock_date: String
)

/**
 * エラーレスポンス
 */
data class ErrorResponse(
    val error: String,
    val message: String
) 