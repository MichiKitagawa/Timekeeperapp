package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.service.MyAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class DayPassPurchaseViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DayPassPurchaseViewModel"
    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    /**
     * ローカル環境でのテスト用：モック決済処理
     */
    fun purchaseDayPassMock() {
        viewModelScope.launch {
            try {
                _purchaseState.value = PurchaseState.Loading
                Log.i(TAG, "Starting mock day pass purchase")
                
                // 2秒の遅延でリアルな決済感を演出
                delay(2000)
                
                // 全ての監視対象アプリにデイパスを適用
                appUsageRepository.purchaseDayPassForAllApps()
                Log.i(TAG, "Day pass applied to all monitored apps")
                
                // アクセシビリティサービスにブロック解除を通知
                try {
                    val serviceInstance = MyAccessibilityService.getInstance()
                    if (serviceInstance != null) {
                        serviceInstance.onDayPassPurchasedForAllApps()
                        Log.i(TAG, "Successfully notified accessibility service about day pass purchase")
                    } else {
                        Log.w(TAG, "Accessibility service instance not available")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify accessibility service", e)
                    // アクセシビリティサービスの通知に失敗しても、デイパス購入は成功とする
                }
                
                _purchaseState.value = PurchaseState.Success
                Log.i(TAG, "Mock day pass purchase completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to purchase day pass", e)
                _purchaseState.value = PurchaseState.Error("デイパスの購入に失敗しました: ${e.message}")
            }
        }
    }

    /**
     * 購入状態をリセット
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
} 