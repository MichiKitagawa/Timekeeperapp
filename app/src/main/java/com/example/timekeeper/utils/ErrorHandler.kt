package com.example.timekeeper.utils

import androidx.navigation.NavController
import com.example.timekeeper.data.api.ApiResponse
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import com.example.timekeeper.ui.screens.ErrorType
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 汎用エラーハンドリングユーティリティ
 */
object ErrorHandler {
    
    /**
     * APIレスポンスのエラーを処理し、適切なエラー画面に遷移する
     * 
     * @param navController ナビゲーションコントローラー
     * @param apiResponse APIレスポンス
     * @param onLicenseRequired ライセンス未購入エラー時のコールバック
     */
    fun <T> handleApiError(
        navController: NavController,
        apiResponse: ApiResponse<T>,
        onLicenseRequired: (() -> Unit)? = null
    ) {
        when (apiResponse) {
            is ApiResponse.Error -> {
                when (apiResponse.error) {
                    "device_not_found" -> {
                        // ライセンス未購入エラー
                        onLicenseRequired?.invoke() ?: run {
                            navigateToErrorScreen(navController, ErrorType.LICENSE_REQUIRED)
                        }
                    }
                    "invalid_purchase_token",
                    "stripe_verification_failed",
                    "payment_verification_failed" -> {
                        // 決済関連エラー
                        navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
                    }
                    "invalid_request" -> {
                        // リクエストエラー
                        navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
                    }
                    else -> {
                        // その他のAPIエラー
                        navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
                    }
                }
            }
            is ApiResponse.Exception -> {
                handleException(navController, apiResponse.exception)
            }
            else -> {
                // 成功時は何もしない
            }
        }
    }
    
    /**
     * 例外を処理し、適切なエラー画面に遷移する
     * 
     * @param navController ナビゲーションコントローラー
     * @param exception 発生した例外
     */
    fun handleException(
        navController: NavController,
        exception: Throwable
    ) {
        when (exception) {
            is JsonSyntaxException -> {
                // JSONパースエラー
                navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
            }
            is HttpException -> {
                // HTTPエラー
                when (exception.code()) {
                    404 -> {
                        // デバイス未登録エラー
                        navigateToErrorScreen(navController, ErrorType.LICENSE_REQUIRED)
                    }
                    else -> {
                        navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
                    }
                }
            }
            is SocketTimeoutException,
            is IOException -> {
                // ネットワークエラー
                navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
            }
            else -> {
                // その他の予期せぬエラー
                navigateToErrorScreen(navController, ErrorType.UNEXPECTED_ERROR)
            }
        }
    }
    
    /**
     * Stripe決済成功後のFirestore更新失敗エラーを処理する
     * 
     * @param navController ナビゲーションコントローラー
     */
    fun handlePaymentSuccessButUnlockFailed(navController: NavController) {
        navigateToErrorScreen(navController, ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED)
    }
    
    /**
     * エラー画面に遷移する
     * 
     * @param navController ナビゲーションコントローラー
     * @param errorType エラータイプ
     */
    private fun navigateToErrorScreen(
        navController: NavController,
        errorType: ErrorType
    ) {
        navController.navigate("${TimekeeperRoutes.ERROR_SCREEN}/${errorType.name}") {
            // エラー画面は現在の画面の上に表示し、バックスタックは保持
            launchSingleTop = true
        }
    }
} 