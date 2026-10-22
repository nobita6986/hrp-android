package com.example

import com.example.data.api.AuthInterceptor
import com.example.data.api.model.ApiError
import com.example.data.api.model.ApiErrorParser
import com.example.data.api.model.AttendanceDayResponse
import com.example.data.api.model.FeedbackItemDto
import com.example.data.api.model.JobOpeningResponse
import com.example.data.api.model.MobileAccessDto
import com.example.data.api.model.ReferralItemDto
import com.example.data.api.model.ReferralProgramDto
import com.example.data.api.model.ReferralProgramWrapper
import com.example.data.api.model.TicketItemResponse
import com.example.data.api.model.WorkerAssignmentDto
import com.example.data.api.model.WorkerAssignmentSiteDto
import com.example.data.api.model.WorkerProfileResponse
import com.example.data.api.model.WorkersMeResponse
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
import org.junit.Assert.assertNotNull
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
    fun `test WorkersMeResponse resolves mobileAccess and assignment with confirmed Site GPS`() {
        val confirmedMeResponse = WorkersMeResponse(
            worker = WorkerProfileResponse(
                id = "w-202",
                fullName = "Trần Văn Nam",
                status = "ASSIGNED"
            ),
            assignment = WorkerAssignmentDto(
                assignmentId = "asg-01",
                projectId = "proj-01",
                projectName = "Samsung Thái Nguyên",
                status = "ACTIVE",
                position = "Lắp ráp linh kiện",
                startDate = "2026-10-01T00:00:00.000Z",
                sites = listOf(
                    WorkerAssignmentSiteDto(
                        id = "site-01",
                        name = "Cổng A - KCN Yên Bình",
                        latitude = 21.4567,
                        longitude = 105.8765,
                        geofenceRadiusMeters = 200,
                        geoConfirmedAt = "2026-10-10T08:00:00.000Z"
                    )
                )
            ),
            mobileAccess = MobileAccessDto(
                phoneVerified = true,
                hasActiveAssignment = true,
                canCheckIn = true
            )
        )
        val profile = confirmedMeResponse.resolvedProfile()
        assertNotNull(profile)
        assertTrue(profile!!.canCheckIn)
        assertTrue(profile.hasActiveAssignment)
        assertTrue(profile.phoneVerified)
        assertTrue(profile.hasConfiguredSiteGps)
        assertEquals(VerificationStatus.APPROVED, profile.verificationStatus)
        assertTrue(profile.factoryLocation.contains("Samsung Thái Nguyên"))

        // Case where Site GPS is NOT yet confirmed by Admin (geoConfirmedAt = null -> backend sets canCheckIn = false)
        val unconfirmedSiteMeResponse = confirmedMeResponse.copy(
            assignment = confirmedMeResponse.assignment?.copy(
                projectName = "PIAGGIO",
                position = "CTY- PIAGGIO - BÌNH XUYÊN 1",
                employeeCode = "NV-4321",
                sites = listOf(
                    WorkerAssignmentSiteDto(
                        id = "site-02",
                        name = "Xưởng Mới",
                        latitude = null,
                        longitude = null,
                        geoConfirmedAt = null
                    )
                )
            ),
            mobileAccess = MobileAccessDto(
                phoneVerified = true,
                hasActiveAssignment = true,
                hasConfiguredSiteGps = false,
                siteGeofenceNotConfigured = true,
                canCheckIn = false
            )
        )
        val unconfirmedProfile = unconfirmedSiteMeResponse.resolvedProfile()
        assertNotNull(unconfirmedProfile)
        assertFalse(unconfirmedProfile!!.canCheckIn)
        assertTrue(unconfirmedProfile.hasActiveAssignment)
        assertFalse(unconfirmedProfile.hasConfiguredSiteGps)
        assertTrue(unconfirmedProfile.siteGeofenceNotConfigured)
    }

    @Test
    fun `test ReferralItemDto parses benefitAmountVnd from BigInt string or numeric`() {
        val dtoFromString = ReferralItemDto(
            id = "ref-01",
            maskedReferredName = "Nguyễn V***",
            maskedReferredPhone = "098***123",
            status = "APPROVED",
            benefitStatus = "APPROVED",
            benefitAmountVndRaw = "1500000",
            submittedAt = "2026-10-19T01:00:00.000Z"
        )
        val domain = dtoFromString.toDomain()
        assertEquals(1_500_000L, domain.benefitAmountVnd)
        assertEquals("2026-10-19", domain.submittedAt)
    }

    @Test
    fun `test ApiErrorParser maps contract error codes accurately`() {
        val otpErr = ApiErrorParser.fromApiError(ApiError(code = "OTP_SERVICE_UNAVAILABLE", message = ""), 503)
        assertEquals("OTP_SERVICE_UNAVAILABLE", otpErr.code)
        assertTrue(otpErr.message.contains("bảo trì"))

        val phoneNotVerifiedErr = ApiErrorParser.fromApiError(ApiError(code = "PHONE_NOT_VERIFIED", message = ""), 403)
        assertEquals("PHONE_NOT_VERIFIED", phoneNotVerifiedErr.code)
        assertTrue(phoneNotVerifiedErr.message.contains("OTP"))

        val siteGeofenceNotConfigErr = ApiErrorParser.fromApiError(ApiError(code = "SITE_GEOFENCE_NOT_CONFIGURED", message = ""), 503)
        assertEquals("SITE_GEOFENCE_NOT_CONFIGURED", siteGeofenceNotConfigErr.code)
        assertTrue(siteGeofenceNotConfigErr.message.contains("xác nhận tọa độ"))

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
            workDate = "2026-10-19T00:00:00.000Z",
            dayOfWeek = "Thứ Hai",
            shift = "MORNING",
            checkInTime = "2026-10-19T00:55:00.000Z",
            checkOutTime = "2026-10-19T10:05:00.000Z",
            regularHours = 8.0,
            otHours = 2.0,
            geofenceResult = "INSIDE",
            factorySite = "Nhà máy HRP",
            status = "APPENDED",
            note = "Đủ công"
        )
        val domain = dto.toDomain()
        assertEquals(AttendanceStatus.APPROVED, domain.status)
        assertEquals("2026-10-19", domain.dateString)
        assertTrue(domain.locationVerified)
        assertEquals(8.0, domain.standardHours, 0.001)
        assertEquals(2.0, domain.otHours, 0.001)

        // Same-time check-in/out (e.g. 14:49 -> 14:49) must NOT default to 8.0h and must NOT be APPROVED
        val sameTimeDto = AttendanceDayResponse(
            id = "att-same-time",
            workDate = "2026-10-21",
            checkInTime = "14:49",
            checkOutTime = "14:49",
            regularHours = null,
            geofenceResult = "INSIDE",
            status = "APPENDED"
        )
        val sameTimeDomain = sameTimeDto.toDomain()
        assertEquals(0.0, sameTimeDomain.standardHours, 0.001)
        assertEquals(AttendanceStatus.ABSENT, sameTimeDomain.status)

        // Valid 7.5h reward shift (08:00 -> 15:30) computes 7.5h when regularHours is null
        val sevenHalfDto = AttendanceDayResponse(
            id = "att-7-5h",
            workDate = "2026-10-21",
            checkInTime = "08:00",
            checkOutTime = "15:30",
            regularHours = null,
            geofenceResult = "INSIDE",
            status = "APPENDED"
        )
        val sevenHalfDomain = sevenHalfDto.toDomain()
        assertEquals(7.5, sevenHalfDomain.standardHours, 0.001)
        assertEquals(AttendanceStatus.APPROVED, sevenHalfDomain.status)
    }

    @Test
    fun `test AuthInterceptor attaches Bearer token and X-HRP-Client android header`() {
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
        assertEquals("hrp_session=test_bearer_jwt_token_456; hrp_token=test_bearer_jwt_token_456", interceptedRequest?.header("Cookie"))
        assertEquals("android", interceptedRequest?.header("X-HRP-Client"))
        assertEquals("application/json", interceptedRequest?.header("Content-Type"))
        assertEquals("application/json", interceptedRequest?.header("Accept"))
    }

    @Test
    fun `test RegisterData and registration error codes match contract section 14_7`() {
        val regData = com.example.data.api.model.RegisterData(
            userId = "usr-999",
            phone = "0912345678",
            fullName = "Nguyễn Văn Mới",
            role = "WORKER",
            phoneVerified = false,
            token = "jwt_register_token_abc"
        )
        assertEquals("usr-999", regData.userId)
        assertEquals("0912345678", regData.phone)
        assertEquals("Nguyễn Văn Mới", regData.fullName)
        assertEquals("WORKER", regData.role)
        assertFalse(regData.phoneVerified)
        assertEquals("jwt_register_token_abc", regData.token)

        val conflictErr = ApiErrorParser.fromApiError(ApiError(code = "PHONE_ALREADY_REGISTERED", message = ""), 409)
        assertEquals("PHONE_ALREADY_REGISTERED", conflictErr.code)
        assertTrue(conflictErr.message.contains("đăng nhập"))

        val profileIncErr = ApiErrorParser.fromApiError(ApiError(code = "PROFILE_INCOMPLETE", message = ""), 422)
        assertEquals("PROFILE_INCOMPLETE", profileIncErr.code)
        assertTrue(profileIncErr.message.contains("họ tên"))
    }

    @Test
    fun `test strict VN phone validation accepts only 10 digits starting with 0 and rejects +84 spaces and separators`() {
        assertTrue(HrpFormatters.isValidStrictVnPhone("0912345678"))
        assertTrue(HrpFormatters.isValidStrictVnPhone("0387654321"))

        // Rejects +84 or 84 prefix (must NOT auto-convert)
        assertFalse(HrpFormatters.isValidStrictVnPhone("+84912345678"))
        assertFalse(HrpFormatters.isValidStrictVnPhone("84912345678"))

        // Rejects whitespace and separators
        assertFalse(HrpFormatters.isValidStrictVnPhone("0912 345 678"))
        assertFalse(HrpFormatters.isValidStrictVnPhone(" 0912345678"))
        assertFalse(HrpFormatters.isValidStrictVnPhone("0912345678 "))
        assertFalse(HrpFormatters.isValidStrictVnPhone("0912.345.678"))
        assertFalse(HrpFormatters.isValidStrictVnPhone("0912-345-678"))

        // Rejects wrong length or non-0 leading digit
        assertFalse(HrpFormatters.isValidStrictVnPhone("091234567"))
        assertFalse(HrpFormatters.isValidStrictVnPhone("09123456789"))
        assertFalse(HrpFormatters.isValidStrictVnPhone("1912345678"))

        // Verify HrpRepository.register rejects +84 and separators before hitting network
        val repo = com.example.data.repository.HrpRepository()
        val plus84Result = kotlinx.coroutines.runBlocking {
            repo.register("+84912345678", "password123", "Nguyễn Văn A")
        }
        assertTrue(plus84Result.isFailure)

        val spaceResult = kotlinx.coroutines.runBlocking {
            repo.register("0912 345 678", "password123", "Nguyễn Văn A")
        }
        assertTrue(spaceResult.isFailure)
    }

    @Test
    fun `test JobDetailHtmlParser parses Piaggio web job detail HTML sections and metadata accurately`() {
        val baseDto = com.example.data.api.model.JobOpeningResponse(
            id = "4b268856-018f-46b9-a2be-941401f2f277",
            slug = "cty-piaggio-binh-xuyen-1-0-23f5f452",
            title = "CTY- PIAGGIO - BÌNH XUYÊN 1",
            companyName = "CTY- PIAGGIO",
            position = "CTY- PIAGGIO - BÌNH XUYÊN 1",
            shift = "08:00-20:00",
            location = "KCN BÌNH XUYÊN 1",
            jobType = "toan_thoi_gian",
            availableSlots = 20,
            deadline = "2026-10-31T00:00:00.000Z",
            statusLabel = "Đang tuyển",
            isHot = true,
            salaryDisplay = "10~15tr"
        )
        val baseJob = baseDto.toDomain()
        assertEquals("Toàn thời gian", baseJob.jobTypeLabel)
        assertEquals("31/10/2026", baseJob.deadlineDisplay)
        assertFalse(baseJob.isDetailLoaded)

        val sampleHtml = """
            <dl class="mt-5 grid gap-3 sm:grid-cols-3">
              <div><dt class="text-xs">Mức lương</dt><dd class="text-sm font-semibold">10~15tr</dd></div>
              <div><dt class="text-xs">Chỗ trống</dt><dd class="text-sm font-semibold">Còn 20 chỗ trống</dd></div>
              <div><dt class="text-xs">Chỉ tiêu đã tuyển</dt><dd class="text-sm font-semibold">0/20</dd></div>
              <div><dt class="text-xs">Hạn nhận hồ sơ</dt><dd class="text-sm font-semibold">31/10/2026</dd></div>
            </dl>
            <section data-section="rich-text" data-source="REAL" aria-label="Mô tả công việc" class="rounded-xl border p-5">
              <h2 class="text-base font-semibold mb-3">Mô tả công việc</h2>
              <div class="prose-like"><p>🚀Tuyển gấp 50 nam - Vị trí:lắp ráp - Có tăng ca, làm thêm thứ 7 x 200% - LCB : 7.700.000đ/22 công</p></div>
            </section>
            <section data-section="rich-text" data-source="REAL" aria-label="Phúc lợi" class="rounded-xl border p-5">
              <h2 class="text-base font-semibold mb-3">Phúc lợi</h2>
              <div class="prose-like"><p>✔ Hỗ trợ ứng lương sau 7 ngày làm việc và tìm nhà trọ cho công nhân ở xa</p></div>
            </section>
            <section data-section="rich-text" data-source="REAL" aria-label="Yêu cầu ứng viên" class="rounded-xl border p-5">
              <h2 class="text-base font-semibold mb-3">Yêu cầu ứng viên</h2>
              <div class="prose-like"><p>Trong Độ Tuổi Lao Động</p></div>
            </section>
            <section data-section="rich-text" data-source="REAL" aria-label="Hướng dẫn ứng tuyển" class="rounded-xl border p-5">
              <h2 class="text-base font-semibold mb-3">Hướng dẫn ứng tuyển</h2>
              <div class="prose-like"><p>👉Bấm vào nút Ứng tuyển trên website hoặc liên hệ với Zalo/ĐT: 0968114696</p></div>
            </section>
            <aside data-section="employer-sidebar" data-source="REAL" aria-label="Đơn vị tuyển dụng">
              <div class="flex items-start gap-2 text-xs">
                <span class="material-symbols-outlined">location_on</span><span>KCN BÌNH XUYÊN 1</span>
              </div>
            </aside>
        """.trimIndent()

        val enriched = com.example.data.api.model.JobDetailHtmlParser.enrichJobWithHtml(baseJob, sampleHtml)
        assertTrue(enriched.isDetailLoaded)
        assertEquals("0/20", enriched.filledQuotaText)
        assertEquals("31/10/2026", enriched.deadlineDisplay)
        assertTrue(enriched.description.contains("Tuyển gấp 50 nam"))
        assertTrue(enriched.benefitsDetail.contains("Hỗ trợ ứng lương sau 7 ngày"))
        assertEquals("Trong Độ Tuổi Lao Động", enriched.requirementsDetail)
        assertTrue(enriched.applicationGuide.contains("0968114696"))
        assertEquals("KCN BÌNH XUYÊN 1", enriched.employerAddress)
    }

    @Test
    fun `test ETrust OTP send and verify flow matches contract section 14_8 without calling real OTP service`() {
        var lastOtpSendRequest: com.example.data.api.model.OtpSendRequest? = null
        var lastOtpVerifyRequest: com.example.data.api.model.OtpVerifyRequest? = null
        var nextSendResponse = com.example.data.api.model.ApiResponse(
            ok = true,
            data = com.example.data.api.model.OtpSendData(sent = true, cooldownSeconds = 60)
        )
        var nextVerifyResponse = com.example.data.api.model.ApiResponse(
            ok = true,
            data = com.example.data.api.model.OtpVerifyData(
                phoneVerified = true,
                verifiedAt = "2026-10-20T08:00:00.000Z"
            )
        )

        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "register" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.RegisterData(
                        userId = "usr-etrust-01",
                        phone = "0912345678",
                        fullName = "Nguyễn Văn OTP",
                        role = "WORKER",
                        phoneVerified = false,
                        token = "jwt_etrust_session_token"
                    )
                )
                "sendPhoneOtp" -> {
                    lastOtpSendRequest = args?.getOrNull(0) as? com.example.data.api.model.OtpSendRequest
                    nextSendResponse
                }
                "verifyPhoneOtp" -> {
                    lastOtpVerifyRequest = args?.getOrNull(0) as? com.example.data.api.model.OtpVerifyRequest
                    nextVerifyResponse
                }
                else -> throw UnsupportedOperationException("Unexpected API call in OTP unit test: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()
            // 1. Register creates Bearer session with phoneVerified = false
            val regResult = repo.register("0912345678", "password123", "Nguyễn Văn OTP")
            assertTrue(regResult.isSuccess)
            assertEquals("jwt_etrust_session_token", tokenManager.getToken())
            assertEquals("0912345678", repo.workerProfile.value.phone)
            assertFalse(repo.workerProfile.value.phoneVerified)

            // 2. Sending OTP with a phone different from the registered account phone is rejected
            val mismatchResult = repo.sendPhoneOtp("0987654321")
            assertTrue(mismatchResult.isFailure)
            assertEquals(null, lastOtpSendRequest)

            // 3. Sending OTP with matching account phone succeeds and returns cooldownSeconds = 60
            val sendResult = repo.sendPhoneOtp("0912345678")
            assertTrue(sendResult.isSuccess)
            assertEquals(60, sendResult.getOrNull()?.cooldownSeconds)
            assertEquals("0912345678", lastOtpSendRequest?.phone)

            // 4. Verify OTP requires strictly 6 digits
            val shortOtpResult = repo.verifyPhoneOtp("12345")
            assertTrue(shortOtpResult.isFailure)
            assertFalse(repo.workerProfile.value.phoneVerified)

            val nonDigitOtpResult = repo.verifyPhoneOtp("12345a")
            assertTrue(nonDigitOtpResult.isFailure)
            assertFalse(repo.workerProfile.value.phoneVerified)

            // 5. Verify OTP does NOT mark verified if verifiedAt is missing or phoneVerified=false
            nextVerifyResponse = com.example.data.api.model.ApiResponse(
                ok = true,
                data = com.example.data.api.model.OtpVerifyData(phoneVerified = true, verifiedAt = "")
            )
            val missingVerifiedAtResult = repo.verifyPhoneOtp("123456")
            assertTrue(missingVerifiedAtResult.isFailure)
            assertFalse(repo.workerProfile.value.phoneVerified)

            // 6. Verify OTP updates phoneVerified=true only when ok=true, phoneVerified=true, and verifiedAt is present
            nextVerifyResponse = com.example.data.api.model.ApiResponse(
                ok = true,
                data = com.example.data.api.model.OtpVerifyData(
                    phoneVerified = true,
                    verifiedAt = "2026-10-20T08:05:00.000Z"
                )
            )
            val verifySuccessResult = repo.verifyPhoneOtp("123456")
            assertTrue(verifySuccessResult.isSuccess)
            assertEquals("123456", lastOtpVerifyRequest?.code)
            assertTrue(repo.workerProfile.value.phoneVerified)
        }
    }

    @Test
    fun `test ETrust OTP surfaces contract error codes OTP_SERVICE_UNAVAILABLE OTP_INVALID_OR_EXPIRED and RATE_LIMIT_EXCEEDED`() {
        var sendErrorCode = "OTP_SERVICE_UNAVAILABLE"
        var verifyErrorCode = "OTP_INVALID_OR_EXPIRED"

        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "register" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.RegisterData(
                        userId = "usr-etrust-02",
                        phone = "0912345678",
                        fullName = "Trần Thị B",
                        role = "WORKER",
                        phoneVerified = false,
                        token = "jwt_etrust_session_token_2"
                    )
                )
                "sendPhoneOtp" -> com.example.data.api.model.ApiResponse<com.example.data.api.model.OtpSendData>(
                    ok = false,
                    error = ApiError(code = sendErrorCode, message = "")
                )
                "verifyPhoneOtp" -> com.example.data.api.model.ApiResponse<com.example.data.api.model.OtpVerifyData>(
                    ok = false,
                    error = ApiError(code = verifyErrorCode, message = "")
                )
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()
            repo.register("0912345678", "password123", "Trần Thị B")

            // Test OTP_SERVICE_UNAVAILABLE
            sendErrorCode = "OTP_SERVICE_UNAVAILABLE"
            val unavailableRes = repo.sendPhoneOtp("0912345678")
            assertTrue(unavailableRes.isFailure)
            val unavailableEx = unavailableRes.exceptionOrNull() as com.example.data.api.model.HrpApiException
            assertEquals("OTP_SERVICE_UNAVAILABLE", unavailableEx.code)

            // Test RATE_LIMIT_EXCEEDED
            sendErrorCode = "RATE_LIMIT_EXCEEDED"
            val rateLimitRes = repo.sendPhoneOtp("0912345678")
            assertTrue(rateLimitRes.isFailure)
            val rateLimitEx = rateLimitRes.exceptionOrNull() as com.example.data.api.model.HrpApiException
            assertEquals("RATE_LIMIT_EXCEEDED", rateLimitEx.code)

            // Test OTP_INVALID_OR_EXPIRED
            verifyErrorCode = "OTP_INVALID_OR_EXPIRED"
            val invalidOtpRes = repo.verifyPhoneOtp("654321")
            assertTrue(invalidOtpRes.isFailure)
            val invalidOtpEx = invalidOtpRes.exceptionOrNull() as com.example.data.api.model.HrpApiException
            assertEquals("OTP_INVALID_OR_EXPIRED", invalidOtpEx.code)
            assertFalse(repo.workerProfile.value.phoneVerified)
            assertFalse(tokenManager.isPhoneVerified())

            // Feedback and Ticket creation must be strictly blocked while phoneVerified == false
            val feedbackBlocked = repo.submitFeedback(FeedbackCategory.SUGGESTION, "Góp ý thử nghiệm khi chưa xác thực OTP")
            assertTrue(feedbackBlocked.isFailure)
            val ticketBlocked = repo.createTicket(TicketType.OTHER, "Tiêu đề", "Nội dung yêu cầu khi chưa xác thực OTP")
            assertTrue(ticketBlocked.isFailure)
        }
    }

    @Test
    fun `test ETrust OTP handles non-2xx HttpException and enforces fail-closed defaults`() {
        // ApiResponse must be fail-closed by default (ok = false)
        val defaultEnvelope = com.example.data.api.model.ApiResponse<com.example.data.api.model.OtpVerifyData>()
        assertFalse(defaultEnvelope.ok)
        assertFalse(defaultEnvelope.isSuccessful)

        var httpStatus = 503
        var errorBodyJson = """{"ok":false,"error":{"code":"OTP_SERVICE_UNAVAILABLE","message":"ETrust tạm ngừng"}}"""

        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "register" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.RegisterData(
                        userId = "usr-etrust-http-err",
                        phone = "0912345678",
                        fullName = "Lê Văn C",
                        role = "WORKER",
                        phoneVerified = false,
                        token = "jwt_etrust_session_token_3"
                    )
                )
                "sendPhoneOtp", "verifyPhoneOtp" -> {
                    val body = okhttp3.ResponseBody.create(null, errorBodyJson)
                    throw retrofit2.HttpException(retrofit2.Response.error<Any>(httpStatus, body))
                }
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()

            // When no account phone is in session, sendPhoneOtp must fail immediately
            val blankAccountSend = repo.sendPhoneOtp("0912345678")
            assertTrue(blankAccountSend.isFailure)

            // Register sets phone = 0912345678, phoneVerified = false
            repo.register("0912345678", "password123", "Lê Văn C")
            assertFalse(repo.workerProfile.value.phoneVerified)

            // HTTP 503 HttpException on sendPhoneOtp
            httpStatus = 503
            errorBodyJson = """{"ok":false,"error":{"code":"OTP_SERVICE_UNAVAILABLE","message":"ETrust tạm ngừng"}}"""
            val send503 = repo.sendPhoneOtp("0912345678")
            assertTrue(send503.isFailure)
            assertEquals("OTP_SERVICE_UNAVAILABLE", (send503.exceptionOrNull() as com.example.data.api.model.HrpApiException).code)

            // HTTP 400 HttpException on verifyPhoneOtp resets any stale phoneVerified state to false
            tokenManager.updatePhoneVerified(true)
            httpStatus = 400
            errorBodyJson = """{"ok":false,"error":{"code":"OTP_INVALID_OR_EXPIRED","message":"Mã OTP hết hạn"}}"""
            val verify400 = repo.verifyPhoneOtp("123456")
            assertTrue(verify400.isFailure)
            assertEquals("OTP_INVALID_OR_EXPIRED", (verify400.exceptionOrNull() as com.example.data.api.model.HrpApiException).code)
            assertFalse(repo.workerProfile.value.phoneVerified)
            assertFalse(tokenManager.isPhoneVerified())
        }
    }

    @Test
    fun `applyToJob translates raw POSSIBLE_MATCH_NOT_RESOLVED code and reuses Idempotency-Key on retry`() {
        val capturedIdempotencyKeys = mutableListOf<String>()
        var httpStatus = 409
        var errorBodyJson = """{"error":"POSSIBLE_MATCH_NOT_RESOLVED","message":"POSSIBLE_MATCH_NOT_RESOLVED"}"""

        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "applyPublicJob" -> {
                    capturedIdempotencyKeys.add(args[1] as String)
                    val body = okhttp3.ResponseBody.create(null, errorBodyJson)
                    throw retrofit2.HttpException(retrofit2.Response.error<Any>(httpStatus, body))
                }
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.saveUserSession(
                token = "jwt_worker_session",
                phone = "0987654321",
                role = "WORKER",
                fullName = "Nguyễn Văn A"
            )

            val res1 = repo.applyToJob(slug = "cty-piaggio-binh-xuyen-1")
            assertTrue(res1.isFailure)
            val err1 = res1.exceptionOrNull() as com.example.data.api.model.HrpApiException
            assertEquals("POSSIBLE_MATCH_NOT_RESOLVED", err1.code)
            assertFalse(err1.message.contains("POSSIBLE_MATCH_NOT_RESOLVED"))
            assertTrue(err1.message.contains("đối soát"))

            // Retrying the exact same job application reuses the same Idempotency-Key
            httpStatus = 429
            errorBodyJson = """{"error":"RATE_LIMITED","message":"Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút."}"""
            val res2 = repo.applyToJob(slug = "cty-piaggio-binh-xuyen-1")
            assertTrue(res2.isFailure)
            val err2 = res2.exceptionOrNull() as com.example.data.api.model.HrpApiException
            assertEquals("RATE_LIMITED", err2.code)
            assertEquals("Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút.", err2.message)
            assertEquals(2, capturedIdempotencyKeys.size)
            assertEquals(capturedIdempotencyKeys[0], capturedIdempotencyKeys[1])
        }
    }

    @Test
    fun `performCheckIn and performCheckOut report missing Site GPS when hasActiveAssignment is true but canCheckIn is false due to unconfirmed Site GPS`() {
        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getProfile" -> WorkersMeResponse(
                    ok = true,
                    worker = WorkerProfileResponse(
                        id = "w-piaggio-01",
                        employeeCode = "NV-4321",
                        fullName = "Duc Nguyen",
                        phone = "0987654321",
                        position = "CTY- PIAGGIO - BÌNH XUYÊN 1",
                        profileStatus = "VERIFIED",
                        employmentStatus = "ACTIVE"
                    ),
                    assignment = WorkerAssignmentDto(
                        assignmentId = "asg-piaggio-01",
                        projectId = "proj-piaggio",
                        projectName = "PIAGGIO",
                        status = "ACTIVE",
                        employeeCode = "NV-4321",
                        position = "CTY- PIAGGIO - BÌNH XUYÊN 1",
                        sites = emptyList()
                    ),
                    mobileAccess = MobileAccessDto(
                        phoneVerified = true,
                        hasActiveAssignment = true,
                        hasConfiguredSiteGps = false,
                        siteGeofenceNotConfigured = true,
                        canCheckIn = false
                    )
                )
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            val profileRes = repo.fetchProfile()
            assertTrue(profileRes.isSuccess)
            val profile = repo.workerProfile.value
            assertTrue(profile.hasActiveAssignment)
            assertFalse(profile.hasConfiguredSiteGps)
            assertTrue(profile.siteGeofenceNotConfigured)
            assertFalse(profile.canCheckIn)

            val checkInRes = repo.performCheckIn(latitude = 21.3, longitude = 105.6, accuracy = 10f)
            assertTrue(checkInRes.isFailure)
            assertEquals(
                "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS nên chưa thể chấm công.",
                checkInRes.exceptionOrNull()?.message
            )

            val checkOutRes = repo.performCheckOut(latitude = 21.3, longitude = 105.6, accuracy = 10f)
            assertTrue(checkOutRes.isFailure)
            assertEquals(
                "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS nên chưa thể chấm công.",
                checkOutRes.exceptionOrNull()?.message
            )
        }
    }

    @Test
    fun `sendPhoneOtp handles sent=false and phoneVerified=true by updating profile and tokenManager immediately`() {
        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "register" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.RegisterData(
                        userId = "usr-admin-verified-01",
                        phone = "0987654321",
                        fullName = "Duc Nguyen",
                        role = "WORKER",
                        phoneVerified = false,
                        token = "jwt_admin_verified_session"
                    )
                )
                "sendPhoneOtp" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.OtpSendData(
                        sent = false,
                        phoneVerified = true,
                        cooldownSeconds = 0
                    )
                )
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()
            repo.register("0987654321", "password123", "Duc Nguyen")
            assertFalse(repo.workerProfile.value.phoneVerified)
            assertFalse(tokenManager.isPhoneVerified())

            val sendResult = repo.sendPhoneOtp("0987654321")
            assertTrue(sendResult.isSuccess)
            val data = sendResult.getOrNull()
            assertNotNull(data)
            assertFalse(data!!.sent)
            assertTrue(data.phoneVerified)
            assertTrue(repo.workerProfile.value.phoneVerified)
            assertTrue(tokenManager.isPhoneVerified())
        }
    }

    @Test
    fun `unauthenticated guest can fetch public jobs and apply with fullName and phone`() {
        var capturedApplyReq: com.example.data.api.model.JobApplicationRequest? = null
        var capturedSlug: String? = null

        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getPublicJobs" -> com.example.data.api.model.JobsRootResponse(
                    jobs = listOf(
                        JobOpeningResponse(
                            id = "job-guest-1",
                            slug = "cty-piaggio-binh-xuyen-1",
                            title = "Công Nhân Lắp Ráp Piaggio",
                            companyName = "CTY- PIAGGIO",
                            location = "KCN Bình Xuyên 1",
                            salaryDisplay = "10 - 15 Triệu"
                        )
                    ),
                    total = 1
                )
                "applyPublicJob" -> {
                    capturedSlug = args[0] as String
                    capturedApplyReq = args[2] as com.example.data.api.model.JobApplicationRequest
                    com.example.data.api.model.JobApplicationResponse(
                        trackingCode = "APP-GUEST-99",
                        status = "NEW"
                    )
                }
                else -> throw UnsupportedOperationException("Unexpected API call: ${method.name}")
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()
            assertFalse(tokenManager.isLoggedIn())

            val jobsRes = repo.fetchJobs()
            assertTrue(jobsRes.isSuccess)
            assertEquals(1, repo.jobListings.value.size)
            assertEquals("Công Nhân Lắp Ráp Piaggio", repo.jobListings.value[0].title)

            val applyRes = repo.applyToJob(
                slug = "cty-piaggio-binh-xuyen-1",
                fullName = "Nguyễn Văn Khách",
                phone = "0912345678",
                consent = true
            )
            assertTrue(applyRes.isSuccess)
            assertEquals("APP-GUEST-99", applyRes.getOrNull()?.trackingCode)
            assertEquals("cty-piaggio-binh-xuyen-1", capturedSlug)
            assertEquals("Nguyễn Văn Khách", capturedApplyReq?.fullName)
            assertEquals("0912345678", capturedApplyReq?.phone)
            assertEquals(true, capturedApplyReq?.consent)
        }
    }

    @Test
    fun `unverified registration never marks tokenManager isLoggedIn true until OTP is verified`() {
        val fakeApi = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.api.HrpApiService::class.java.classLoader,
            arrayOf(com.example.data.api.HrpApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "register" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.RegisterData(
                        userId = "u-unverified-01",
                        phone = "0936224490",
                        fullName = "Thuận",
                        role = "WORKER",
                        phoneVerified = false,
                        token = "jwt_unverified_reg_token"
                    )
                )
                "verifyPhoneOtp" -> com.example.data.api.model.ApiResponse(
                    ok = true,
                    data = com.example.data.api.model.OtpVerifyData(
                        phoneVerified = true,
                        verifiedAt = "2026-10-22T07:00:00.000Z"
                    )
                )
                else -> com.example.data.api.model.ApiResponse<Any>(ok = true)
            }
        } as com.example.data.api.HrpApiService

        val tokenManager = TokenManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val repo = com.example.data.repository.HrpRepository(apiService = fakeApi, tokenManager = tokenManager)

        kotlinx.coroutines.runBlocking {
            tokenManager.clearToken()
            val regRes = repo.register("0936224490", "password123", "Thuận")
            assertTrue(regRes.isSuccess)

            // Token is available for AuthInterceptor to call /api/auth/phone-otp/send & /verify,
            // but isLoggedIn() MUST be false while phoneVerified == false!
            assertEquals("jwt_unverified_reg_token", tokenManager.getToken())
            assertFalse(tokenManager.isPhoneVerified())
            assertFalse(tokenManager.isLoggedIn())

            // Only after verifyPhoneOtp succeeds does isLoggedIn() become true
            val verifyRes = repo.verifyPhoneOtp("123456")
            assertTrue(verifyRes.isSuccess)
            assertTrue(tokenManager.isPhoneVerified())
            assertTrue(tokenManager.isLoggedIn())
        }
    }
}



