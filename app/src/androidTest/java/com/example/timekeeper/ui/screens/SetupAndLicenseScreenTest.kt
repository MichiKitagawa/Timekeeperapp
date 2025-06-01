package com.example.timekeeper.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.viewmodel.StripeViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class SetupAndLicenseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockStripeViewModel: StripeViewModel

    private var navigateToMonitoringSetupCount = 0
    private var purchaseClickCount = 0

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        navigateToMonitoringSetupCount = 0
        purchaseClickCount = 0
    }

    @Test
    fun setupAndLicenseScreen_UI要素が正しく表示されること() {
        // SetupAndLicenseScreenを表示
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // タイトルが表示されることを確認
        composeTestRule.onNodeWithText("Timekeeper セットアップ").assertIsDisplayed()
        
        // 説明文が表示されることを確認
        composeTestRule.onNodeWithText("アプリを使用するには、以下の設定を完了してからライセンスを購入してください。").assertIsDisplayed()
        
        // 必要な設定カードが表示されることを確認
        composeTestRule.onNodeWithText("必要な設定").assertIsDisplayed()
        
        // チェックリスト項目が表示されることを確認
        composeTestRule.onNodeWithText("アクセシビリティサービスの有効化").assertIsDisplayed()
        composeTestRule.onNodeWithText("必要な権限について理解しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("使用状況統計について理解しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("監視対象アプリの設定について理解しました").assertIsDisplayed()
        
        // 価格が表示されることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        
        // 購入ボタンが表示されることを確認
        composeTestRule.onNodeWithText("ライセンスを購入").assertIsDisplayed()
    }

    @Test
    fun setupAndLicenseScreen_初期状態で購入ボタンが無効であること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 購入ボタンが無効であることを確認
        composeTestRule.onNodeWithText("ライセンスを購入").assertIsNotEnabled()
    }

    @Test
    fun setupAndLicenseScreen_チェックボックスをタップできること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 権限理解のチェックボックスをタップ
        composeTestRule.onAllNodesWithText("必要な権限について理解しました")[0]
            .onParent()
            .onChildren()
            .filterToOne(hasClickAction())
            .performClick()
        
        // 使用状況統計理解のチェックボックスをタップ
        composeTestRule.onAllNodesWithText("使用状況統計について理解しました")[0]
            .onParent()
            .onChildren()
            .filterToOne(hasClickAction())
            .performClick()
        
        // アプリ設定理解のチェックボックスをタップ
        composeTestRule.onAllNodesWithText("監視対象アプリの設定について理解しました")[0]
            .onParent()
            .onChildren()
            .filterToOne(hasClickAction())
            .performClick()
    }

    @Test
    fun setupAndLicenseScreen_アクセシビリティ設定ボタンが表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // アクセシビリティ設定を開くボタンが表示されることを確認
        composeTestRule.onNodeWithText("設定を開く").assertIsDisplayed()
    }

    @Test
    fun setupAndLicenseScreen_設定状況更新ボタンが表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 設定状況を更新ボタンが表示されることを確認（アクセシビリティが無効の場合）
        composeTestRule.onNodeWithText("設定状況を更新").assertIsDisplayed()
    }

    @Test
    fun setupAndLicenseScreen_完了状況カードが表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                SetupAndLicenseScreen(
                    stripeViewModel = mockStripeViewModel,
                    onNavigateToMonitoringSetup = { navigateToMonitoringSetupCount++ },
                    onPurchaseLicenseClick = { purchaseClickCount++ }
                )
            }
        }

        // 初期状態では未完了メッセージが表示される
        composeTestRule.onNodeWithText("上記の項目をすべて完了してください").assertIsDisplayed()
    }
} 