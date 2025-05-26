package com.example.timekeeper.data.api

import java.util.Date // DaypassUnlockResponseのために追加

/**
 * APIレスポンスの共通ラッパー
 */
sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val error: String, val message: String) : ApiResponse<Nothing>()
    data class Exception(val exception: Throwable) : ApiResponse<Nothing>()
}

/**
 * ライセンス確認APIのレスポンス
 */
data class LicenseConfirmResponse(
    val status: String
)

/**
 * デイパス購入APIのレスポンス
 */
data class DaypassUnlockResponse(
    val status: String,
    val unlock_count: Int,
    val last_unlock_date: String // APIからは YYYY-MM-DD 形式の文字列で返る
)

/**
 * エラーレスポンスの共通形式
 */
data class ErrorResponse(
    val error: String,
    val message: String
) 