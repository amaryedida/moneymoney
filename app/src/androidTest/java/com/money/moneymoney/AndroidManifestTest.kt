package com.money.moneymoney

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidManifestTest {

    @Test
    fun testDashboardActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        assertNotNull(intent)
        assertEquals(DashboardActivity::class.java.name, intent?.component?.className)

        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, DashboardActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertTrue(activityInfo.exported)
            assertEquals(1, activityInfo.launchMode) // LAUNCH_SINGLE_TOP
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)

            val intentFilter = activityInfo.intentFilters?.find {
                it.hasAction(Intent.ACTION_MAIN) && it.hasCategory(Intent.CATEGORY_LAUNCHER)
            }
            assertNotNull(intentFilter)
        } catch (e: PackageManager.NameNotFoundException) {
            fail("DashboardActivity not found in manifest")
        }
    }

    @Test
    fun testExpenseEntryActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, ExpenseEntryActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertFalse(activityInfo.exported)
            assertEquals("Add Expense", activityInfo.loadLabel(packageManager))
            assertEquals(DashboardActivity::class.java.name, activityInfo.parentActivityName)
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)
            val metaData = activityInfo.metaData
            assertEquals(DashboardActivity::class.java.name, metaData.getString("android.support.PARENT_ACTIVITY"))
        } catch (e: PackageManager.NameNotFoundException) {
            fail("ExpenseEntryActivity not found in manifest")
        }
    }

    @Test
    fun testGoalEntryActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, GoalEntryActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertFalse(activityInfo.exported)
            assertEquals("Add Goal", activityInfo.loadLabel(packageManager))
            assertEquals(DashboardActivity::class.java.name, activityInfo.parentActivityName)
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)
            val metaData = activityInfo.metaData
            assertEquals(DashboardActivity::class.java.name, metaData.getString("android.support.PARENT_ACTIVITY"))
        } catch (e: PackageManager.NameNotFoundException) {
            fail("GoalEntryActivity not found in manifest")
        }
    }

    @Test
    fun testCurrencyReportsActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, CurrencyReportsActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertFalse(activityInfo.exported)
            assertEquals("Reports", activityInfo.loadLabel(packageManager))
            assertEquals(DashboardActivity::class.java.name, activityInfo.parentActivityName)
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)
            val metaData = activityInfo.metaData
            assertEquals(DashboardActivity::class.java.name, metaData.getString("android.support.PARENT_ACTIVITY"))
        } catch (e: PackageManager.NameNotFoundException) {
            fail("CurrencyReportsActivity not found in manifest")
        }
    }

    @Test
    fun testIncomeEntryActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, IncomeEntryActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertFalse(activityInfo.exported)
            assertEquals("Add Income", activityInfo.loadLabel(packageManager))
            assertEquals(DashboardActivity::class.java.name, activityInfo.parentActivityName)
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)
            val metaData = activityInfo.metaData
            assertEquals(DashboardActivity::class.java.name, metaData.getString("android.support.PARENT_ACTIVITY"))
        } catch (e: PackageManager.NameNotFoundException) {
            fail("IncomeEntryActivity not found in manifest")
        }
    }

    @Test
    fun testInvestmentEntryActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        try {
            val activityInfo = packageManager.getActivityInfo(
                ComponentName(context.packageName, InvestmentEntryActivity::class.java.name),
                PackageManager.GET_META_DATA
            )
            assertFalse(activityInfo.exported)
            assertEquals("Add Investment", activityInfo.loadLabel(packageManager))
            assertEquals(DashboardActivity::class.java.name, activityInfo.parentActivityName)
            assertEquals(R.style.Theme_MoneyMoney, activityInfo.theme)
            val metaData = activityInfo.metaData
            assertEquals(DashboardActivity::class.java.name, metaData.getString("android.support.PARENT_ACTIVITY"))
        } catch (e: PackageManager.NameNotFoundException) {
            fail("InvestmentEntryActivity not found in manifest")
        }
    }
}