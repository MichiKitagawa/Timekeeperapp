package com.example.timekeeper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class PurchaseStateManagerTest {

    private lateinit var purchaseStateManager: PurchaseStateManager
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        purchaseStateManager = PurchaseStateManager(context)
        
        // テスト前にSharedPreferencesをクリア
        sharedPreferences = context.getSharedPreferences("purchase_state", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `初期状態でライセンス購入状態がfalseであること`() {
        // 初期状態の確認
        assertFalse(purchaseStateManager.isLicensePurchased())
        assertNull(purchaseStateManager.getLicensePurchaseDate())
    }

    @Test
    fun `ライセンス購入状態を設定できること`() {
        // ライセンス購入状態を設定
        purchaseStateManager.setLicensePurchased(true, "2024-01-01")
        
        // 設定した値が正しく取得できることを確認
        assertTrue(purchaseStateManager.isLicensePurchased())
        assertEquals("2024-01-01", purchaseStateManager.getLicensePurchaseDate())
    }

    @Test
    fun `ライセンス購入状態をfalseに設定できること`() {
        // まずtrueに設定
        purchaseStateManager.setLicensePurchased(true, "2024-01-01")
        assertTrue(purchaseStateManager.isLicensePurchased())
        
        // falseに設定
        purchaseStateManager.setLicensePurchased(false)
        
        // falseに更新されることを確認
        assertFalse(purchaseStateManager.isLicensePurchased())
        // 購入日は残る（仕様による）
        assertEquals("2024-01-01", purchaseStateManager.getLicensePurchaseDate())
    }

    @Test
    fun `購入日なしでライセンス購入状態を設定できること`() {
        // 購入日なしで設定
        purchaseStateManager.setLicensePurchased(true)
        
        // ライセンス購入状態のみ更新されることを確認
        assertTrue(purchaseStateManager.isLicensePurchased())
        assertNull(purchaseStateManager.getLicensePurchaseDate())
    }

    @Test
    fun `デイパスのアンロック回数を更新できること`() {
        // デイパスのアンロック回数を更新
        purchaseStateManager.updateDaypassUnlockCount(5, "2024-01-01")
        
        // SharedPreferencesに正しく保存されることを確認
        val unlockCount = sharedPreferences.getInt("daypass_unlock_count", 0)
        val lastUnlockDate = sharedPreferences.getString("last_unlock_date", null)
        
        assertEquals(5, unlockCount)
        assertEquals("2024-01-01", lastUnlockDate)
    }

    @Test
    fun `複数回の状態更新が正しく動作すること`() {
        // 複数回の更新
        purchaseStateManager.setLicensePurchased(true, "2024-01-01")
        purchaseStateManager.updateDaypassUnlockCount(3, "2024-01-02")
        purchaseStateManager.setLicensePurchased(false)
        
        // 最終状態の確認
        assertFalse(purchaseStateManager.isLicensePurchased())
        assertEquals("2024-01-01", purchaseStateManager.getLicensePurchaseDate())
        
        val unlockCount = sharedPreferences.getInt("daypass_unlock_count", 0)
        assertEquals(3, unlockCount)
    }

    @Test
    fun `SharedPreferencesのキー名が正しく使用されていること`() {
        // ライセンス購入状態を設定
        purchaseStateManager.setLicensePurchased(true, "2024-01-01")
        
        // SharedPreferencesに正しいキーで保存されていることを確認
        assertTrue(sharedPreferences.getBoolean("license_purchased", false))
        assertEquals("2024-01-01", sharedPreferences.getString("license_purchase_date", null))
    }
} 