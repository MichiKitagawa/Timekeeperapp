package com.example.timekeeper.data.api

/**
 * ライセンス確認API のリクエスト
 */
data class LicenseConfirmRequest(
    val device_id: String,
    val purchase_token: String
)

/**
 * デイパス購入API のリクエスト
 */
data class DaypassUnlockRequest(
    val device_id: String,
    val purchase_token: String
) 