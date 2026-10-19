package com.example

import com.example.data.api.AuthInterceptor
import com.example.data.api.model.ApiError
import com.example.data.api.model.ApiErrorParser
import com.example.data.api.model.AttendanceDayResponse
import com.example.data.api.model.FeedbackItemDto
import com.example.data.api.model.JobOpeningResponse
import com.example.data.api.model.ReferralProgramDto
import com.example.data.api.model.ReferralProgramWrapper
import com.example.data.api.model.TicketItemResponse
import com.example.data.api.model.WorkerProfileResponse
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceStatus
import com.example.data.model.FeedbackCategory
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.model.VerificationStatus
import com.example.data.repository.HrpFormatters
import com.example.ui.viewmodel.NavigationTab
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HrpNetworkAndAuthTest {

    @Test
    fun `test NavigationTab has strictly 5 worker tabs in contract order`() {
        val tabs = NavigationTab.entries
        assertEquals(5, tabs.size)
        assertEquals(NavigationTab.HOME, tabs[0])
        assertEquals(NavigationTab.JOBS, tabs[1])
        assertEquals(NavigationTab.ATTENDANCE, tabs[2])
        assertEquals(NavigationTab.FEEDBACK, tabs[3])
        assertEquals(NavigationTab.REFERRAL, tabs[4])
    }

    @Test
    fun `test formatVnd formats currency properly with Vietnamese locale`() {
        val formatted = HrpFormatters.formatVnd(2_500_000L)
        assertTrue(formatted.contains("2.500.000") || formatted.contains("2,500,000"))
    }

    @Test
    fun `test formatVndShort formats billions and millions correctly`() {
        val shortBillion = HrpFormatters.formatVndShort(21_850_000_000L)
        assertTrue(shortBillion.contains("21.85 Tỷ đ"))

        val shortMillion = HrpFormatters.formatVndShort(9_600_000L)
        assertTrue(shortMillion.contains("9.6 Tr đ"))
    }

    @Test
    fun `test WorkerProfileResponse converts cleanly to domain model including verification fields`() {
        val dto = WorkerProfileResponse(
            id = "w-101",
            employeeCode = "HRP-9999",
            fullName = "Lê Văn Cường",
            cccdNumber = "034098111222",
            phone = "0987654321",
            factoryLocation = "Samsung SEVT",
            projectCode = "PRJ-01",
            department = "Xưởng Linh Kiện",
            position = "Công nhân",
            baseSalaryPerDayVnd = 450_000L,
            joinedDate = "01/01/2024",
            bankAccount = "Vietcombank - 123456",
            phoneVerified = true,
            verificationStatus = "APPROVED",
            hasActiveAssignment = true
        )
        val domain = dto.toDomain()
        assertEquals("w-101", domain.id)
        assertEquals("HRP-9999", domain.employeeCode)
        assertEquals("Lê Văn Cường", domain.fullName)
        assertEquals(450_000L, domain.baseSalaryPerDayVnd)
        assertTrue(domain.phoneVerified)
        assertEquals(VerificationStatus.APPROVED, domain.verificationStatus)
        assertTrue(domain.hasActiveAssignment)
    }

    @Test
    fun `test ApiErrorParser maps contract error codes accurately`() {
        val otpErr = ApiErrorParser.fromApiError(ApiError(code = "OTP_SERVICE_UNAVAILABLE", message = ""), 503)
        assertEquals("OTP_SERVICE_UNAVAILABLE", otpErr.code)
        assertTrue(otpErr.message.contains("bảo trì"))

        val geofenceErr = ApiErrorParser.fromApiError(ApiError(code = "GEOFENCE_OUT_OF_RANGE", message = ""), 422)
        assertEquals("GEOFENCE_OUT_OF_RANGE", geofenceErr.code)
        assertTrue(geofenceErr.message.contains("ngoài bán kính"))

        val refDisabledErr = ApiErrorParser.fromApiError(ApiError(code = "REFERRAL_PROGRAM_DISABLED", message = ""), 422)
        assertEquals("REFERRAL_PROGRAM_DISABLED", refDisabledErr.code)
        assertTrue(refDisabledErr.message.contains("chưa mở"))
    }

    @Test
    fun `test ReferralProgramWrapper resolves disabled and enabled programs dynamically`() {
        val disabledWrapper = ReferralProgramWrapper(
            program = ReferralProgramDto(
                isEnabled = false,
                canSubmit = false,
                blockedReason = "Chính sách chưa được công bố"
            )
        )
        val disabledDomain = disabledWrapper.resolvedProgram()
        assertFalse(disabledDomain.isEnabled)
        assertFalse(disabledDomain.canSubmit)
        assertEquals("Chính sách chưa được công bố", disabledDomain.blockedReason)

        val enabledWrapper = ReferralProgramWrapper(
            isEnabled = true,
            policyVersion = "v1.2",
            title = "Thưởng Giới Thiệu Đồng Đội",
            rewardPolicyDescription = "Theo quy định công bố của Admin HRP",
            canSubmit = true
        )
        val enabledDomain = enabledWrapper.resolvedProgram()
        assertTrue(enabledDomain.isEnabled)
        assertTrue(enabledDomain.canSubmit)
        assertEquals("v1.2", enabledDomain.policyVersion)
    }

    @Test
    fun `test FeedbackItemDto maps category and ticket item`() {
        val fbDto = FeedbackItemDto(
            id = "fb-123456",
            category = "SALARY_BENEFITS",
            content = "Cần kiểm tra phụ cấp chuyên cần",
            status = "RESOLVED",
            adminResponse = "Đã cập nhật vào kỳ lương tháng 10",
            createdAt = "2026-10-19"
        )
        val fbDomain = fbDto.toDomain()
        assertEquals(FeedbackCategory.SALARY_BENEFITS, fbDomain.category)
        assertEquals("RESOLVED", fbDomain.status)

        val ticketItem = fbDto.toTicketItem()
        assertEquals(TicketStatus.APPROVED, ticketItem.status)
        assertEquals("Đã cập nhật vào kỳ lương tháng 10", ticketItem.hrNote)
    }

    @Test
    fun `test JobOpeningResponse resolves slug and salary display`() {
        val jobDto = JobOpeningResponse(
            id = "job-1",
            slug = "cong-nhan-lap-rap-sevt",
            title = "Công Nhân Lắp Ráp",
            companyName = "Samsung SEVT",
            salaryMinVnd = 9_000_000L,
            salaryMaxVnd = 14_000_000L
        )
        val domain = jobDto.toDomain()
        assertEquals("cong-nhan-lap-rap-sevt", domain.slug)
        assertEquals("9 - 14 Triệu đ/tháng", domain.salaryRange)
    }

    @Test
    fun `test TicketItemResponse converts ticket status and type appropriately`() {
        val dto = TicketItemResponse(
            id = "tk-1",
            code = "TK-ADV-01",
            type = "ADVANCE_SALARY",
            title = "Tạm ứng",
            description = "Chi phí sinh hoạt",
            amountVnd = 2_000_000L,
            createdDate = "18/10/2026",
            status = "HR_APPROVED",
            requesterName = "Nguyễn Văn Hùng",
            requesterCode = "HRP-8839"
        )
        val domain = dto.toDomain()
        assertEquals(TicketType.ADVANCE_SALARY, domain.type)
        assertEquals(TicketStatus.HR_APPROVED, domain.status)
        assertEquals(2_000_000L, domain.amountVnd)
    }

    @Test
    fun `test AttendanceDayResponse maps status and geofenceResult correctly`() {
        val dto = AttendanceDayResponse(
            id = "att-1",
            workDate = "2026-10-19",
            dayOfWeek = "Thứ Hai",
            shiftName = "Ca Sáng",
            checkInTime = "07:55",
            checkOutTime = "17:05",
            standardHours = 8.0,
            otHours = 2.0,
            geofenceResult = "INSIDE",
            factorySite = "Nhà máy HRP",
            status = "APPROVED",
            note = "Đủ công"
        )
        val domain = dto.toDomain()
        assertEquals(AttendanceStatus.APPROVED, domain.status)
        assertEquals("2026-10-19", domain.dateString)
        assertTrue(domain.locationVerified)
        assertEquals(8.0, domain.standardHours, 0.001)
        assertEquals(2.0, domain.otHours, 0.001)
    }

    @Test
    fun `test AuthInterceptor attaches Bearer token when token is present`() {
        var interceptedRequest: Request? = null

        val fakeChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder()
                .url("https://vieclammienbac.com.vn/api/workers/me")
                .build()

            override fun proceed(request: Request): Response {
                interceptedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build()
            }

            override fun connection(): Connection? = null
            override fun call(): okhttp3.Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 1000
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 1000
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val fakeTokenManager = object : TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
            override fun getToken(): String = "test_bearer_jwt_token_456"
        }

        val interceptor = AuthInterceptor(fakeTokenManager)
        interceptor.intercept(fakeChain)

        assertEquals("Bearer test_bearer_jwt_token_456", interceptedRequest?.header("Authorization"))
        assertEquals("hrp_session=test_bearer_jwt_token_456", interceptedRequest?.header("Cookie"))
        assertEquals("application/json", interceptedRequest?.header("Content-Type"))
        assertEquals("application/json", interceptedRequest?.header("Accept"))
    }
}
