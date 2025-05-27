package com.example.timekeeper.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.ui.theme.TimekeeperTheme

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicenseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockNavController: NavController
    private var purchaseClickCount = 0
    private var navigateToMonitoringSetupCount = 0

    @Before
    fun setup() {
        purchaseClickCount = 0
        navigateToMonitoringSetupCount = 0
    }

    @Test
    fun licenseScreen_UI要素が正しく表示されること() {
        // LicenseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicenseScreen(
                    navController = mockNavController,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // UI要素が表示されることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        composeTestRule.onNodeWithText("購入").assertIsDisplayed()
    }

    @Test
    fun licenseScreen_購入ボタンをクリックするとonPurchaseLicenseClickが呼ばれること() {
        // LicenseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicenseScreen(
                    navController = mockNavController,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 購入ボタンをクリック
        composeTestRule.onNodeWithText("購入").performClick()

        // コールバックが呼ばれることを確認
        assert(purchaseClickCount == 1)
    }

    @Test
    fun licenseScreen_購入ボタンが有効であること() {
        // LicenseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicenseScreen(
                    navController = mockNavController,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 購入ボタンが有効であることを確認
        composeTestRule.onNodeWithText("購入").assertIsEnabled()
    }

    @Test
    fun licenseScreen_レイアウトが正しく配置されていること() {
        // LicenseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicenseScreen(
                    navController = mockNavController,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // テキストと購入ボタンが表示されていることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("購入")
            .assertIsDisplayed()
    }
} 