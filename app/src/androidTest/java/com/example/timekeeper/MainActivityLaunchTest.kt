package com.example.timekeeper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainActivityLaunchTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assert.assertEquals("com.example.timekeeper", appContext.packageName)
    }

    @Test
    fun licenseScreen_initialDisplay_isCorrect() {
        // MainActivityが起動し、LicenseScreenが表示されることを確認
        // LicenseScreenに表示されるテキスト「¥10,000で利用開始」が表示されていることを確認
        composeTestRule.onNodeWithText("¥10,000で利用開始").assertIsDisplayed()
        // LicenseScreenに表示されるボタン「購入」が表示されていることを確認
        composeTestRule.onNodeWithText("購入").assertIsDisplayed()
    }
} 