package com.example

import com.example.data.api.AuthInterceptor
import com.example.data.api.model.AttendanceDayResponse
import com.example.data.api.model.TicketItemResponse
import com.example.data.api.model.WorkerProfileResponse
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceStatus
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.repository.HrpFormatters
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
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
    fun `test WorkerProfileResponse converts cleanly to domain model`() {
        val dto = WorkerProfileResponse(
            id = "w-101",
            employeeCode = "HRP-9999",
            fullName = "Lê Văn Cường",
            cccdNumber = "034098111222",
            phone = "0987654321",
            factoryLocation = "Samsung SEVT",
            projectCode = "PRJ-01",
            department = "Xưởng Linh Kiện",
            position = "Trưởng ca",
            baseSalaryPerDayVnd = 450_000L,
            joinedDate = "01/01/2024",
            bankAccount = "Vietcombank - 123456"
        )
        val domain = dto.toDomain()
        assertEquals("w-101", domain.id)
        assertEquals("HRP-9999", domain.employeeCode)
        assertEquals("Lê Văn Cường", domain.fullName)
        assertEquals(450_000L, domain.baseSalaryPerDayVnd)
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
    fun `test AttendanceDayResponse maps status string correctly`() {
        val dto = AttendanceDayResponse(
            id = "att-1",
            dateString = "18/10/2026",
            dayOfWeek = "Chủ Nhật",
            shiftName = "Ca Sáng",
            checkInTime = "07:55",
            checkOutTime = "17:05",
            standardHours = 8.0,
            otHours = 2.0,
            locationVerified = true,
            factorySite = "Cổng B - Samsung SEVT",
            status = "APPROVED",
            note = "Đủ công"
        )
        val domain = dto.toDomain()
        assertEquals(AttendanceStatus.APPROVED, domain.status)
        assertEquals(8.0, domain.standardHours, 0.001)
        assertEquals(2.0, domain.otHours, 0.001)
    }

    @Test
    fun `test AuthInterceptor attaches Bearer token when token is present`() {
        var interceptedRequest: Request? = null

        val fakeChain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder()
                .url("https://vieclammienbac.com.vn/api/worker/profile")
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

        // Test with custom token provider
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
