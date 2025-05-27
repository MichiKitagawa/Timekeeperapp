package com.example.timekeeper.ui.viewmodels

import com.example.timekeeper.ui.screens.ErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LicenseViewModelTest {

    private lateinit var viewModel: LicenseViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // テスト前にリポジトリをリセット
        TempLicenseRepository.reset()
        viewModel = LicenseViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期状態でlicense_purchasedがfalseであること`() = runTest {
        // 初期状態の確認
        assertEquals(false, viewModel.licensePurchased.first())
        assertEquals(null, viewModel.errorState.first())
    }

    @Test
    fun `有効なpurchaseTokenでconfirmLicensePurchaseを呼び出すとlicense_purchasedがtrueになること`() = runTest {
        // 有効なトークンで購入確認を実行
        viewModel.confirmLicensePurchase("dummy_purchase_token_valid")
        
        // コルーチンの実行を待つ
        testScheduler.advanceUntilIdle()
        
        // ライセンス購入状態が更新されることを確認
        assertEquals(true, viewModel.licensePurchased.first())
        assertEquals(null, viewModel.errorState.first())
    }

    @Test
    fun `無効なpurchaseTokenでconfirmLicensePurchaseを呼び出すとエラー状態になること`() = runTest {
        // 無効なトークンで購入確認を実行
        viewModel.confirmLicensePurchase("invalid_token")
        
        // コルーチンの実行を待つ
        testScheduler.advanceUntilIdle()
        
        // エラー状態が設定されることを確認
        assertEquals(false, viewModel.licensePurchased.first())
        assertEquals(ErrorType.PAYMENT_VERIFICATION_FAILED, viewModel.errorState.first())
    }

    @Test
    fun `nullのpurchaseTokenでconfirmLicensePurchaseを呼び出すとUNEXPECTED_ERRORになること`() = runTest {
        // nullトークンで購入確認を実行
        viewModel.confirmLicensePurchase(null)
        
        // コルーチンの実行を待つ
        testScheduler.advanceUntilIdle()
        
        // 予期しないエラー状態が設定されることを確認
        assertEquals(false, viewModel.licensePurchased.first())
        assertEquals(ErrorType.UNEXPECTED_ERROR, viewModel.errorState.first())
    }

    @Test
    fun `clearErrorStateを呼び出すとエラー状態がクリアされること`() = runTest {
        // まずエラー状態を作る
        viewModel.confirmLicensePurchase("invalid_token")
        testScheduler.advanceUntilIdle()
        
        // エラー状態が設定されていることを確認
        assertEquals(ErrorType.PAYMENT_VERIFICATION_FAILED, viewModel.errorState.first())
        
        // エラー状態をクリア
        viewModel.clearErrorState()
        
        // エラー状態がクリアされることを確認
        assertEquals(null, viewModel.errorState.first())
    }

    @Test
    fun `TempLicenseRepositoryのreset機能が正常に動作すること`() = runTest {
        // ライセンスを購入状態にする
        TempLicenseRepository.purchaseLicense()
        assertEquals(true, TempLicenseRepository.licensePurchased.first())
        
        // リセットする
        TempLicenseRepository.reset()
        assertEquals(false, TempLicenseRepository.licensePurchased.first())
    }
} 