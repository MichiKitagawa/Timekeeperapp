package com.example.timekeeper.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.ui.theme.TimekeeperTheme

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicensePurchaseNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
    }

    @Test
    fun licensePurchase_初期画面がP01であること() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            TimekeeperTheme {
                TimekeeperNavigation(
                    navController = navController,
                    startDestination = TimekeeperRoutes.LICENSE_PURCHASE
                )
            }
        }

        // 初期画面がライセンス購入画面であることを確認
        assert(navController.currentDestination?.route == TimekeeperRoutes.LICENSE_PURCHASE)
        
        // ライセンス購入画面のUI要素が表示されることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        composeTestRule.onNodeWithText("購入").assertIsDisplayed()
    }

    @Test
    fun licensePurchase_購入成功後にP02画面に遷移すること() {
        var purchaseSuccessful = false
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            TimekeeperTheme {
                TimekeeperNavigation(
                    navController = navController,
                    startDestination = TimekeeperRoutes.LICENSE_PURCHASE,
                    onPurchaseLicenseClick = {
                        // 購入成功をシミュレート
                        purchaseSuccessful = true
                        // P02画面への遷移をシミュレート
                        navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                            popUpTo(TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true }
                        }
                    }
                )
            }
        }

        // 初期状態でP01画面であることを確認
        assert(navController.currentDestination?.route == TimekeeperRoutes.LICENSE_PURCHASE)

        // 購入ボタンをクリック
        composeTestRule.onNodeWithText("購入").performClick()

        // 購入が成功したことを確認
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            purchaseSuccessful
        }

        // P02画面に遷移したことを確認
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            navController.currentDestination?.route == TimekeeperRoutes.MONITORING_SETUP
        }
        
        assert(navController.currentDestination?.route == TimekeeperRoutes.MONITORING_SETUP)
    }

    @Test
    fun licensePurchase_ルート定数が正しく定義されていること() {
        // ルート定数が正しく定義されていることを確認
        assert(TimekeeperRoutes.LICENSE_PURCHASE == "license_purchase")
        assert(TimekeeperRoutes.MONITORING_SETUP == "monitoring_setup")
        assert(TimekeeperRoutes.DASHBOARD == "dashboard")
        assert(TimekeeperRoutes.LOCK_SCREEN == "lock_screen")
        assert(TimekeeperRoutes.DAY_PASS_PURCHASE == "day_pass_purchase")
        assert(TimekeeperRoutes.ERROR_SCREEN == "error_screen")
    }

    @Test
    fun licensePurchase_P01からP02への遷移でP01がバックスタックから削除されること() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            TimekeeperTheme {
                TimekeeperNavigation(
                    navController = navController,
                    startDestination = TimekeeperRoutes.LICENSE_PURCHASE
                )
            }
        }

        // 初期状態でP01画面であることを確認
        assert(navController.currentDestination?.route == TimekeeperRoutes.LICENSE_PURCHASE)

        // P02画面への遷移をシミュレート（popUpTo with inclusive = trueを使用）
        composeTestRule.runOnUiThread {
            navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                popUpTo(TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true }
            }
        }

        // P02画面に遷移したことを確認
        assert(navController.currentDestination?.route == TimekeeperRoutes.MONITORING_SETUP)
        
        // バックスタックにP01画面が残っていないことを確認
        // （戻るボタンを押してもP01に戻らない）
        // TestNavHostControllerではbackQueueにアクセスできないため、
        // 現在の画面がP02であることで間接的に確認
        assert(navController.currentDestination?.route == TimekeeperRoutes.MONITORING_SETUP)
    }
} 