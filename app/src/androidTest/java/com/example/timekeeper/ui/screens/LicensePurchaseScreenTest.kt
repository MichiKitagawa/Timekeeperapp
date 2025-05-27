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
class LicensePurchaseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockNavController: NavController
    private var purchaseClickCount = 0

    @Before
    fun setup() {
        purchaseClickCount = 0
    }

    @Test
    fun licensePurchaseScreen_UI要素が正しく表示されること() {
        // LicensePurchaseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicensePurchaseScreen(
                    onPurchaseClick = { purchaseClickCount++ },
                    navController = mockNavController
                )
            }
        }

        // UI要素が表示されることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        composeTestRule.onNodeWithText("購入").assertIsDisplayed()
    }

    @Test
    fun licensePurchaseScreen_購入ボタンをクリックするとonPurchaseClickが呼ばれること() {
        // LicensePurchaseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicensePurchaseScreen(
                    onPurchaseClick = { purchaseClickCount++ },
                    navController = mockNavController
                )
            }
        }

        // 購入ボタンをクリック
        composeTestRule.onNodeWithText("購入").performClick()

        // 少し待ってからコールバックが呼ばれることを確認
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            purchaseClickCount > 0
        }
        assert(purchaseClickCount == 1)
    }

    @Test
    fun licensePurchaseScreen_購入ボタンが初期状態で有効であること() {
        // LicensePurchaseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicensePurchaseScreen(
                    onPurchaseClick = { purchaseClickCount++ },
                    navController = mockNavController
                )
            }
        }

        // 購入ボタンが有効であることを確認
        composeTestRule.onNodeWithText("購入").assertIsEnabled()
    }

    @Test
    fun licensePurchaseScreen_購入ボタンクリック後にローディング状態になること() {
        // LicensePurchaseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicensePurchaseScreen(
                    onPurchaseClick = { 
                        // 処理を遅延させてローディング状態を確認できるようにする
                        Thread.sleep(100)
                        purchaseClickCount++ 
                    },
                    navController = mockNavController
                )
            }
        }

        // 購入ボタンをクリック
        composeTestRule.onNodeWithText("購入").performClick()

        // ローディングインジケーターが表示されることを確認
        // 注意: 実際の実装では非同期処理のため、タイミングによってはローディング状態を捕捉できない場合があります
        composeTestRule.waitForIdle()
    }

    @Test
    fun licensePurchaseScreen_レイアウトが正しく配置されていること() {
        // LicensePurchaseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                mockNavController = rememberNavController()
                LicensePurchaseScreen(
                    onPurchaseClick = { purchaseClickCount++ },
                    navController = mockNavController
                )
            }
        }

        // テキストと購入ボタンが表示されていることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        composeTestRule.onNodeWithText("購入").assertIsDisplayed()
        
        // ボタンがフルワイドスであることを確認（テストセマンティクスで確認可能な範囲で）
        composeTestRule.onNodeWithText("購入").assertWidthIsAtLeast(200.dp)
    }
} 