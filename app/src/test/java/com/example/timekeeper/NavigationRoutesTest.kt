package com.example.timekeeper

import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import org.junit.Test
import org.junit.Assert.*

class NavigationRoutesTest {

    @Test
    fun testRouteConstants() {
        assertEquals("license_purchase", TimekeeperRoutes.LICENSE_PURCHASE)
        assertEquals("monitoring_setup", TimekeeperRoutes.MONITORING_SETUP)
        assertEquals("dashboard", TimekeeperRoutes.DASHBOARD)
        assertEquals("lock_screen", TimekeeperRoutes.LOCK_SCREEN)
        assertEquals("day_pass_purchase", TimekeeperRoutes.DAY_PASS_PURCHASE)
        assertEquals("error_screen", TimekeeperRoutes.ERROR_SCREEN)
    }

    @Test
    fun testRouteUniqueness() {
        val routes = listOf(
            TimekeeperRoutes.LICENSE_PURCHASE,
            TimekeeperRoutes.MONITORING_SETUP,
            TimekeeperRoutes.DASHBOARD,
            TimekeeperRoutes.LOCK_SCREEN,
            TimekeeperRoutes.DAY_PASS_PURCHASE,
            TimekeeperRoutes.ERROR_SCREEN
        )
        
        // すべてのルートが一意であることを確認
        assertEquals(routes.size, routes.toSet().size)
    }
} 