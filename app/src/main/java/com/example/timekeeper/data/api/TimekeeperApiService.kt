package com.example.timekeeper.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Timekeeper API サービスインターフェース
 */
interface TimekeeperApiService {
    
    /**
     * ライセンス購入完了の通知と状態登録／更新
     * 
     * @param request ライセンス確認リクエスト
     * @return ライセンス確認レスポンス
     */
    @POST("license/confirm")
    suspend fun confirmLicense(@Body request: LicenseConfirmRequest): Response<LicenseConfirmResponse>
    
    /**
     * デイパス（1日アンロック）課金完了の通知と状態更新
     * 
     * @param request デイパス購入リクエスト
     * @return デイパス購入レスポンス
     */
    @POST("unlock/daypass")
    suspend fun unlockDaypass(@Body request: DaypassUnlockRequest): Response<DaypassUnlockResponse>
} 