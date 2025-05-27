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
data class UnlockDaypassRequest(
    val device_id: String,
    val purchase_token: String
)

/**
 * Stripe Checkoutセッション作成APIのリクエスト
 */
data class CreateCheckoutSessionRequest(
    val device_id: String,
    val product_type: String, // "license" or "daypass"
    val unlock_count: Int?   // デイパスの場合のみ使用
) 