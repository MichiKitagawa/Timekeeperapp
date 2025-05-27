package com.example.timekeeper.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("purchase_state", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LICENSE_PURCHASED = "license_purchased"
        private const val KEY_LICENSE_PURCHASE_DATE = "license_purchase_date"
        private const val KEY_DAYPASS_UNLOCK_COUNT = "daypass_unlock_count"
        private const val KEY_LAST_UNLOCK_DATE = "last_unlock_date"
    }

    /**
     * ライセンス購入状態を設定
     */
    fun setLicensePurchased(purchased: Boolean, purchaseDate: String? = null) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_LICENSE_PURCHASED, purchased)
            purchaseDate?.let { putString(KEY_LICENSE_PURCHASE_DATE, it) }
            apply()
        }
    }

    /**
     * ライセンス購入状態を取得
     */
    fun isLicensePurchased(): Boolean {
        return sharedPreferences.getBoolean(KEY_LICENSE_PURCHASED, false)
    }

    /**
     * ライセンス購入日を取得
     */
    fun getLicensePurchaseDate(): String? {
        return sharedPreferences.getString(KEY_LICENSE_PURCHASE_DATE, null)
    }

    /**
     * デイパスのアンロック回数を更新
     */
    fun updateDaypassUnlockCount(unlockCount: Int, lastUnlockDate: String) {
        sharedPreferences.edit().apply {
            putInt(KEY_DAYPASS_UNLOCK_COUNT, unlockCount)
            putString(KEY_LAST_UNLOCK_DATE, lastUnlockDate)
            apply()
        }
    }

    /**
     * デイパスのアンロック回数を取得
     */
    fun getDaypassUnlockCount(): Int {
        return sharedPreferences.getInt(KEY_DAYPASS_UNLOCK_COUNT, 0)
    }

    /**
     * 最後のアンロック日を取得
     */
    fun getLastUnlockDate(): String? {
        return sharedPreferences.getString(KEY_LAST_UNLOCK_DATE, null)
    }

    /**
     * 今日がデイパス有効日かどうかを確認
     */
    fun isDaypassValidToday(): Boolean {
        val lastUnlockDate = getLastUnlockDate() ?: return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return lastUnlockDate == today
    }

    /**
     * 購入状態をクリア（テスト用）
     */
    fun clearPurchaseState() {
        sharedPreferences.edit().clear().apply()
    }
} 