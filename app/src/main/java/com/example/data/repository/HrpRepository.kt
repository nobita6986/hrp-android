package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.HrpApiService
import com.example.data.api.model.ApiError
import com.example.data.api.model.ApiErrorParser
import com.example.data.api.model.CheckInEventData
import com.example.data.api.model.CheckInEventRequest
import com.example.data.api.model.DeviceRegistrationRequest
import com.example.data.api.model.FeedbackSubmitRequest
import com.example.data.api.model.HrpApiException
import com.example.data.api.model.JobApplicationRequest
import com.example.data.api.model.JobApplicationResponse
import com.example.data.api.model.JobDetailHtmlParser
import com.example.data.api.model.LoginRequest
import com.example.data.api.model.LoginResponse
import com.example.data.api.model.OtpSendData
import com.example.data.api.model.OtpSendRequest
import com.example.data.api.model.OtpVerifyData
import com.example.data.api.model.OtpVerifyRequest
import com.example.data.api.model.ReferralSubmitRequest
import com.example.data.api.model.RegisterData
import com.example.data.api.model.RegisterRequest
import com.example.data.api.model.WorkerVerificationRequest
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.BodOverviewStats
import com.example.data.model.CtvCandidate
import com.example.data.model.FeedbackCategory
import com.example.data.model.FeedbackItem
import com.example.data.model.JobOpening
import com.example.data.model.PayslipSummary
import com.example.data.model.PublicProject
import com.example.data.model.ReferralItem
import com.example.data.model.ReferralProgram
import com.example.data.model.RewardAttendancePolicy
import com.example.data.model.TicketItem
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.data.model.WorkerVerificationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object HrpFormatters {
    private val vnLocale = Locale("vi", "VN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(vnLocale)
    private val STRICT_VN_PHONE_REGEX = Regex("^0\\d{9}$")
    private val STRICT_OTP_6_DIGIT_REGEX = Regex("^\\d{6}$")

    /**
     * Strictly validates that phone is 10 digits starting with '0'.
     * Rejects '+84', whitespace, and any separators ('.', '-', etc.) without auto-converting.
     */
    fun isValidStrictVnPhone(phone: String): Boolean {
        return STRICT_VN_PHONE_REGEX.matches(phone)
    }

    /**
     * Strictly validates that OTP code is 6 numeric digits per contract §14.8.
     */
    fun isValidOtpCode(code: String): Boolean {
        return STRICT_OTP_6_DIGIT_REGEX.matches(code)
    }

    fun formatVnd(amount: Long): String {
        return currencyFormat.format(amount).replace("₫", "đ")
    }

    fun formatVndShort(amount: Long): String {
        return when {
            amount >= 1_000_000_000L -> String.format(Locale.US, "%.2f Tỷ đ", amount / 1_000_000_000.0)
            amount >= 1_000_000L -> String.format(Locale.US, "%.1f Tr đ", amount / 1_000_000.0)
            else -> formatVnd(amount)
        }
    }

    fun todayDateIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun nowIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

class HrpRepository(
    private var apiService: HrpApiService? = null,
    private var tokenManager: TokenManager? = null
) {
    companion object {
        private const val TAG = "HrpRepository"

        @Volatile
        private var INSTANCE: HrpRepository? = null

        fun getInstance(context: Context? = null): HrpRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val repo = if (context != null) {
                        val appCtx = context.applicationContext
                        HrpRepository(
                            apiService = ApiClient.getApiService(appCtx),
                            tokenManager = TokenManager.getInstance(appCtx)
                        )
                    } else {
                        HrpRepository()
                    }
                    INSTANCE = repo
                    repo
                }
            }
        }
    }

    fun initServices(context: Context) {
        val appCtx = context.applicationContext
        if (apiService == null) {
            apiService = ApiClient.getApiService(appCtx)
        }
        if (tokenManager == null) {
            tokenManager = TokenManager.getInstance(appCtx)
        }
        tokenManager?.let { tm ->
            if (tm.isLoggedIn()) {
                val cachedName = tm.getFullName()?.takeIf { it.isNotBlank() } ?: "Người Lao Động"
                val cachedPhone = tm.getPhone().orEmpty()
                val cachedPhoneVerified = tm.isPhoneVerified()
                val cachedHasAssignment = tm.hasActiveAssignment()
                val cachedVerStatus = VerificationStatus.fromString(tm.getVerificationStatus())
                _workerProfile.value = _workerProfile.value.copy(
                    fullName = cachedName,
                    phone = cachedPhone,
                    phoneVerified = cachedPhoneVerified,
                    verificationStatus = cachedVerStatus,
                    hasActiveAssignment = cachedHasAssignment,
                    canCheckIn = cachedHasAssignment
                )
                _lastCheckInEpochMs.value = tm.getLastCheckInEpochMs()
                _lastCheckOutEpochMs.value = tm.getLastCheckOutEpochMs()
            }
        }
    }

    private val _currentRole = MutableStateFlow(UserRole.WORKER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _workerProfile = MutableStateFlow(
        WorkerProfile(
            id = "",
            employeeCode = "",
            fullName = "Người Lao Động",
            cccdNumber = "",
            phone = "",
            factoryLocation = "",
            projectCode = "",
            department = "",
            position = "",
            baseSalaryPerDayVnd = 0L,
            joinedDate = "",
            bankAccount = "",
            status = "PENDING_VERIFICATION",
            phoneVerified = false,
            verificationStatus = VerificationStatus.NONE,
            hasActiveAssignment = false
        )
    )
    val workerProfile: StateFlow<WorkerProfile> = _workerProfile.asStateFlow()

    private val _verificationInfo = MutableStateFlow<WorkerVerificationInfo?>(null)
    val verificationInfo: StateFlow<WorkerVerificationInfo?> = _verificationInfo.asStateFlow()

    private val _publicProjects = MutableStateFlow<List<PublicProject>>(emptyList())
    val publicProjects: StateFlow<List<PublicProject>> = _publicProjects.asStateFlow()

    private val _attendanceList = MutableStateFlow<List<AttendanceDay>>(emptyList())
    val attendanceList: StateFlow<List<AttendanceDay>> = _attendanceList.asStateFlow()

    private val _lastCheckInEpochMs = MutableStateFlow(tokenManager?.getLastCheckInEpochMs() ?: 0L)
    val lastCheckInEpochMs: StateFlow<Long> = _lastCheckInEpochMs.asStateFlow()

    private val _lastCheckOutEpochMs = MutableStateFlow(tokenManager?.getLastCheckOutEpochMs() ?: 0L)
    val lastCheckOutEpochMs: StateFlow<Long> = _lastCheckOutEpochMs.asStateFlow()

    private val _payslip = MutableStateFlow(
        PayslipSummary(
            monthPeriod = "",
            standardWorkDays = 0,
            actualWorkDays = 0.0,
            otHours = 0.0,
            baseSalaryVnd = 0L,
            otPayVnd = 0L,
            attendanceBonusVnd = 0L,
            mealAllowanceVnd = 0L,
            transportAllowanceVnd = 0L,
            grossSalaryVnd = 0L,
            socialInsuranceVnd = 0L,
            healthInsuranceVnd = 0L,
            unemploymentInsVnd = 0L,
            personalIncomeTaxVnd = 0L,
            advancePaidVnd = 0L,
            netSalaryVnd = 0L,
            isFinalized = false,
            payDate = ""
        )
    )
    val payslip: StateFlow<PayslipSummary> = _payslip.asStateFlow()

    private val _tickets = MutableStateFlow<List<TicketItem>>(emptyList())
    val tickets: StateFlow<List<TicketItem>> = _tickets.asStateFlow()

    private val _feedbackList = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val feedbackList: StateFlow<List<FeedbackItem>> = _feedbackList.asStateFlow()

    private val _referralProgram = MutableStateFlow<ReferralProgram?>(null)
    val referralProgram: StateFlow<ReferralProgram?> = _referralProgram.asStateFlow()

    private val _referralList = MutableStateFlow<List<ReferralItem>>(emptyList())
    val referralList: StateFlow<List<ReferralItem>> = _referralList.asStateFlow()

    private val _jobListings = MutableStateFlow<List<JobOpening>>(emptyList())
    val jobListings: StateFlow<List<JobOpening>> = _jobListings.asStateFlow()

    private val _ctvCandidates = MutableStateFlow<List<CtvCandidate>>(emptyList())
    val ctvCandidates: StateFlow<List<CtvCandidate>> = _ctvCandidates.asStateFlow()

    private val _bodMetrics = MutableStateFlow(
        BodOverviewStats(
            activeWorkersCount = 0,
            workerGrowthPercent = 0.0,
            fillRatePercent = 0.0,
            totalPayrollVnd = 0L,
            recruitmentSlaDays = 0.0,
            pendingTicketsCount = 0,
            attendanceComplianceRate = 0.0
        )
    )
    val bodMetrics: StateFlow<BodOverviewStats> = _bodMetrics.asStateFlow()

    fun switchRole(role: UserRole) {
        _currentRole.value = UserRole.WORKER
    }

    // ==================== 1-4. AUTH & PHONE OTP ACTIONS ====================

    /**
     * Endpoint 1: POST /api/auth/register
     * Sends X-HRP-Client: android and reads data.token to persist Bearer session directly.
     * Strictly requires phone to be 10 digits starting with '0'; rejects +84, spaces, and separators.
     */
    suspend fun register(phone: String, password: String, fullName: String): Result<RegisterData> = withContext(Dispatchers.IO) {
        if (!HrpFormatters.isValidStrictVnPhone(phone)) {
            return@withContext Result.failure(
                Exception("Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0 (không dùng +84, khoảng trắng hoặc dấu phân cách)")
            )
        }
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.register(RegisterRequest(phone = phone, password = password, fullName = fullName.trim()))
            val data = response.data
            if (response.isSuccessful && data != null) {
                val roleStr = data.role.ifBlank { "WORKER" }
                if (!roleStr.equals("WORKER", ignoreCase = true)) {
                    tokenManager?.clearToken()
                    return@withContext Result.failure(
                        Exception("Ứng dụng chỉ dành cho Người lao động. Vai trò '$roleStr' không được phép sử dụng trên mobile.")
                    )
                }

                val resolvedName = data.fullName.takeIf { it.isNotBlank() } ?: fullName.trim()
                val resolvedPhone = data.phone.takeIf { it.isNotBlank() } ?: phone
                // Strictly respect backend phoneVerified (false on new registration; never auto-verify)
                val isPhoneVerified = data.phoneVerified
                val sessionToken = data.token?.takeIf { it.isNotBlank() }

                // Clear any previous session state before storing the new registration session
                tokenManager?.clearToken()

                if (!sessionToken.isNullOrBlank()) {
                    tokenManager?.saveUserSession(
                        token = sessionToken,
                        phone = resolvedPhone,
                        role = "WORKER",
                        fullName = resolvedName,
                        phoneVerified = isPhoneVerified,
                        verificationStatus = "NONE",
                        hasActiveAssignment = false
                    )
                } else {
                    tokenManager?.updatePhoneVerified(false)
                }
                tokenManager?.saveRememberedCredentials(
                    phone = resolvedPhone,
                    password = password,
                    fullName = resolvedName,
                    verifiedToken = if (isPhoneVerified) sessionToken else null
                )

                _currentRole.value = UserRole.WORKER
                _workerProfile.value = WorkerProfile(
                    id = data.userId,
                    employeeCode = "",
                    fullName = resolvedName,
                    cccdNumber = "",
                    phone = resolvedPhone,
                    factoryLocation = "",
                    projectCode = "",
                    department = "",
                    position = "",
                    baseSalaryPerDayVnd = 0L,
                    joinedDate = "",
                    bankAccount = "",
                    status = "PENDING_VERIFICATION",
                    phoneVerified = isPhoneVerified,
                    verificationStatus = VerificationStatus.NONE,
                    hasActiveAssignment = false,
                    canCheckIn = false,
                    projectName = "",
                    sites = emptyList(),
                    hasConfiguredSiteGps = true,
                    siteGeofenceNotConfigured = false
                )
                _attendanceList.value = emptyList()
                _tickets.value = emptyList()
                _feedbackList.value = emptyList()
                _referralList.value = emptyList()

                if (!sessionToken.isNullOrBlank() && isPhoneVerified) {
                    loadInitialData()
                }

                Result.success(data)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = response.message ?: "Đăng ký tài khoản thất bại"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Không thể đăng ký tài khoản"))
        }
    }

    /**
     * Endpoint 2: POST /api/auth/login (Root JSON)
     */
    suspend fun login(phone: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.login(LoginRequest(phone, password))
            val sessionToken = response.token?.takeIf { it.isNotBlank() }
            if ((response.ok || response.success) && !sessionToken.isNullOrBlank()) {
                val roleStr = response.role ?: response.user?.role ?: "WORKER"
                if (!roleStr.equals("WORKER", ignoreCase = true)) {
                    tokenManager?.clearToken()
                    return@withContext Result.failure(
                        Exception("Ứng dụng chỉ dành cho Người lao động. Tài khoản có vai trò '$roleStr' không được phép đăng nhập trên mobile.")
                    )
                }

                val token = sessionToken
                val accountUser = response.user
                val workerDto = response.worker

                val resolvedName = workerDto?.fullName?.takeIf { it.isNotBlank() }
                    ?: accountUser?.fullName?.takeIf { it.isNotBlank() }
                    ?: accountUser?.name?.takeIf { it.isNotBlank() }
                    ?: "Người Lao Động"
                val resolvedPhone = workerDto?.phone?.takeIf { it.isNotBlank() }
                    ?: accountUser?.phone?.takeIf { it.isNotBlank() }
                    ?: phone
                val phoneVerified = workerDto?.phoneVerified ?: accountUser?.phoneVerified ?: false
                val hasActiveAssignment = workerDto?.hasActiveAssignment
                    ?: workerDto?.assignment?.status?.equals("ACTIVE", ignoreCase = true)
                    ?: false
                val verStatusStr = workerDto?.verificationStatus
                    ?: accountUser?.verificationStatus
                    ?: if (hasActiveAssignment) "APPROVED" else "UNVERIFIED"

                tokenManager?.saveUserSession(
                    token = token,
                    phone = resolvedPhone,
                    role = "WORKER",
                    fullName = resolvedName,
                    phoneVerified = phoneVerified,
                    verificationStatus = verStatusStr,
                    hasActiveAssignment = hasActiveAssignment
                )
                _currentRole.value = UserRole.WORKER

                if (workerDto != null) {
                    val initialDomain = workerDto.toDomain()
                    _workerProfile.value = initialDomain.copy(
                        fullName = initialDomain.fullName.ifBlank { resolvedName },
                        phone = initialDomain.phone.ifBlank { resolvedPhone },
                        phoneVerified = phoneVerified,
                        hasActiveAssignment = hasActiveAssignment,
                        canCheckIn = hasActiveAssignment
                    )
                } else {
                    _workerProfile.value = _workerProfile.value.copy(
                        id = accountUser?.id ?: "",
                        fullName = resolvedName,
                        phone = resolvedPhone,
                        phoneVerified = phoneVerified,
                        verificationStatus = VerificationStatus.fromString(verStatusStr),
                        hasActiveAssignment = hasActiveAssignment,
                        canCheckIn = false
                    )
                }

                fetchProfile()
                tokenManager?.saveRememberedCredentials(
                    phone = _workerProfile.value.phone.ifBlank { resolvedPhone },
                    password = password,
                    fullName = _workerProfile.value.fullName.ifBlank { resolvedName },
                    verifiedToken = if (_workerProfile.value.phoneVerified) token else null
                )
                if (_workerProfile.value.phoneVerified) {
                    loadInitialData()
                }

                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Sai số điện thoại hoặc mật khẩu"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Không thể đăng nhập. Vui lòng kiểm tra kết nối."))
        }
    }

    /**
     * Endpoint 3: POST /api/auth/phone-otp/send
     * Contract §14.8: Bearer auth, body { "phone": "09xxxxxxxx" }.
     * Phone must be 10 digits starting with '0' and match the phone currently stored in the account.
     * Success: { "ok": true, "data": { "sent": true, "cooldownSeconds": 60 } }.
     */
    suspend fun sendPhoneOtp(phone: String = _workerProfile.value.phone): Result<OtpSendData> = withContext(Dispatchers.IO) {
        val accountPhone = _workerProfile.value.phone.trim().ifBlank { tokenManager?.getPhone()?.trim().orEmpty() }
        val targetPhone = phone.trim().ifBlank { accountPhone }

        if (accountPhone.isBlank()) {
            return@withContext Result.failure(
                HrpApiException(
                    code = "VALIDATION_ERROR",
                    message = "Không tìm thấy số điện thoại trong tài khoản đã đăng nhập để gửi mã OTP.",
                    httpCode = 400
                )
            )
        }
        if (!HrpFormatters.isValidStrictVnPhone(targetPhone)) {
            return@withContext Result.failure(
                HrpApiException(
                    code = "VALIDATION_ERROR",
                    message = "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0 (không dùng +84, khoảng trắng hoặc dấu phân cách).",
                    httpCode = 400
                )
            )
        }
        if (targetPhone != accountPhone) {
            return@withContext Result.failure(
                HrpApiException(
                    code = "VALIDATION_ERROR",
                    message = "Số điện thoại gửi OTP phải khớp với số điện thoại trong tài khoản ($accountPhone).",
                    httpCode = 400
                )
            )
        }

        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.sendPhoneOtp(OtpSendRequest(phone = targetPhone))
            val data = response.data
            if (response.isSuccessful && data != null) {
                if (data.phoneVerified) {
                    tokenManager?.updatePhoneVerified(true)
                    _workerProfile.value = _workerProfile.value.copy(phoneVerified = true)
                    Result.success(data)
                } else if (data.sent) {
                    Result.success(data)
                } else {
                    val fallbackCode = response.error?.code ?: "OTP_SERVICE_UNAVAILABLE"
                    val fallbackHttp = if (fallbackCode.equals("OTP_SERVICE_UNAVAILABLE", ignoreCase = true)) 503 else 400
                    val err = ApiErrorParser.fromApiError(
                        error = response.error ?: ApiError(code = fallbackCode, message = response.message.orEmpty()),
                        httpCode = fallbackHttp,
                        fallbackMessage = response.message ?: "Không thể gửi mã OTP"
                    )
                    Result.failure(err)
                }
            } else {
                val fallbackCode = response.error?.code ?: "OTP_SERVICE_UNAVAILABLE"
                val fallbackHttp = if (fallbackCode.equals("OTP_SERVICE_UNAVAILABLE", ignoreCase = true)) 503 else 400
                val err = ApiErrorParser.fromApiError(
                    error = response.error ?: ApiError(code = fallbackCode, message = response.message.orEmpty()),
                    httpCode = fallbackHttp,
                    fallbackMessage = response.message ?: "Không thể gửi mã OTP"
                )
                Result.failure(err)
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Hệ thống gửi mã OTP đang tạm gián đoạn"))
        }
    }

    /**
     * Endpoint 4: POST /api/auth/phone-otp/verify
     * Contract §14.8: Bearer auth, body { "code": "123456" } (6 digits).
     * Only marks phoneVerified=true when backend returns { "ok": true, "data": { "phoneVerified": true, "verifiedAt": "..." } }.
     */
    suspend fun verifyPhoneOtp(code: String): Result<OtpVerifyData> = withContext(Dispatchers.IO) {
        val trimmedCode = code.trim()
        if (!HrpFormatters.isValidOtpCode(trimmedCode)) {
            return@withContext Result.failure(
                HrpApiException(
                    code = "OTP_INVALID_OR_EXPIRED",
                    message = "Mã OTP phải gồm đúng 6 chữ số.",
                    httpCode = 400
                )
            )
        }
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.verifyPhoneOtp(OtpVerifyRequest(code = trimmedCode))
            val data = response.data
            if (response.isSuccessful && data != null && data.phoneVerified && !data.verifiedAt.isNullOrBlank()) {
                tokenManager?.updatePhoneVerified(true)
                _workerProfile.value = _workerProfile.value.copy(phoneVerified = true)
                Result.success(data)
            } else {
                // Keep phoneVerified=false on any invalid/expired/unverified response
                tokenManager?.updatePhoneVerified(false)
                _workerProfile.value = _workerProfile.value.copy(phoneVerified = false)
                val fallbackCode = response.error?.code ?: "OTP_INVALID_OR_EXPIRED"
                val fallbackHttp = when (fallbackCode.uppercase()) {
                    "OTP_SERVICE_UNAVAILABLE" -> 503
                    "RATE_LIMIT_EXCEEDED" -> 429
                    else -> 400
                }
                val err = ApiErrorParser.fromApiError(
                    error = response.error ?: ApiError(code = fallbackCode, message = response.message.orEmpty()),
                    httpCode = fallbackHttp,
                    fallbackMessage = response.message ?: "Mã OTP không chính xác hoặc đã hết hạn."
                )
                Result.failure(err)
            }
        } catch (t: Throwable) {
            tokenManager?.updatePhoneVerified(false)
            _workerProfile.value = _workerProfile.value.copy(phoneVerified = false)
            Result.failure(ApiErrorParser.parseThrowable(t, "Xác thực OTP thất bại"))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            tokenManager?.clearToken()
        } catch (_: Exception) {
        }
        _workerProfile.value = WorkerProfile(
            id = "",
            employeeCode = "",
            fullName = "Người Lao Động",
            cccdNumber = "",
            phone = "",
            factoryLocation = "",
            projectCode = "",
            department = "",
            position = "",
            baseSalaryPerDayVnd = 0L,
            joinedDate = "",
            bankAccount = "",
            status = "PENDING_VERIFICATION",
            phoneVerified = false,
            verificationStatus = VerificationStatus.NONE,
            hasActiveAssignment = false,
            canCheckIn = false
        )
        _attendanceList.value = emptyList()
        _lastCheckInEpochMs.value = 0L
        _lastCheckOutEpochMs.value = 0L
        _tickets.value = emptyList()
        _feedbackList.value = emptyList()
        _referralList.value = emptyList()
    }

    /**
     * Restores a remembered worker session via saved credentials or saved session cookie token
     * so workers who accidentally tap Logout do not need to hunt for or remember their account.
     */
    suspend fun restoreRememberedAccount(): Result<Boolean> = withContext(Dispatchers.IO) {
        val tm = tokenManager ?: return@withContext Result.failure(Exception("Không tìm thấy bộ nhớ phiên đăng nhập"))
        val rememberedPhone = tm.getRememberedPhone()
        val rememberedPass = tm.getRememberedPassword()
        val rememberedName = tm.getRememberedFullName()
        val rememberedToken = tm.getRememberedToken()

        if (rememberedPhone.isNotBlank() && rememberedPass.isNotBlank()) {
            val loginRes = login(rememberedPhone, rememberedPass)
            if (loginRes.isSuccess) {
                return@withContext Result.success(true)
            }
        }

        if (rememberedPhone.isNotBlank() && rememberedToken.isNotBlank()) {
            tm.saveUserSession(
                token = rememberedToken,
                phone = rememberedPhone,
                role = "WORKER",
                fullName = rememberedName.ifBlank { "Người Lao Động" },
                phoneVerified = true
            )
            _workerProfile.value = _workerProfile.value.copy(
                phone = rememberedPhone,
                fullName = rememberedName.ifBlank { "Người Lao Động" },
                phoneVerified = true
            )
            loadInitialData()
            return@withContext Result.success(true)
        }

        Result.failure(Exception("Chưa có tài khoản lưu sẵn trên thiết bị này"))
    }

    // ==================== DATA SYNC / REFRESH ====================

    suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        fetchProfile()
        if (_workerProfile.value.canCheckIn || _workerProfile.value.hasActiveAssignment) {
            fetchAttendance()
        } else {
            _attendanceList.value = emptyList()
        }
        if (_workerProfile.value.phoneVerified) {
            fetchTickets()
        } else {
            _feedbackList.value = emptyList()
            _tickets.value = emptyList()
        }
        fetchJobs()
        fetchReferralProgram()
        if (_workerProfile.value.canCheckIn || _workerProfile.value.hasActiveAssignment) {
            fetchMyReferrals()
        } else {
            _referralList.value = emptyList()
        }
    }

    /**
     * GET /api/workers/me (Root JSON { worker, assignment, mobileAccess } or Envelope)
     */
    suspend fun fetchProfile(): Result<WorkerProfile> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getProfile()
            val resolved = response.resolvedProfile()
            if (response.isSuccessful && resolved != null) {
                val fallbackPhone = _workerProfile.value.phone.ifBlank { tokenManager?.getPhone().orEmpty() }
                val fallbackName = _workerProfile.value.fullName.takeIf { it.isNotBlank() && it != "Người Lao Động" }
                    ?: tokenManager?.getFullName().orEmpty().ifBlank { "Người Lao Động" }
                val domain = resolved.copy(
                    fullName = resolved.fullName.ifBlank { fallbackName },
                    phone = resolved.phone.ifBlank { fallbackPhone }
                )
                _workerProfile.value = domain
                tokenManager?.updatePhoneVerified(domain.phoneVerified)
                tokenManager?.updateVerificationStatus(domain.verificationStatus.name, domain.hasActiveAssignment)
                Result.success(domain)
            } else {
                val err = ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không lấy được thông tin hồ sơ")
                Result.failure(err)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching profile: ${t.message}")
            val parsed = ApiErrorParser.parseThrowable(t, "Không lấy được thông tin hồ sơ")
            // If 404 WORKER_PROFILE_NOT_FOUND, keep user logged in as WORKER but mark canCheckIn=false, hasActiveAssignment=false
            if (parsed.code.equals("WORKER_PROFILE_NOT_FOUND", ignoreCase = true) || parsed.httpCode == 404) {
                val fallbackPhone = _workerProfile.value.phone.ifBlank { tokenManager?.getPhone().orEmpty() }
                val fallbackName = _workerProfile.value.fullName.takeIf { it.isNotBlank() && it != "Người Lao Động" }
                    ?: tokenManager?.getFullName()?.takeIf { it.isNotBlank() }
                    ?: "Người Lao Động"
                val fallbackPhoneVerified = tokenManager?.isPhoneVerified() ?: _workerProfile.value.phoneVerified
                _workerProfile.value = _workerProfile.value.copy(
                    fullName = fallbackName,
                    phone = fallbackPhone,
                    phoneVerified = fallbackPhoneVerified,
                    verificationStatus = VerificationStatus.NONE,
                    hasActiveAssignment = false,
                    canCheckIn = false
                )
            }
            Result.failure(parsed)
        }
    }

    // ==================== 5-7. PUBLIC PROJECTS & WORKER VERIFICATION ====================

    /**
     * Endpoint 5: GET /api/public/projects
     */
    suspend fun fetchPublicProjects(): Result<List<PublicProject>> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getPublicProjects("ACTIVE")
            if (response.isSuccessful && response.data != null) {
                val list = response.data.resolvedProjects()
                _publicProjects.value = list
                Result.success(list)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không tải được danh sách dự án"))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching public projects: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không tải được danh sách dự án"))
        }
    }

    /**
     * Endpoint 6: POST /api/worker-verifications
     */
    suspend fun submitWorkerVerification(
        fullName: String,
        cccdNumber: String,
        dob: String,
        projectId: String,
        employeeCode: String
    ): Result<WorkerVerificationInfo> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val req = WorkerVerificationRequest(
                fullName = fullName,
                cccdNumber = cccdNumber,
                dob = dob,
                projectId = projectId,
                employeeCode = employeeCode
            )
            val response = api.submitWorkerVerification(req)
            val data = response.data
            if (response.isSuccessful && data != null) {
                val status = VerificationStatus.fromString(data.status)
                val info = WorkerVerificationInfo(
                    id = data.verificationId,
                    status = status,
                    projectId = projectId,
                    employeeCode = employeeCode,
                    submittedAt = data.submittedAt
                )
                _verificationInfo.value = info
                _workerProfile.value = _workerProfile.value.copy(verificationStatus = status)
                tokenManager?.updateVerificationStatus(status.name)
                Result.success(info)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Gửi yêu cầu xác thực thất bại"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Không thể gửi yêu cầu xác thực công nhân"))
        }
    }

    /**
     * Endpoint 7: GET /api/worker-verifications/me
     */
    suspend fun fetchMyVerification(): Result<WorkerVerificationInfo?> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getMyWorkerVerification()
            if (response.isSuccessful && response.data != null) {
                val detail = response.data.verification?.toDomain()
                _verificationInfo.value = detail
                if (detail != null) {
                    _workerProfile.value = _workerProfile.value.copy(verificationStatus = detail.status)
                    tokenManager?.updateVerificationStatus(detail.status.name)
                }
                Result.success(detail)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không tải được trạng thái xác thực"))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching worker verification: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không tải được trạng thái xác thực"))
        }
    }

    // ==================== 8-9. PUBLIC JOBS & APPLICATIONS ====================

    private val jobDetailCache = mutableMapOf<String, JobOpening>()

    /**
     * Endpoint 8: GET /api/jobs (Root JSON { jobs, nextOffset, total, facets, overview })
     */
    suspend fun fetchJobs(
        query: String? = null,
        area: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): Result<List<JobOpening>> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getPublicJobs(q = query, area = area, offset = offset, limit = limit)
            val domainList = response.jobs.map { dto ->
                val base = dto.toDomain()
                val cached = jobDetailCache[base.slug]
                if (cached != null && cached.isDetailLoaded) {
                    base.copy(
                        filledQuotaText = cached.filledQuotaText.ifBlank { base.filledQuotaText },
                        deadlineDisplay = cached.deadlineDisplay.ifBlank { base.deadlineDisplay },
                        description = cached.description.ifBlank { base.description },
                        benefitsDetail = cached.benefitsDetail.ifBlank { base.benefitsDetail },
                        requirementsDetail = cached.requirementsDetail.ifBlank { base.requirementsDetail },
                        applicationGuide = cached.applicationGuide.ifBlank { base.applicationGuide },
                        employerAddress = cached.employerAddress.ifBlank { base.employerAddress },
                        isDetailLoaded = true
                    )
                } else {
                    base
                }
            }
            _jobListings.value = domainList
            Result.success(domainList)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching jobs: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không lấy được danh sách việc làm"))
        }
    }

    /**
     * Fetches full job details by slug (combining GET /api/jobs/{slug} and rich-text sections from /viec-lam/{slug}).
     */
    suspend fun fetchJobDetail(job: JobOpening): Result<JobOpening> = withContext(Dispatchers.IO) {
        val slug = job.slug.takeIf { it.isNotBlank() } ?: job.id
        jobDetailCache[slug]?.takeIf { it.isDetailLoaded }?.let { cached ->
            return@withContext Result.success(cached)
        }

        try {
            var resolvedJob = job
            apiService?.let { api ->
                try {
                    val detailResp = api.getPublicJobDetail(slug)
                    detailResp.job?.toDomain()?.let { apiJob ->
                        resolvedJob = apiJob
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "GET /api/jobs/$slug fallback to base job: ${e.message}")
                }
            }

            if (!resolvedJob.isDetailLoaded) {
                val webUrl = "${ApiClient.getSiteOriginUrl()}/viec-lam/$slug"
                val html = ApiClient.fetchWebPageHtml(webUrl)
                if (!html.isNullOrBlank()) {
                    resolvedJob = JobDetailHtmlParser.enrichJobWithHtml(resolvedJob, html)
                }
            }

            if (resolvedJob.isDetailLoaded) {
                jobDetailCache[slug] = resolvedJob
            }
            _jobListings.value = _jobListings.value.map { existing ->
                if (existing.id == resolvedJob.id || existing.slug == slug) resolvedJob else existing
            }
            Result.success(resolvedJob)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching job detail for $slug: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không tải được chi tiết việc làm"))
        }
    }

    private var pendingApplySignature: String? = null
    private var pendingApplyIdempotencyKey: String? = null

    /**
     * Endpoint 9: POST /api/public/jobs/{slug}/applications (Root JSON { trackingCode, status })
     * Sends Idempotency-Key via HTTP Header per contract Section 6.2 & 14.3.B.
     * When authenticated as WORKER, omits fullName/phone so backend takes them from the logged-in account per Section 14.7.2.
     */
    suspend fun applyToJob(
        slug: String,
        fullName: String? = null,
        phone: String? = null,
        experience: String? = null,
        dateOfBirth: String? = null,
        gender: String? = null,
        consent: Boolean = true
    ): Result<JobApplicationResponse> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val isAuthenticated = tokenManager?.isLoggedIn() == true
            val resolvedName = if (isAuthenticated) null else fullName?.takeIf { it.isNotBlank() }
            val resolvedPhone = if (isAuthenticated) null else phone?.takeIf { it.isNotBlank() }
            val signature = listOf(
                slug.trim(),
                isAuthenticated.toString(),
                resolvedName.orEmpty(),
                resolvedPhone.orEmpty(),
                experience.orEmpty(),
                dateOfBirth.orEmpty(),
                gender.orEmpty(),
                consent.toString()
            ).joinToString("|")

            val idempotencyKey = if (pendingApplySignature == signature && !pendingApplyIdempotencyKey.isNullOrBlank()) {
                pendingApplyIdempotencyKey!!
            } else {
                UUID.randomUUID().toString().also {
                    pendingApplySignature = signature
                    pendingApplyIdempotencyKey = it
                }
            }

            val req = JobApplicationRequest(
                fullName = resolvedName,
                phone = resolvedPhone,
                experience = experience,
                dateOfBirth = dateOfBirth,
                gender = gender,
                consent = consent
            )
            val response = api.applyPublicJob(
                slug = slug,
                idempotencyKey = idempotencyKey,
                req = req
            )
            pendingApplySignature = null
            pendingApplyIdempotencyKey = null
            Result.success(response)
        } catch (t: Throwable) {
            val parsed = ApiErrorParser.parseThrowable(t, "Nộp hồ sơ ứng tuyển thất bại")
            if (parsed.code.equals("IDEMPOTENCY_PAYLOAD_MISMATCH", ignoreCase = true) ||
                parsed.code.equals("IDEMPOTENCY_CONFLICT", ignoreCase = true)
            ) {
                pendingApplySignature = null
                pendingApplyIdempotencyKey = null
            }
            Result.failure(parsed)
        }
    }

    // ==================== 10-11. ATTENDANCE & GPS CHECK-IN ====================

    /**
     * Endpoint 10: GET /api/worker/attendance (Root JSON { items, total })
     */
    suspend fun fetchAttendance(month: Int? = null, year: Int? = null): Result<List<AttendanceDay>> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getWorkerAttendance(month = month, year = year)
            val domainList = response.items.map { it.toDomain() }
            _attendanceList.value = domainList
            Result.success(domainList)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching attendance: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không lấy được dữ liệu chấm công"))
        }
    }

    /**
     * Endpoint 11: POST /api/worker/checkins/events (Mobile Envelope)
     * Header: Idempotency-Key: <UUID>
     */
    suspend fun submitCheckInEvent(
        action: String, // "CHECK_IN" | "CHECK_OUT"
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float
    ): Result<CheckInEventData> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val idempotencyKey = UUID.randomUUID().toString()
            val safeAccuracy = if (accuracyMeters > 0f && accuracyMeters.isFinite()) accuracyMeters else 15.0f
            val req = CheckInEventRequest(
                workDate = HrpFormatters.todayDateIso(),
                action = action,
                gpsLatitude = latitude,
                gpsLongitude = longitude,
                gpsAccuracyMeters = safeAccuracy,
                capturedAt = HrpFormatters.nowIsoTimestamp()
            )
            val response = api.submitCheckInEvent(
                idempotencyKey = idempotencyKey,
                req = req
            )
            val data = response.data
            if (response.isSuccessful && data != null) {
                if (_workerProfile.value.siteGeofenceNotConfigured) {
                    _workerProfile.value = _workerProfile.value.copy(siteGeofenceNotConfigured = false)
                }
                // Refresh attendance history after check-in/out event
                fetchAttendance()
                Result.success(data)
            } else {
                if (response.error?.code.equals("SITE_GEOFENCE_NOT_CONFIGURED", ignoreCase = true)) {
                    _workerProfile.value = _workerProfile.value.copy(siteGeofenceNotConfigured = true)
                }
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Chấm công thất bại từ máy chủ"))
            }
        } catch (t: Throwable) {
            val parsed = ApiErrorParser.parseThrowable(t, "Lỗi chấm công qua GPS")
            if (parsed.code.equals("SITE_GEOFENCE_NOT_CONFIGURED", ignoreCase = true)) {
                _workerProfile.value = _workerProfile.value.copy(siteGeofenceNotConfigured = true)
            }
            Result.failure(parsed)
        }
    }

    suspend fun performCheckIn(
        site: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        val currentProfile = _workerProfile.value
        if (!currentProfile.canCheckIn) {
            val errorMsg = when {
                currentProfile.hasActiveAssignment &&
                    (!currentProfile.hasConfiguredSiteGps || currentProfile.siteGeofenceNotConfigured) ->
                    "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS nên chưa thể chấm công."
                currentProfile.hasActiveAssignment ->
                    "Hồ sơ công nhân đang chờ hoàn tất kích hoạt chấm công."
                else ->
                    "Tài khoản chưa đủ điều kiện chấm công (cần được Admin liên kết hồ sơ công nhân và phân công dự án ACTIVE)."
            }
            return@withContext Result.failure(Exception(errorMsg))
        }
        val gate = RewardAttendancePolicy.evaluateButtonGate(
            attendanceList = _attendanceList.value,
            cachedCheckInMs = _lastCheckInEpochMs.value,
            cachedCheckOutMs = _lastCheckOutEpochMs.value
        )
        if (!gate.isCheckInEnabled) {
            return@withContext Result.failure(
                Exception(gate.statusHint ?: "Chưa đến thời điểm được phép bấm Vào Ca.")
            )
        }
        if (latitude == null || longitude == null) {
            return@withContext Result.failure(Exception("Yêu cầu định vị GPS thực tế từ thiết bị để chấm công."))
        }
        val eventResult = submitCheckInEvent(
            action = "CHECK_IN",
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy ?: 15.0f
        )
        eventResult.map { eventData ->
            val nowMs = System.currentTimeMillis()
            val today = HrpFormatters.todayDateIso()
            val parsedInMs = RewardAttendancePolicy.parseDateAndTimeEpochMs(today, eventData.recordedAt) ?: nowMs
            _lastCheckInEpochMs.value = parsedInMs
            _lastCheckOutEpochMs.value = 0L
            tokenManager?.saveCheckInOutTimestamps(checkInEpochMs = parsedInMs, checkOutEpochMs = 0L)

            _attendanceList.value.firstOrNull { it.dateString == today } ?: AttendanceDay(
                id = eventData.attendanceId.ifBlank { UUID.randomUUID().toString() },
                dateString = today,
                dayOfWeek = today,
                shiftName = "Ca tiêu chuẩn",
                checkInTime = eventData.recordedAt,
                checkOutTime = null,
                standardHours = 0.0,
                otHours = 0.0,
                locationVerified = eventData.geofenceResult.equals("INSIDE", ignoreCase = true),
                factorySite = site.ifBlank { _workerProfile.value.factoryLocation.ifBlank { "Nhà máy HRP" } },
                status = AttendanceStatus.PENDING,
                note = "Đang trong ca (cần bấm Tan ca sau 6h–12h để được ghi nhận)",
                geofenceResult = eventData.geofenceResult,
                riskFlag = false
            )
        }
    }

    suspend fun performCheckOut(
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        val currentProfile = _workerProfile.value
        if (!currentProfile.canCheckIn) {
            val errorMsg = when {
                currentProfile.hasActiveAssignment &&
                    (!currentProfile.hasConfiguredSiteGps || currentProfile.siteGeofenceNotConfigured) ->
                    "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS nên chưa thể chấm công."
                currentProfile.hasActiveAssignment ->
                    "Hồ sơ công nhân đang chờ hoàn tất kích hoạt chấm công."
                else ->
                    "Tài khoản chưa đủ điều kiện chấm công (cần được Admin liên kết hồ sơ công nhân và phân công dự án ACTIVE)."
            }
            return@withContext Result.failure(Exception(errorMsg))
        }
        val gate = RewardAttendancePolicy.evaluateButtonGate(
            attendanceList = _attendanceList.value,
            cachedCheckInMs = _lastCheckInEpochMs.value,
            cachedCheckOutMs = _lastCheckOutEpochMs.value
        )
        if (!gate.isCheckOutEnabled) {
            return@withContext Result.failure(
                Exception(gate.statusHint ?: "Chưa đủ điều kiện bấm Tan Ca (khoảng cách Vào ca – Tan ca phải từ 6h đến 12h).")
            )
        }
        if (latitude == null || longitude == null) {
            return@withContext Result.failure(Exception("Yêu cầu định vị GPS thực tế từ thiết bị để kết thúc ca."))
        }
        val eventResult = submitCheckInEvent(
            action = "CHECK_OUT",
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy ?: 15.0f
        )
        eventResult.map { eventData ->
            val nowMs = System.currentTimeMillis()
            val today = HrpFormatters.todayDateIso()
            val parsedOutMs = RewardAttendancePolicy.parseDateAndTimeEpochMs(today, eventData.recordedAt) ?: nowMs
            _lastCheckOutEpochMs.value = parsedOutMs
            tokenManager?.saveCheckInOutTimestamps(checkOutEpochMs = parsedOutMs)

            _attendanceList.value.firstOrNull { it.dateString == today } ?: AttendanceDay(
                id = eventData.attendanceId.ifBlank { UUID.randomUUID().toString() },
                dateString = today,
                dayOfWeek = today,
                shiftName = "Ca tiêu chuẩn",
                checkInTime = null,
                checkOutTime = eventData.recordedAt,
                standardHours = 0.0,
                otHours = 0.0,
                locationVerified = eventData.geofenceResult.equals("INSIDE", ignoreCase = true),
                factorySite = _workerProfile.value.factoryLocation.ifBlank { "Nhà máy HRP" },
                status = AttendanceStatus.APPROVED,
                note = "Tan ca GPS",
                geofenceResult = eventData.geofenceResult,
                riskFlag = false
            )
        }
    }

    // ==================== 12-13. FEEDBACK (GÓP Ý) ====================

    /**
     * Endpoint 13: GET /api/feedback/mine
     */
    suspend fun fetchMyFeedback(page: Int = 1, limit: Int = 20): Result<List<FeedbackItem>> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getMyFeedback(page = page, limit = limit)
            if (response.isSuccessful && response.data != null) {
                val feedbackDomain = response.data.items.map { it.toDomain() }
                val ticketDomain = response.data.items.map { it.toTicketItem() }
                _feedbackList.value = feedbackDomain
                _tickets.value = ticketDomain
                Result.success(feedbackDomain)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không lấy được danh sách góp ý"))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching feedback: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không lấy được danh sách góp ý"))
        }
    }

    /**
     * Endpoint 12: POST /api/feedback
     */
    suspend fun submitFeedback(category: FeedbackCategory, content: String): Result<FeedbackItem> = withContext(Dispatchers.IO) {
        val trimmedContent = content.trim()
        if (!_workerProfile.value.phoneVerified) {
            return@withContext Result.failure(
                Exception("Tài khoản chưa xác thực số điện thoại (phoneVerified=false). Cần xác thực OTP trước khi gửi góp ý.")
            )
        }
        if (trimmedContent.length < 10) {
            return@withContext Result.failure(
                Exception("Nội dung góp ý phải có ít nhất 10 ký tự.")
            )
        }
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val req = FeedbackSubmitRequest(
                category = category.code,
                content = trimmedContent
            )
            val response = api.submitFeedback(req)
            val data = response.data
            if (response.isSuccessful && data != null) {
                val item = FeedbackItem(
                    id = data.feedbackId,
                    category = category,
                    content = trimmedContent,
                    status = data.status,
                    createdAt = data.submittedAt ?: HrpFormatters.todayDateIso()
                )
                _feedbackList.value = listOf(item) + _feedbackList.value
                fetchMyFeedback()
                Result.success(item)
            } else {
                if (response.error?.code.equals("PHONE_NOT_VERIFIED", ignoreCase = true)) {
                    _workerProfile.value = _workerProfile.value.copy(phoneVerified = false)
                    tokenManager?.updatePhoneVerified(false)
                }
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Gửi góp ý thất bại"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Không thể gửi góp ý"))
        }
    }

    suspend fun fetchTickets(): Result<List<TicketItem>> = withContext(Dispatchers.IO) {
        fetchMyFeedback().map { _tickets.value }
    }

    suspend fun createTicket(
        type: TicketType,
        title: String,
        description: String,
        amountVnd: Long? = null,
        requestedDays: String? = null
    ): Result<TicketItem> = withContext(Dispatchers.IO) {
        val category = when (type) {
            TicketType.ADVANCE_SALARY -> FeedbackCategory.SALARY_BENEFITS
            TicketType.TIMESHEET_DISPUTE -> FeedbackCategory.SALARY_BENEFITS
            TicketType.LEAVE_REQUEST -> FeedbackCategory.WORK_ENVIRONMENT
            TicketType.OTHER -> FeedbackCategory.SUGGESTION
        }
        val combinedContent = if (title.isNotBlank()) "[$title] $description" else description
        submitFeedback(category = category, content = combinedContent).map {
            _tickets.value.firstOrNull() ?: TicketItem(
                id = it.id,
                code = "FB-${it.id.takeLast(4).uppercase()}",
                type = type,
                title = category.label,
                description = combinedContent,
                createdDate = it.createdAt,
                status = com.example.data.model.TicketStatus.PENDING,
                requesterName = _workerProfile.value.fullName,
                requesterCode = _workerProfile.value.employeeCode
            )
        }
    }

    // ==================== 14-16. DYNAMIC REFERRAL PROGRAM ====================

    /**
     * Endpoint 14: GET /api/referrals/program
     */
    suspend fun fetchReferralProgram(): Result<ReferralProgram> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getReferralProgram()
            if (response.isSuccessful && response.data != null) {
                val program = response.data.resolvedProgram()
                _referralProgram.value = program
                Result.success(program)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không lấy được thông tin chương trình giới thiệu"))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching referral program: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không lấy được thông tin chương trình giới thiệu"))
        }
    }

    /**
     * Endpoint 15: POST /api/referrals
     * Header: Idempotency-Key: <UUID>
     */
    suspend fun submitReferral(
        referredName: String,
        referredPhone: String,
        jobInterest: String? = null,
        consentToContact: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val idempotencyKey = UUID.randomUUID().toString()
            val req = ReferralSubmitRequest(
                referredName = referredName,
                referredPhone = referredPhone,
                jobInterest = jobInterest?.takeIf { it.isNotBlank() },
                consentToContact = consentToContact
            )
            val response = api.submitReferral(
                idempotencyKey = idempotencyKey,
                req = req
            )
            if (response.isSuccessful) {
                val refId = response.data?.referral?.id ?: ""
                fetchMyReferrals()
                Result.success(refId)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Gửi lời giới thiệu thất bại"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Không thể gửi thông tin giới thiệu"))
        }
    }

    /**
     * Endpoint 16: GET /api/referrals/mine
     */
    suspend fun fetchMyReferrals(page: Int = 1, limit: Int = 20): Result<List<ReferralItem>> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.getMyReferrals(page = page, limit = limit)
            if (response.isSuccessful && response.data != null) {
                val items = response.data.items.map { it.toDomain() }
                _referralList.value = items
                Result.success(items)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Không tải được danh sách giới thiệu"))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching referrals: ${t.message}")
            Result.failure(ApiErrorParser.parseThrowable(t, "Không tải được danh sách giới thiệu"))
        }
    }

    // ==================== 17. PUSH DEVICE REGISTRATION ====================

    /**
     * Endpoint 17: POST /api/push/register-device
     */
    suspend fun registerDeviceToken(fcmToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val api = apiService ?: return@withContext Result.failure(Exception("Dịch vụ mạng chưa khởi tạo"))
            val response = api.registerDevice(DeviceRegistrationRequest(fcmToken = fcmToken, platform = "ANDROID"))
            if (response.isSuccessful) {
                Result.success(response.data?.registered ?: true)
            } else {
                Result.failure(ApiErrorParser.fromApiError(response.error, fallbackMessage = "Đăng ký thiết bị nhận thông báo thất bại"))
            }
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.parseThrowable(t, "Đăng ký thiết bị nhận thông báo thất bại"))
        }
    }

    // ==================== DEFERRED / REMOVED ACTIONS ====================

    suspend fun fetchPayslip(): Result<PayslipSummary> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Phiếu lương tạm thời chưa khả dụng trong giai đoạn này"))
    }

    suspend fun fetchCandidates(): Result<List<CtvCandidate>> = withContext(Dispatchers.IO) {
        _ctvCandidates.value = emptyList()
        Result.success(emptyList())
    }

    suspend fun fetchBodOverview(): Result<BodOverviewStats> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Không có quyền truy cập dữ liệu ban giám đốc"))
    }

    suspend fun approveTicket(ticketId: String, note: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Thao tác duyệt không hỗ trợ trên ứng dụng mobile"))
    }

    suspend fun markTicketPaid(ticketId: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Thao tác giải ngân không hỗ trợ trên ứng dụng mobile"))
    }

    suspend fun rejectTicket(ticketId: String, reason: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Thao tác từ chối không hỗ trợ trên ứng dụng mobile"))
    }

    suspend fun approveAllAttendanceToday(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Thao tác duyệt bảng công không hỗ trợ trên ứng dụng mobile"))
    }

    suspend fun registerCandidate(
        candidateName: String,
        phone: String,
        jobTitle: String
    ): Result<CtvCandidate> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Chức năng CTV nội bộ tạm ẩn trong chặng này"))
    }
}
