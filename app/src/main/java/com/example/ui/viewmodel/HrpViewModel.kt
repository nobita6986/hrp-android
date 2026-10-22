package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceDay
import com.example.data.model.BodOverviewStats
import com.example.data.model.CtvCandidate
import com.example.data.model.FeedbackCategory
import com.example.data.model.FeedbackItem
import com.example.data.model.JobOpening
import com.example.data.model.PayslipSummary
import com.example.data.model.PublicProject
import com.example.data.model.ReferralItem
import com.example.data.model.ReferralProgram
import com.example.data.model.TicketItem
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.data.model.WorkerVerificationInfo
import com.example.data.repository.HrpRepository
import com.example.utils.GpsLocationResult
import com.example.utils.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String) {
    HOME("Tổng quan"),
    JOBS("Việc làm"),
    ATTENDANCE("Bảng công"),
    FEEDBACK("Góp ý"),
    REFERRAL("Giới thiệu")
}

class HrpViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository: HrpRepository = HrpRepository.getInstance(application)
    val locationHelper: LocationHelper = LocationHelper(application)
    val tokenManager: TokenManager = TokenManager.getInstance(application)

    // ==================== AUTH STATE ====================
    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage.asStateFlow()

    // ==================== OTP & VERIFICATION DIALOG STATE ====================
    private val _showPhoneOtpDialog = MutableStateFlow(false)
    val showPhoneOtpDialog: StateFlow<Boolean> = _showPhoneOtpDialog.asStateFlow()

    private val _otpCooldownSeconds = MutableStateFlow(0)
    val otpCooldownSeconds: StateFlow<Int> = _otpCooldownSeconds.asStateFlow()

    private val _otpErrorMessage = MutableStateFlow<String?>(null)
    val otpErrorMessage: StateFlow<String?> = _otpErrorMessage.asStateFlow()

    private val _isSendingOtp = MutableStateFlow(false)
    val isSendingOtp: StateFlow<Boolean> = _isSendingOtp.asStateFlow()

    private val _isVerifyingOtp = MutableStateFlow(false)
    val isVerifyingOtp: StateFlow<Boolean> = _isVerifyingOtp.asStateFlow()

    private var otpTimerJob: Job? = null

    private val _showWorkerVerificationDialog = MutableStateFlow(false)
    val showWorkerVerificationDialog: StateFlow<Boolean> = _showWorkerVerificationDialog.asStateFlow()

    private val _isSubmittingVerification = MutableStateFlow(false)
    val isSubmittingVerification: StateFlow<Boolean> = _isSubmittingVerification.asStateFlow()

    private val _isSubmittingReferral = MutableStateFlow(false)
    val isSubmittingReferral: StateFlow<Boolean> = _isSubmittingReferral.asStateFlow()

    private val _isSubmittingApplication = MutableStateFlow(false)
    val isSubmittingApplication: StateFlow<Boolean> = _isSubmittingApplication.asStateFlow()

    // Last recorded GPS check-in info
    private val _lastGpsCheckIn = MutableStateFlow<GpsLocationResult?>(null)
    val lastGpsCheckIn: StateFlow<GpsLocationResult?> = _lastGpsCheckIn.asStateFlow()

    // ==================== CORE DATA FLOWS ====================
    val currentRole: StateFlow<UserRole> = repository.currentRole
    val workerProfile: StateFlow<WorkerProfile> = repository.workerProfile
    val verificationInfo: StateFlow<WorkerVerificationInfo?> = repository.verificationInfo
    val publicProjects: StateFlow<List<PublicProject>> = repository.publicProjects
    val attendanceList: StateFlow<List<AttendanceDay>> = repository.attendanceList
    val lastCheckInEpochMs: StateFlow<Long> = repository.lastCheckInEpochMs
    val lastCheckOutEpochMs: StateFlow<Long> = repository.lastCheckOutEpochMs
    val payslip: StateFlow<PayslipSummary> = repository.payslip
    val tickets: StateFlow<List<TicketItem>> = repository.tickets
    val feedbackList: StateFlow<List<FeedbackItem>> = repository.feedbackList
    val referralProgram: StateFlow<ReferralProgram?> = repository.referralProgram
    val referralList: StateFlow<List<ReferralItem>> = repository.referralList
    val jobListings: StateFlow<List<JobOpening>> = repository.jobListings
    val ctvCandidates: StateFlow<List<CtvCandidate>> = repository.ctvCandidates
    val bodMetrics: StateFlow<BodOverviewStats> = repository.bodMetrics

    private val _currentTab = MutableStateFlow(
        if (_isLoggedIn.value) NavigationTab.HOME else NavigationTab.JOBS
    )
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Dialog toggles
    private val _showCreateTicketDialog = MutableStateFlow(false)
    val showCreateTicketDialog: StateFlow<Boolean> = _showCreateTicketDialog.asStateFlow()

    private val _showAdvanceSalaryDialog = MutableStateFlow(false)
    val showAdvanceSalaryDialog: StateFlow<Boolean> = _showAdvanceSalaryDialog.asStateFlow()

    private val _showCheckInSuccessDialog = MutableStateFlow(false)
    val showCheckInSuccessDialog: StateFlow<Boolean> = _showCheckInSuccessDialog.asStateFlow()

    private val _showApplyJobDialog = MutableStateFlow<JobOpening?>(null)
    val showApplyJobDialog: StateFlow<JobOpening?> = _showApplyJobDialog.asStateFlow()

    private val _selectedJobDetail = MutableStateFlow<JobOpening?>(null)
    val selectedJobDetail: StateFlow<JobOpening?> = _selectedJobDetail.asStateFlow()

    private val _isLoadingJobDetail = MutableStateFlow(false)
    val isLoadingJobDetail: StateFlow<Boolean> = _isLoadingJobDetail.asStateFlow()

    init {
        repository.initServices(application)
        if (_isLoggedIn.value) {
            _currentTab.value = NavigationTab.HOME
            viewModelScope.launch {
                repository.loadInitialData()
            }
        } else {
            _currentTab.value = NavigationTab.JOBS
            viewModelScope.launch {
                repository.fetchJobs()
            }
        }
    }

    /**
     * Ensures that whenever a logged-in user enters/re-opens the app,
     * the Overview (Tổng quan) screen with the unified Check-in button is shown first.
     */
    fun onAppEntered() {
        if (_isLoggedIn.value) {
            _selectedJobDetail.value = null
            _isLoadingJobDetail.value = false
            _currentTab.value = NavigationTab.HOME
            viewModelScope.launch {
                repository.fetchProfile()
                if (workerProfile.value.canCheckIn || workerProfile.value.hasActiveAssignment) {
                    repository.fetchAttendance()
                }
            }
        }
    }

    // ==================== AUTH & REGISTER METHODS ====================

    fun setLoginScreenVisible(visible: Boolean) {
        _showLoginScreen.value = visible
        if (!visible) {
            _loginErrorMessage.value = null
        }
    }

    fun login(phone: String, pass: String) {
        if (_isAuthenticating.value) return
        if (phone.isBlank() || pass.isBlank()) {
            _loginErrorMessage.value = "Vui lòng nhập đầy đủ Số điện thoại và Mật khẩu"
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginErrorMessage.value = null

            val result = repository.login(phone.trim(), pass.trim())
            _isAuthenticating.value = false

            result.onSuccess {
                _isLoggedIn.value = true
                _showLoginScreen.value = false
                _currentTab.value = NavigationTab.HOME
                _toastMessage.value = "Đăng nhập thành công! Chào mừng trở lại."
            }.onFailure { err ->
                _loginErrorMessage.value = err.message ?: "Đăng nhập thất bại. Vui lòng thử lại."
            }
        }
    }

    fun register(phone: String, pass: String, fullName: String) {
        if (_isAuthenticating.value) return
        val trimmedName = fullName.trim()

        if (trimmedName.isBlank() || phone.isBlank() || pass.isBlank()) {
            _loginErrorMessage.value = "Vui lòng nhập đầy đủ Họ tên, Số điện thoại và Mật khẩu"
            return
        }
        if (!com.example.data.repository.HrpFormatters.isValidStrictVnPhone(phone)) {
            _loginErrorMessage.value = "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0 (không dùng +84, khoảng trắng hoặc dấu phân cách)"
            return
        }
        if (pass.length < 8) {
            _loginErrorMessage.value = "Mật khẩu cần tối thiểu 8 ký tự"
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginErrorMessage.value = null

            val registerResult = repository.register(phone, pass, trimmedName)
            registerResult.onSuccess { data ->
                if (!data.token.isNullOrBlank() || tokenManager.isLoggedIn()) {
                    _isAuthenticating.value = false
                    _currentTab.value = NavigationTab.HOME
                    _isLoggedIn.value = true
                    _showLoginScreen.value = false
                    if (!data.phoneVerified) {
                        _otpErrorMessage.value = null
                        _showPhoneOtpDialog.value = true
                    }
                    _toastMessage.value = "Đăng ký tài khoản thành công! Vui lòng xác minh OTP cho số điện thoại ${data.phone.ifBlank { phone }}."
                } else {
                    val loginResult = repository.login(phone, pass)
                    _isAuthenticating.value = false
                    loginResult.onSuccess {
                        _currentTab.value = NavigationTab.HOME
                        _isLoggedIn.value = true
                        _showLoginScreen.value = false
                        if (!workerProfile.value.phoneVerified) {
                            _otpErrorMessage.value = null
                            _showPhoneOtpDialog.value = true
                        }
                        _toastMessage.value = "Đăng ký tài khoản thành công!"
                    }.onFailure {
                        _toastMessage.value = "Đăng ký thành công! Vui lòng đăng nhập."
                    }
                }
            }.onFailure { err ->
                _isAuthenticating.value = false
                _loginErrorMessage.value = err.message ?: "Đăng ký tài khoản thất bại."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            otpTimerJob?.cancel()
            _otpCooldownSeconds.value = 0
            _showPhoneOtpDialog.value = false
            _otpErrorMessage.value = null
            repository.logout()
            _isLoggedIn.value = false
            _showLoginScreen.value = false
            _currentTab.value = NavigationTab.JOBS
            repository.fetchJobs()
            _toastMessage.value = "Đã đăng xuất khỏi tài khoản. Bạn vẫn có thể xem việc làm và ứng tuyển."
        }
    }

    fun clearLoginError() {
        _loginErrorMessage.value = null
    }

    // ==================== PHONE OTP VERIFICATION ====================

    fun setPhoneOtpDialogVisible(visible: Boolean) {
        _showPhoneOtpDialog.value = visible
        if (!visible) {
            _otpErrorMessage.value = null
        }
    }

    private fun formatOtpUiError(err: Throwable, fallback: String): String {
        val baseMsg = err.message?.takeIf { it.isNotBlank() } ?: fallback
        val apiErr = err as? com.example.data.api.model.HrpApiException
        val code = apiErr?.code?.takeIf { it.isNotBlank() && it != "NETWORK_ERROR" }
        return if (code != null && !baseMsg.contains(code, ignoreCase = true)) {
            "$baseMsg ($code)"
        } else {
            baseMsg
        }
    }

    fun sendPhoneOtp(phone: String = workerProfile.value.phone) {
        val accountPhone = workerProfile.value.phone.trim().ifBlank { tokenManager.getPhone()?.trim().orEmpty() }
        val targetPhone = phone.trim().ifBlank { accountPhone }
        if (accountPhone.isBlank() || targetPhone.isBlank()) {
            _otpErrorMessage.value = "Không tìm thấy số điện thoại trong tài khoản để gửi mã OTP."
            return
        }
        if (targetPhone != accountPhone) {
            _otpErrorMessage.value = "Số điện thoại gửi OTP phải khớp với số điện thoại trong tài khoản ($accountPhone)."
            return
        }
        if (!com.example.data.repository.HrpFormatters.isValidStrictVnPhone(targetPhone)) {
            _otpErrorMessage.value = "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0."
            return
        }

        viewModelScope.launch {
            _isSendingOtp.value = true
            _otpErrorMessage.value = null

            val result = repository.sendPhoneOtp(targetPhone)
            _isSendingOtp.value = false

            result.onSuccess { data ->
                if (data.phoneVerified) {
                    _showPhoneOtpDialog.value = false
                    _toastMessage.value = "Số điện thoại $targetPhone đã được xác thực!"
                    repository.fetchProfile()
                } else {
                    startOtpCooldown(data.cooldownSeconds.coerceAtLeast(1))
                    _toastMessage.value = "Đã gửi mã OTP 6 chữ số tới số điện thoại $targetPhone"
                }
            }.onFailure { err ->
                // Strictly surface real backend errors (e.g., 503 OTP_SERVICE_UNAVAILABLE, 429 RATE_LIMIT_EXCEEDED)
                _otpErrorMessage.value = formatOtpUiError(err, "Không thể gửi mã OTP lúc này.")
            }
        }
    }

    fun verifyPhoneOtp(code: String) {
        val trimmedCode = code.trim()
        if (!com.example.data.repository.HrpFormatters.isValidOtpCode(trimmedCode)) {
            _otpErrorMessage.value = "Vui lòng nhập đúng mã OTP gồm 6 chữ số"
            return
        }

        viewModelScope.launch {
            _isVerifyingOtp.value = true
            _otpErrorMessage.value = null

            val result = repository.verifyPhoneOtp(trimmedCode)
            _isVerifyingOtp.value = false

            result.onSuccess {
                _showPhoneOtpDialog.value = false
                _toastMessage.value = "Xác thực số điện thoại thành công!"
                repository.fetchProfile()
            }.onFailure { err ->
                _otpErrorMessage.value = formatOtpUiError(err, "Mã OTP không chính xác hoặc đã hết hạn.")
            }
        }
    }

    private fun startOtpCooldown(seconds: Int) {
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            _otpCooldownSeconds.value = seconds
            while (_otpCooldownSeconds.value > 0) {
                delay(1000L)
                _otpCooldownSeconds.value -= 1
            }
        }
    }

    // ==================== WORKER VERIFICATION (HRP) ====================

    fun setWorkerVerificationDialogVisible(visible: Boolean) {
        _showWorkerVerificationDialog.value = visible
        if (visible) {
            viewModelScope.launch {
                repository.fetchPublicProjects()
                repository.fetchMyVerification()
            }
        }
    }

    fun refreshVerificationAndProfile() {
        viewModelScope.launch {
            repository.fetchProfile()
            repository.fetchAttendance()
        }
    }

    fun submitWorkerVerification(
        fullName: String,
        cccdNumber: String,
        dob: String,
        projectId: String,
        employeeCode: String
    ) {
        if (fullName.isBlank() || cccdNumber.isBlank() || dob.isBlank() || projectId.isBlank() || employeeCode.isBlank()) {
            _toastMessage.value = "Vui lòng điền đầy đủ thông tin xác thực công nhân"
            return
        }

        viewModelScope.launch {
            _isSubmittingVerification.value = true
            val result = repository.submitWorkerVerification(
                fullName = fullName.trim(),
                cccdNumber = cccdNumber.trim(),
                dob = dob.trim(),
                projectId = projectId.trim(),
                employeeCode = employeeCode.trim()
            )
            _isSubmittingVerification.value = false

            result.onSuccess {
                _showWorkerVerificationDialog.value = false
                _toastMessage.value = "Đã gửi yêu cầu xác thực công nhân! Vui lòng chờ HR đối soát."
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Gửi hồ sơ xác thực thất bại"
            }
        }
    }

    // ==================== NAVIGATION & DIALOGS ====================

    fun selectTab(tab: NavigationTab) {
        if (tab != NavigationTab.JOBS) {
            _selectedJobDetail.value = null
            _isLoadingJobDetail.value = false
        }
        _currentTab.value = tab
        if (!_isLoggedIn.value) {
            if (tab == NavigationTab.JOBS) {
                viewModelScope.launch {
                    repository.fetchJobs()
                }
            }
            return
        }
        viewModelScope.launch {
            when (tab) {
                NavigationTab.HOME -> {
                    repository.fetchProfile()
                    if (workerProfile.value.canCheckIn || workerProfile.value.hasActiveAssignment) {
                        repository.fetchAttendance()
                    }
                }
                NavigationTab.JOBS -> repository.fetchJobs()
                NavigationTab.ATTENDANCE -> {
                    repository.fetchProfile()
                    if (workerProfile.value.canCheckIn || workerProfile.value.hasActiveAssignment) {
                        repository.fetchAttendance()
                    }
                }
                NavigationTab.FEEDBACK -> {
                    if (workerProfile.value.phoneVerified) {
                        repository.fetchMyFeedback()
                    }
                }
                NavigationTab.REFERRAL -> {
                    repository.fetchReferralProgram()
                    if (workerProfile.value.canCheckIn || workerProfile.value.hasActiveAssignment || workerProfile.value.verificationStatus == VerificationStatus.APPROVED) {
                        repository.fetchMyReferrals()
                    }
                }
            }
        }
    }

    fun setCreateTicketDialogVisible(visible: Boolean) {
        _showCreateTicketDialog.value = visible
    }

    fun setAdvanceSalaryDialogVisible(visible: Boolean) {
        _showAdvanceSalaryDialog.value = visible
    }

    fun setCheckInSuccessVisible(visible: Boolean) {
        _showCheckInSuccessDialog.value = visible
    }

    fun openApplyJob(job: JobOpening?) {
        _showApplyJobDialog.value = job
    }

    fun openJobDetail(job: JobOpening?) {
        if (job == null) {
            _selectedJobDetail.value = null
            _isLoadingJobDetail.value = false
            return
        }
        _selectedJobDetail.value = job
        if (job.isDetailLoaded) {
            _isLoadingJobDetail.value = false
            return
        }
        viewModelScope.launch {
            _isLoadingJobDetail.value = true
            val result = repository.fetchJobDetail(job)
            _isLoadingJobDetail.value = false
            result.onSuccess { detailed ->
                if (_selectedJobDetail.value?.slug == job.slug || _selectedJobDetail.value?.id == job.id) {
                    _selectedJobDetail.value = detailed
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==================== WORKER ATTENDANCE & GPS CHECK-IN ====================

    /**
     * Executes check-in with real hardware GPS verification via POST /api/worker/checkins/events
     */
    fun checkIn(location: GpsLocationResult? = null) {
        viewModelScope.launch {
            val siteName = location?.matchedSite ?: workerProfile.value.factoryLocation
            val lat = location?.latitude
            val lng = location?.longitude
            val accuracy = location?.accuracyMeters

            _lastGpsCheckIn.value = location

            val result = repository.performCheckIn(
                site = siteName,
                latitude = lat,
                longitude = lng,
                accuracy = accuracy
            )

            result.onSuccess {
                _showCheckInSuccessDialog.value = true
                val accuracyInfo = if (accuracy != null) " (Sai số GPS: ±%.1fm)".format(accuracy) else ""
                _toastMessage.value = "Chấm công vào ca thành công qua GPS!$accuracyInfo"
            }.onFailure {
                _toastMessage.value = "Không thể chấm công: ${it.message}"
            }
        }
    }

    /**
     * Executes check-out with real hardware GPS verification via POST /api/worker/checkins/events
     */
    fun checkOut(location: GpsLocationResult? = null) {
        viewModelScope.launch {
            val result = repository.performCheckOut(
                latitude = location?.latitude,
                longitude = location?.longitude,
                accuracy = location?.accuracyMeters
            )
            result.onSuccess {
                _toastMessage.value = "Chấm công tan ca thành công qua GPS!"
            }.onFailure {
                _toastMessage.value = "Không thể tan ca: ${it.message}"
            }
        }
    }

    // ==================== FEEDBACK / TICKETS ====================

    fun submitFeedback(category: FeedbackCategory, content: String) {
        if (!workerProfile.value.phoneVerified) {
            _toastMessage.value = "Tài khoản chưa xác thực số điện thoại qua OTP. Cần xác thực OTP trước khi gửi góp ý."
            return
        }
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            _toastMessage.value = "Vui lòng nhập nội dung góp ý"
            return
        }
        if (trimmed.length < 10) {
            _toastMessage.value = "Nội dung góp ý phải có ít nhất 10 ký tự"
            return
        }
        viewModelScope.launch {
            val result = repository.submitFeedback(category, trimmed)

            result.onSuccess {
                _showCreateTicketDialog.value = false
                _showAdvanceSalaryDialog.value = false
                _toastMessage.value = "Đã gửi góp ý (${category.label}) thành công!"
            }.onFailure {
                _toastMessage.value = "Lỗi gửi góp ý: ${it.message}"
            }
        }
    }

    fun submitTicket(
        type: TicketType,
        title: String,
        description: String,
        amountVnd: Long? = null,
        requestedDays: String? = null
    ) {
        if (!workerProfile.value.phoneVerified) {
            _toastMessage.value = "Tài khoản chưa xác thực số điện thoại qua OTP. Cần xác thực OTP trước khi gửi góp ý."
            return
        }
        viewModelScope.launch {
            val result = repository.createTicket(type, title, description, amountVnd, requestedDays)

            result.onSuccess {
                _showCreateTicketDialog.value = false
                _showAdvanceSalaryDialog.value = false
                _toastMessage.value = "Đã gửi yêu cầu ${type.label} thành công!"
            }.onFailure {
                _toastMessage.value = "Lỗi khi gửi yêu cầu: ${it.message}"
            }
        }
    }

    // ==================== PUBLIC JOB APPLICATION ====================

    fun submitApplication(
        jobSlugOrTitle: String,
        candidateName: String,
        phone: String,
        consent: Boolean = true
    ) {
        if (_isSubmittingApplication.value) return
        if (!_isLoggedIn.value) {
            val trimmedName = candidateName.trim()
            val trimmedPhone = phone.trim()
            if (trimmedName.isBlank() || trimmedPhone.isBlank()) {
                _toastMessage.value = "Vui lòng nhập đầy đủ Họ và tên và Số điện thoại liên hệ"
                return
            }
            if (!com.example.data.repository.HrpFormatters.isValidStrictVnPhone(trimmedPhone)) {
                _toastMessage.value = "Số điện thoại liên hệ phải gồm đúng 10 chữ số, bắt đầu bằng 0"
                return
            }
        }
        val currentJob = _showApplyJobDialog.value
        val slug = currentJob?.slug?.takeIf { it.isNotBlank() } ?: jobSlugOrTitle

        viewModelScope.launch {
            _isSubmittingApplication.value = true
            val result = repository.applyToJob(
                slug = slug,
                fullName = candidateName.trim(),
                phone = phone.trim(),
                consent = consent
            )
            _isSubmittingApplication.value = false
            _showApplyJobDialog.value = null
            result.onSuccess { resp ->
                val trackingInfo = resp.trackingCode?.let { " (Mã hồ sơ: $it)" } ?: ""
                _toastMessage.value = "Nộp hồ sơ ứng tuyển thành công!$trackingInfo"
            }.onFailure {
                _toastMessage.value = "Lỗi nộp hồ sơ: ${it.message}"
            }
        }
    }

    // ==================== DYNAMIC REFERRAL PROGRAM ====================

    fun refreshReferralData() {
        viewModelScope.launch {
            repository.fetchReferralProgram()
            repository.fetchMyReferrals()
        }
    }

    fun submitReferral(
        referredName: String,
        referredPhone: String,
        jobInterest: String?,
        consentToContact: Boolean
    ) {
        if (referredName.isBlank() || referredPhone.isBlank()) {
            _toastMessage.value = "Vui lòng nhập đầy đủ họ tên và số điện thoại người được giới thiệu"
            return
        }
        if (!consentToContact) {
            _toastMessage.value = "Vui lòng xác nhận người được giới thiệu đã đồng ý để HRP liên hệ"
            return
        }

        viewModelScope.launch {
            _isSubmittingReferral.value = true
            val result = repository.submitReferral(
                referredName = referredName.trim(),
                referredPhone = referredPhone.trim(),
                jobInterest = jobInterest?.trim(),
                consentToContact = consentToContact
            )
            _isSubmittingReferral.value = false

            result.onSuccess {
                _toastMessage.value = "Đã gửi thông tin giới thiệu thành công! Bộ phận tuyển dụng HRP sẽ liên hệ ứng viên."
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Gửi lời giới thiệu thất bại"
            }
        }
    }
}
