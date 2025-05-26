package com.example.timekeeper.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.ui.screens.ErrorType // P06連携用に追加
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 仮のデータストア（実際にはSharedPreferencesなどを利用）
object TempLicenseRepository {
    private val _licensePurchased = MutableStateFlow(false)
    val licensePurchased: StateFlow<Boolean> = _licensePurchased.asStateFlow()

    fun purchaseLicense() {
        _licensePurchased.value = true
    }

    // テスト用にリセットする機能 (必要であれば)
    fun reset() {
        _licensePurchased.value = false
    }
}

class LicenseViewModel : ViewModel() {
    private val tempRepository = TempLicenseRepository // DIは後回し
    // private val apiClient = ApiClient.instance // 仮 APIクライアント
    // private val deviceIdRepository = DeviceIdRepository.instance // 仮 deviceId取得

    private val _licensePurchased = MutableStateFlow(false)
    val licensePurchased: StateFlow<Boolean> = _licensePurchased.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorType?>(null) // P06連携用
    val errorState: StateFlow<ErrorType?> = _errorState.asStateFlow()

    init {
        viewModelScope.launch {
            // 初期状態でリセット (テストのため)
            // tempRepository.reset() // 実際のアプリでは不要な場合が多い
            tempRepository.licensePurchased.collect {
                _licensePurchased.value = it
            }
        }
    }

    // 以前の simulatePurchaseSuccess は confirmLicensePurchase に統合、または役割変更
    // fun simulatePurchaseSuccess() {
    //     tempRepository.purchaseLicense()
    // }

    fun confirmLicensePurchase(purchaseToken: String?) {
        if (purchaseToken == null) {
            _errorState.value = ErrorType.UNEXPECTED_ERROR // トークンがないのは予期せぬエラー
            return
        }
        viewModelScope.launch {
            try {
                // val deviceId = deviceIdRepository.getDeviceId() ?: "unknown_device_id" // 仮
                val deviceId = "test-device-id" // 仮

                // --- API呼び出し処理 (本番ではApiClient経由) ---
                println("ViewModel: Calling /license/confirm with token: $purchaseToken, deviceId: $deviceId")
                // この部分は実際のAPIクライアントの呼び出しに置き換える
                // 例: val response = apiClient.confirmLicense(deviceId, purchaseToken)
                // if (response.isSuccessful && response.body()?.status == "ok") { ... }

                // 仮のAPI呼び出しシミュレーション
                if (purchaseToken.startsWith("dummy_purchase_token")) { // "dummy_purchase_token_from_webview" など
                    tempRepository.purchaseLicense() // 成功したらリポジトリ更新
                    _errorState.value = null // エラー状態クリア
                    println("ViewModel: License purchase confirmed (simulated)")
                } else {
                    // APIエラー種別に応じて errorState を設定
                    _errorState.value = ErrorType.PAYMENT_VERIFICATION_FAILED // Stripe検証失敗などを想定
                    println("ViewModel: License purchase confirmation failed (simulated)")
                }
                // --- ここまでAPI呼び出しシミュレーション ---

            } catch (e: Exception) {
                println("ViewModel: Exception during confirmLicensePurchase: ${e.message}")
                _errorState.value = ErrorType.UNEXPECTED_ERROR // 通信エラーなど
            }
        }
    }

    fun clearErrorState() { // エラー表示後にUI側から呼び出す
        _errorState.value = null
    }
} 