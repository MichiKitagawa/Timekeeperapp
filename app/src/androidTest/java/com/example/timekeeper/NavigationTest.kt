package com.example.timekeeper

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.ui.navigation.TimekeeperNavigation
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import com.example.timekeeper.ui.theme.TimekeeperTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            TimekeeperTheme {
                TimekeeperNavigation(navController = navController)
            }
        }
    }

    @Test
    fun navHost_verifyStartDestination() {
        composeTestRule
            .onNodeWithText("¥10,000で利用開始")
            .assertIsDisplayed()
    }

    @Test
    fun navHost_clickPurchaseButton_navigatesToMonitoringSetup() {
        composeTestRule
            .onNodeWithText("購入")
            .performClick()

        composeTestRule
            .onNodeWithText("監視対象設定")
            .assertIsDisplayed()
    }

    @Test
    fun navHost_verifyAllScreensExist() {
        // P01: ライセンス購入画面
        composeTestRule
            .onNodeWithText("¥10,000で利用開始")
            .assertIsDisplayed()

        // P02: 監視対象設定画面へ遷移
        composeTestRule
            .onNodeWithText("購入")
            .performClick()

        composeTestRule
            .onNodeWithText("監視対象設定")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("アプリを選択")
            .assertIsDisplayed()
    }
} 