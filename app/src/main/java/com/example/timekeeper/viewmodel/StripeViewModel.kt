package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.StripeRepository
import com.example.timekeeper.data.PurchaseStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UIに公開する決済状態
sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Loading : PaymentUiState()
    data class Success(val message: String) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

@HiltViewModel
class StripeViewModel @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val purchaseStateManager: PurchaseStateManager
) : ViewModel() {

    private val _paymentUiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentUiState: StateFlow<PaymentUiState> = _paymentUiState

    /**
     * Stripe Checkoutセッションを開始し、決済ページのURLを返します。
     * UIスレッドから呼び出されることを想定。
     */
    fun startStripeCheckout(deviceId: String, productType: String, unlockCount: Int?) {
        viewModelScope.launch {
            _paymentUiState.value = PaymentUiState.Loading
            Log.d("StripeViewModel", "startStripeCheckout called with deviceId: $deviceId, productType: $productType, unlockCount: $unlockCount")
            
            // デイパスの場合、UI側から渡されたunlock_countを使用
            // （UI側で現在の購入回数を正しく取得して渡している前提）
            val actualUnlockCount = if (productType == "daypass") {
                val passedCount = unlockCount ?: 0
                Log.d("StripeViewModel", "Daypass: Using unlock count from UI: $passedCount")
                passedCount
            } else {
                unlockCount
            }
            
            Log.d("StripeViewModel", "Final unlock count to be sent to server: $actualUnlockCount")
            
            val checkoutUrl = stripeRepository.createCheckoutSession(deviceId, productType, actualUnlockCount)
            if (checkoutUrl != null) {
                // Activity/Fragment側でWebViewを開くためにURLを通知する
                // ここでは例としてStateFlowでURLを公開するが、
                // SingleLiveEventやSharedFlowなど、一度きりのイベント通知に適した方法も検討
                _checkoutUrlFlow.value = checkoutUrl
                // _paymentUiState.value = PaymentUiState.Success("Checkout URL ready.") // これは次のステップ
            } else {
                _paymentUiState.value = PaymentUiState.Error("Failed to create checkout session.")
            }
        }
    }

    // Checkout URLをActivityに通知するためのFlow (例)
    private val _checkoutUrlFlow = MutableStateFlow<String?>(null)
    val checkoutUrlFlow: StateFlow<String?> = _checkoutUrlFlow

    fun consumeCheckoutUrl() {
        _checkoutUrlFlow.value = null
        _paymentUiState.value = PaymentUiState.Idle // URL消費後は一旦Idleに
    }


    /**
     * Stripeからのリダイレクト後、決済を最終確認します。
     * (ディープリンク経由でActivityから呼び出されることを想定)
     */
    fun confirmStripePayment(deviceId: String, purchaseToken: String, productType: String) {
        viewModelScope.launch {
            _paymentUiState.value = PaymentUiState.Loading
            Log.d("StripeViewModel", "confirmStripePayment called with deviceId: $deviceId, token: $purchaseToken, productType: $productType")
            val success = stripeRepository.confirmPayment(deviceId, purchaseToken, productType)
            if (success) {
                _paymentUiState.value = PaymentUiState.Success("Payment confirmed successfully for $productType!")
                Log.i("StripeViewModel", "Payment confirmation successful for $productType")
                // 購入状態の更新はStripeRepository内で行われるため、ここでは追加処理は不要
            } else {
                _paymentUiState.value = PaymentUiState.Error("Failed to confirm payment for $productType.")
                Log.e("StripeViewModel", "Payment confirmation failed for $productType")
            }
        }
    }

    /**
     * 決済状態をリセットします（処理完了後にUIから呼び出される想定）
     */
    fun resetPaymentState() {
        _paymentUiState.value = PaymentUiState.Idle
        Log.d("StripeViewModel", "Payment state reset to Idle")
    }
} 