package com.example.timekeeper.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.ui.screens.AccessibilityPromptScreen
import com.example.timekeeper.ui.screens.DashboardScreen
import com.example.timekeeper.ui.screens.DayPassPurchaseScreen
import com.example.timekeeper.ui.screens.ErrorScreen
import com.example.timekeeper.ui.screens.ErrorType
import com.example.timekeeper.ui.screens.LicenseScreen
import com.example.timekeeper.ui.screens.LockScreen
import com.example.timekeeper.ui.screens.MonitoringSetupScreen
import com.example.timekeeper.ui.screens.SetupAndLicenseScreen
import kotlin.math.pow
import com.example.timekeeper.viewmodel.StripeViewModel

// ナビゲーションルート定義
object TimekeeperRoutes {
    const val SETUP_AND_LICENSE = "setup_and_license"
    const val ACCESSIBILITY_PROMPT = "accessibility_prompt"
    const val LICENSE_PURCHASE = "license_purchase"
    const val MONITORING_SETUP = "monitoring_setup"
    const val DASHBOARD = "dashboard"
    const val LOCK_SCREEN = "lock_screen"
    const val DAY_PASS_PURCHASE = "day_pass_purchase"
    const val ERROR_SCREEN = "error_screen"
}

@Composable
fun TimekeeperNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = TimekeeperRoutes.SETUP_AND_LICENSE,
    modifier: Modifier = Modifier,
    stripeViewModel: StripeViewModel,
    onPurchaseLicenseClick: () -> Unit = {},
    onPurchaseDaypassClick: (Int?) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 統合セットアップ・ライセンス購入画面
        composable(TimekeeperRoutes.SETUP_AND_LICENSE) {
            SetupAndLicenseScreen(
                stripeViewModel = stripeViewModel,
                onNavigateToMonitoringSetup = {
                    navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                        popUpTo(TimekeeperRoutes.SETUP_AND_LICENSE) { inclusive = true }
                    }
                },
                onPurchaseLicenseClick = onPurchaseLicenseClick
            )
        }
        
        // アクセシビリティサービス設定画面 (既存との互換性のために残す)
        composable(TimekeeperRoutes.ACCESSIBILITY_PROMPT) {
            AccessibilityPromptScreen(
                onAccessibilityEnabled = {
                    navController.navigate(TimekeeperRoutes.SETUP_AND_LICENSE) {
                        popUpTo(TimekeeperRoutes.ACCESSIBILITY_PROMPT) { inclusive = true }
                    }
                }
            )
        }
        
        // ライセンス購入画面 (既存との互換性のために残す)
        composable(TimekeeperRoutes.LICENSE_PURCHASE) {
            LicenseScreen(
                stripeViewModel = stripeViewModel,
                onNavigateToMonitoringSetup = {
                    navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                        popUpTo(TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true }
                    }
                },
                onPurchaseLicenseClick = onPurchaseLicenseClick
            )
        }
        
        // 監視対象設定画面
        composable(TimekeeperRoutes.MONITORING_SETUP) {
            MonitoringSetupScreen(
                onSetupComplete = {
                    navController.navigate(TimekeeperRoutes.DASHBOARD) {
                        popUpTo(TimekeeperRoutes.MONITORING_SETUP) { inclusive = true }
                    }
                }
            )
        }
        
        // ダッシュボード
        composable(TimekeeperRoutes.DASHBOARD) {
            DashboardScreen(
                navController = navController
            )
        }
        
        // ロック画面
        composable("${TimekeeperRoutes.LOCK_SCREEN}/{appName}") { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            val unlockCount = 0
            val unlockPrice = (200 * 1.2.pow(unlockCount.toDouble())).toInt()
            
            LockScreen(
                appName = appName,
                unlockPrice = unlockPrice,
                onUnlockClick = {
                    navController.navigate(TimekeeperRoutes.DAY_PASS_PURCHASE)
                },
                navController = navController
            )
        }
        
        // デイパス決済画面
        composable(TimekeeperRoutes.DAY_PASS_PURCHASE) {
            DayPassPurchaseScreen(
                stripeViewModel = stripeViewModel,
                onCancelClick = {
                    navController.popBackStack()
                },
                navController = navController,
                onPurchaseDaypassClick = onPurchaseDaypassClick
            )
        }
        
        // エラー画面
        composable("${TimekeeperRoutes.ERROR_SCREEN}/{errorType}") { backStackEntry ->
            val errorTypeString = backStackEntry.arguments?.getString("errorType") ?: "UNEXPECTED_ERROR"
            val errorType = try {
                ErrorType.valueOf(errorTypeString)
            } catch (e: IllegalArgumentException) {
                ErrorType.UNEXPECTED_ERROR
            }
            
            ErrorScreen(
                errorType = errorType,
                onActionClick = {
                    when (errorType) {
                        ErrorType.LICENSE_REQUIRED -> {
                            navController.navigate(TimekeeperRoutes.SETUP_AND_LICENSE) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        ErrorType.UNEXPECTED_ERROR -> {
                            navController.popBackStack()
                        }
                        ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED -> {
                            navController.popBackStack()
                        }
                        ErrorType.PAYMENT_VERIFICATION_FAILED -> {
                            navController.navigate(TimekeeperRoutes.SETUP_AND_LICENSE) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    }
} 