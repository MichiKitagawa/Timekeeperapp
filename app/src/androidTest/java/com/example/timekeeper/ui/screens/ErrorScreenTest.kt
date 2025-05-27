package com.example.timekeeper.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.ui.theme.TimekeeperTheme

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var actionClickCount = 0

    @Before
    fun setup() {
        actionClickCount = 0
    }

    @Test
    fun errorScreen_LICENSE_REQUIRED_エラーが正しく表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.LICENSE_REQUIRED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // エラータイトルが表示されることを確認
        composeTestRule.onNodeWithText("エラー").assertIsDisplayed()
        
        // エラーメッセージが表示されることを確認
        composeTestRule.onNodeWithText("ライセンス購入が必要です").assertIsDisplayed()
        
        // ボタンテキストが正しいことを確認
        composeTestRule.onNodeWithText("購入画面へ").assertIsDisplayed()
    }

    @Test
    fun errorScreen_UNEXPECTED_ERROR_エラーが正しく表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.UNEXPECTED_ERROR,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // エラーメッセージが表示されることを確認
        composeTestRule.onNodeWithText("予期しないエラーが発生しました").assertIsDisplayed()
        
        // ボタンテキストが正しいことを確認
        composeTestRule.onNodeWithText("再試行").assertIsDisplayed()
    }

    @Test
    fun errorScreen_PAYMENT_SUCCESS_BUT_UNLOCK_FAILED_エラーが正しく表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // エラーメッセージが表示されることを確認
        composeTestRule.onNodeWithText("課金は成功しましたが、ロック解除に失敗しました").assertIsDisplayed()
        
        // ボタンテキストが正しいことを確認
        composeTestRule.onNodeWithText("問い合わせる").assertIsDisplayed()
    }

    @Test
    fun errorScreen_PAYMENT_VERIFICATION_FAILED_エラーが正しく表示されること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.PAYMENT_VERIFICATION_FAILED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // エラーメッセージが表示されることを確認
        composeTestRule.onNodeWithText("決済の検証に失敗しました。").assertIsDisplayed()
        
        // ボタンテキストが正しいことを確認
        composeTestRule.onNodeWithText("やり直す").assertIsDisplayed()
    }

    @Test
    fun errorScreen_アクションボタンをクリックするとコールバックが呼ばれること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.UNEXPECTED_ERROR,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // アクションボタンをクリック
        composeTestRule.onNodeWithText("再試行").performClick()

        // コールバックが呼ばれることを確認
        assert(actionClickCount == 1)
    }

    @Test
    fun errorScreen_LICENSE_REQUIRED_ボタンが有効であること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.LICENSE_REQUIRED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }
        composeTestRule.onNodeWithText("購入画面へ").assertIsEnabled()
    }

    @Test
    fun errorScreen_UNEXPECTED_ERROR_ボタンが有効であること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.UNEXPECTED_ERROR,
                    onActionClick = { actionClickCount++ }
                )
            }
        }
        composeTestRule.onNodeWithText("再試行").assertIsEnabled()
    }

    @Test
    fun errorScreen_PAYMENT_SUCCESS_BUT_UNLOCK_FAILED_ボタンが有効であること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }
        composeTestRule.onNodeWithText("問い合わせる").assertIsEnabled()
    }

    @Test
    fun errorScreen_PAYMENT_VERIFICATION_FAILED_ボタンが有効であること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.PAYMENT_VERIFICATION_FAILED,
                    onActionClick = { actionClickCount++ }
                )
            }
        }
        composeTestRule.onNodeWithText("やり直す").assertIsEnabled()
    }

    @Test
    fun errorScreen_レイアウトが正しく配置されていること() {
        composeTestRule.setContent {
            TimekeeperTheme {
                ErrorScreen(
                    errorType = ErrorType.UNEXPECTED_ERROR,
                    onActionClick = { actionClickCount++ }
                )
            }
        }

        // エラータイトル、メッセージ、ボタンが表示されていることを確認
        composeTestRule.onNodeWithText("エラー").assertIsDisplayed()
        composeTestRule.onNodeWithText("予期しないエラーが発生しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("再試行").assertIsDisplayed()
        
        // ボタンがフルワイドスであることを確認
        composeTestRule.onNodeWithText("再試行").assertWidthIsAtLeast(200.dp)
    }
} 