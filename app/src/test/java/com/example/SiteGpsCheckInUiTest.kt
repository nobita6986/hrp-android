package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h915dp")
class SiteGpsCheckInUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val piaggioWaitingSiteGpsProfile = WorkerProfile(
        id = "w-piaggio-01",
        employeeCode = "NV-4321",
        fullName = "Duc Nguyen",
        cccdNumber = "034098111222",
        phone = "0987654321",
        factoryLocation = "PIAGGIO",
        projectCode = "PRJ-PIAGGIO",
        department = "Xưởng Lắp Ráp",
        position = "CTY- PIAGGIO - BÌNH XUYÊN 1",
        baseSalaryPerDayVnd = 350_000L,
        joinedDate = "20/10/2026",
        bankAccount = "",
        phoneVerified = true,
        verificationStatus = VerificationStatus.APPROVED,
        hasActiveAssignment = true,
        canCheckIn = false,
        assignmentId = "asg-piaggio-01",
        projectId = "proj-piaggio",
        projectName = "PIAGGIO",
        hasConfiguredSiteGps = false,
        siteGeofenceNotConfigured = true
    )

    @Test
    fun `HomeScreen shows CHO XAC NHAN SITE and Site GPS lock message when hasActiveAssignment is true and Site GPS is unconfirmed`() {
        composeTestRule.setContent {
            HomeScreen(
                workerProfile = piaggioWaitingSiteGpsProfile,
                attendanceList = emptyList(),
                tickets = emptyList(),
                onNavigateTab = {},
                onCheckIn = {},
                onCheckOut = {},
                onRequestAdvanceSalary = {},
                onCreateTicket = {}
            )
        }

        composeTestRule.onNodeWithText("CHỜ XÁC NHẬN SITE").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Tạm khóa chấm công: Site dự án chưa được Admin xác nhận tọa độ GPS"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Tạm khóa chấm công: Site chưa được Admin xác nhận GPS"
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText("CHƯA PHÂN CÔNG").assertDoesNotExist()
        composeTestRule.onNodeWithText(
            "Tài khoản chưa liên kết hồ sơ Worker",
            substring = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(
            "canCheckIn = false",
            substring = true
        ).assertDoesNotExist()
    }

    @Test
    fun `AttendanceScreen shows CHO XAC NHAN SITE and Site GPS lock message when hasActiveAssignment is true and Site GPS is unconfirmed`() {
        composeTestRule.setContent {
            AttendanceScreen(
                attendanceList = emptyList(),
                workerProfile = piaggioWaitingSiteGpsProfile,
                onCheckIn = {},
                onCheckOut = {},
                onDisputeAttendance = {}
            )
        }

        composeTestRule.onNodeWithText("CHỜ XÁC NHẬN SITE").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Tạm khóa Vào ca / Tan ca: Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn (SITE_GEOFENCE_NOT_CONFIGURED)."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Lịch Sử Chấm Công Thực Tế").performScrollTo()
        composeTestRule.onNodeWithText(
            "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn nên chưa thể ghi nhận chấm công."
        ).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("CHƯA MỞ QUYỀN").assertDoesNotExist()
        composeTestRule.onNodeWithText(
            "Tài khoản cần được Admin liên kết hồ sơ công nhân",
            substring = true
        ).assertDoesNotExist()
    }

    @Test
    fun `RewardAttendancePolicy 6h-12h-20h unified single button states work accurately on HomeScreen`() {
        val readyProfile = piaggioWaitingSiteGpsProfile.copy(
            canCheckIn = true,
            hasConfiguredSiteGps = true,
            siteGeofenceNotConfigured = false
        )
        val checkInTimeMs = 1_760_000_000_000L
        val hourMs = 60L * 60L * 1000L

        var currentNowMs by androidx.compose.runtime.mutableLongStateOf(checkInTimeMs + 2L * hourMs)
        var currentCheckOutMs by androidx.compose.runtime.mutableLongStateOf(0L)
        var checkInCount = 0
        var checkOutCount = 0

        composeTestRule.setContent {
            HomeScreen(
                workerProfile = readyProfile,
                attendanceList = emptyList(),
                tickets = emptyList(),
                onNavigateTab = {},
                onCheckIn = { checkInCount += 1 },
                onCheckOut = { checkOutCount += 1 },
                onRequestAdvanceSalary = {},
                onCreateTicket = {},
                lastCheckInEpochMs = checkInTimeMs,
                lastCheckOutEpochMs = currentCheckOutMs,
                nowEpochMsOverride = currentNowMs
            )
        }

        // 1. At +2h after check-in (< 6h): Unified button is disabled ("ĐANG TRONG CA")
        composeTestRule.onNodeWithTag("button_unified_attendance").assertIsNotEnabled()
        composeTestRule.onNodeWithText("ĐANG TRONG CA").assertIsDisplayed()

        // 2. At +7h after check-in (between 6h and 12h): Unified button is enabled for Check-Out ("TAN CA")
        currentNowMs = checkInTimeMs + 7L * hourMs
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("button_unified_attendance").assertIsEnabled()
        composeTestRule.onNodeWithText("TAN CA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_unified_attendance").performClick()
        org.junit.Assert.assertEquals(1, checkOutCount)
        org.junit.Assert.assertEquals(0, checkInCount)

        // 3. At +13h after check-in WITHOUT check-out (> 12h): shift not recorded, unified button re-enabled for new Check-In ("VÀO CA")
        currentNowMs = checkInTimeMs + 13L * hourMs
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("button_unified_attendance").assertIsEnabled()
        composeTestRule.onNodeWithText("VÀO CA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_unified_attendance").performClick()
        org.junit.Assert.assertEquals(1, checkInCount)

        // 4. When checked out validly at +8h, and now is +15h (< 20h): Unified button is disabled ("ĐÃ HOÀN TẤT")
        currentCheckOutMs = checkInTimeMs + 8L * hourMs
        currentNowMs = checkInTimeMs + 15L * hourMs
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("button_unified_attendance").assertIsNotEnabled()
        composeTestRule.onNodeWithText("ĐÃ HOÀN TẤT").assertIsDisplayed()

        // 5. At +20h since check-in: Unified button is re-enabled for next shift Check-In ("VÀO CA")
        currentNowMs = checkInTimeMs + 20L * hourMs
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("button_unified_attendance").assertIsEnabled()
        composeTestRule.onNodeWithText("VÀO CA").assertIsDisplayed()
    }

    @Test
    fun `Guest mode allows unauthenticated job application while gating non-job tabs behind login`() {
        var openLoginClicked = false
        var backToJobsClicked = false

        composeTestRule.setContent {
            AuthRequiredTabGate(
                tab = com.example.ui.viewmodel.NavigationTab.ATTENDANCE,
                onOpenLogin = { openLoginClicked = true },
                onBrowseJobs = { backToJobsClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Tính Năng Yêu Cầu Đăng Nhập").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mục \"Bảng công\" chỉ khả dụng sau khi đăng nhập").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gate_open_login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gate_back_to_jobs_button").assertIsDisplayed()
    }
}
