package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
