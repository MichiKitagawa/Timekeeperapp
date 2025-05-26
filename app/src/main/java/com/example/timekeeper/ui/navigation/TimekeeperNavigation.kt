package com.example.timekeeper.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.ui.screens.DashboardScreen
import com.example.timekeeper.ui.screens.DayPassPurchaseScreen
import com.example.timekeeper.ui.screens.ErrorScreen
import com.example.timekeeper.ui.screens.ErrorType
import com.example.timekeeper.ui.screens.LicenseScreen
import com.example.timekeeper.ui.screens.LockScreen
import com.example.timekeeper.ui.screens.MonitoringSetupScreen
import kotlin.math.pow

// ナビゲーションルート定義
object TimekeeperRoutes {
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
    startDestination: String = TimekeeperRoutes.LICENSE_PURCHASE,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // P01: ライセンス購入画面
        composable(TimekeeperRoutes.LICENSE_PURCHASE) {
            LicenseScreen(
                navController = navController,
                onNavigateToMonitoringSetup = {
                    navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                        popUpTo(TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true }
                    }
                }
            )
        }
        
        // P02: 監視対象設定画面
        composable(TimekeeperRoutes.MONITORING_SETUP) {
            MonitoringSetupScreen(
                onSetupComplete = {
                    // 設定完了後、ダッシュボードへ遷移
                    navController.navigate(TimekeeperRoutes.DASHBOARD) {
                        popUpTo(TimekeeperRoutes.MONITORING_SETUP) { inclusive = true }
                    }
                },
                navController = navController
            )
        }
        
        // P03: ダッシュボード
        composable(TimekeeperRoutes.DASHBOARD) {
            DashboardScreen(
                onAppLimitExceeded = { appName ->
                    // アプリの制限時間に達した場合、ロック画面へ遷移
                    navController.navigate("${TimekeeperRoutes.LOCK_SCREEN}/$appName")
                },
                navController = navController
            )
        }
        
        // P04: ロック画面
        composable("${TimekeeperRoutes.LOCK_SCREEN}/{appName}") { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            // サンプルのunlock_count（実際の実装ではSharedPreferencesから取得）
            val unlockCount = 0
            val unlockPrice = (200 * 1.2.pow(unlockCount.toDouble())).toInt()
            
            LockScreen(
                appName = appName,
                unlockPrice = unlockPrice,
                onUnlockClick = {
                    // アンロックボタン押下時、デイパス購入画面へ遷移
                    navController.navigate(TimekeeperRoutes.DAY_PASS_PURCHASE)
                },
                navController = navController
            )
        }
        
        // P05: デイパス決済画面
        composable(TimekeeperRoutes.DAY_PASS_PURCHASE) {
            // unlockCount は DayPassPurchaseScreen 内部で SharedPreferences から取得するようになったため削除
            // val unlockCount = 0 

            DayPassPurchaseScreen(
                // unlockCount = unlockCount, // 削除
                onPurchaseSuccess = { // onPurchaseClick から変更
                    // デイパス購入処理後、ダッシュボードへ戻る
                    // この時、P04とP05はスタックからクリアされる想定
                    navController.navigate(TimekeeperRoutes.DASHBOARD) {
                        // P04(LockScreen)とP05(DayPassPurchaseScreen)をスタックから削除し、P03(Dashboard)を表示
                        popUpTo(TimekeeperRoutes.LOCK_SCREEN) { inclusive = true }
                        launchSingleTop = true // Dashboardが既にスタックのトップにある場合は再作成しない
                    }
                },
                onCancelClick = {
                    // キャンセル時は前の画面に戻る (P04 LockScreen)
                    navController.popBackStack()
                },
                navController = navController
            )
        }
        
        // P06: エラー案内画面
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
                            // ライセンス購入画面へ遷移
                            navController.navigate(TimekeeperRoutes.LICENSE_PURCHASE) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        ErrorType.UNEXPECTED_ERROR -> {
                            // 前の画面に戻る（再試行）
                            navController.popBackStack()
                        }
                        ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED -> {
                            // 問い合わせ処理（実際の実装では外部アプリ起動等）
                            navController.popBackStack()
                        }
                        ErrorType.PAYMENT_VERIFICATION_FAILED -> {
                            // ライセンス購入画面へ遷移
                            navController.navigate(TimekeeperRoutes.LICENSE_PURCHASE) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    }
} 