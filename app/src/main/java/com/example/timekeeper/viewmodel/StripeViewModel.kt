package com.example.timekeeper.viewmodel

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

    // このデバイスIDは、実際には SharedPreferences や UserDataStore などから取得することを想定
    // MainActivity で生成しているものをViewModelでも利用できるようにDIするか、
    // ApplicationContext経由で取得するなどの方法が考えられます。
    // ここでは仮に固定値または引数で渡されることを想定します。
    private var currentDeviceId: String = "dummy_device_id" // TODO: 適切な方法で設定する

    fun setDeviceId(deviceId: String) {
        this.currentDeviceId = deviceId
    }

    /**
     * Stripe Checkoutセッションを開始し、決済ページのURLを返します。
     * UIスレッドから呼び出されることを想定。
     */
    fun startStripeCheckout(productType: String, unlockCount: Int?) {
        viewModelScope.launch {
            _paymentUiState.value = PaymentUiState.Loading
            
            // デイパスの場合、現在のunlock_countを取得
            val actualUnlockCount = if (productType == "daypass") {
                purchaseStateManager.getDaypassUnlockCount()
            } else {
                unlockCount
            }
            
            val checkoutUrl = stripeRepository.createCheckoutSession(currentDeviceId, productType, actualUnlockCount)
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
    fun confirmStripePayment(purchaseToken: String, productType: String) {
        viewModelScope.launch {
            _paymentUiState.value = PaymentUiState.Loading
            val success = stripeRepository.confirmPayment(currentDeviceId, purchaseToken, productType)
            if (success) {
                _paymentUiState.value = PaymentUiState.Success("Payment confirmed successfully for $productType!")
                // 購入状態の更新はStripeRepository内で行われるため、ここでは追加処理は不要
            } else {
                _paymentUiState.value = PaymentUiState.Error("Failed to confirm payment for $productType.")
            }
        }
    }
} 