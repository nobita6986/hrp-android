package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceDay
import com.example.data.model.BodOverviewStats
import com.example.data.model.CtvCandidate
import com.example.data.model.JobOpening
import com.example.data.model.PayslipSummary
import com.example.data.model.TicketItem
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.data.model.WorkerProfile
import com.example.data.repository.HrpRepository
import com.example.utils.GpsLocationResult
import com.example.utils.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String) {
    HOME("Tổng quan"),
    JOBS("Việc làm"),
    ATTENDANCE("Chấm công"),
    FEEDBACK("Góp ý"),
    REFERRAL("Giới thiệu")
}

class HrpViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository: HrpRepository = HrpRepository.getInstance(application)
    val locationHelper: LocationHelper = LocationHelper(application)
    val tokenManager: TokenManager = TokenManager.getInstance(application)

    init {
        repository.initServices(application)
    }

    // ==================== AUTH STATE ====================
    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage.asStateFlow()

    // Last recorded GPS check-in info
    private val _lastGpsCheckIn = MutableStateFlow<GpsLocationResult?>(null)
    val lastGpsCheckIn: StateFlow<GpsLocationResult?> = _lastGpsCheckIn.asStateFlow()

    // ==================== CORE DATA FLOWS ====================
    val currentRole: StateFlow<UserRole> = repository.currentRole
    val workerProfile: StateFlow<WorkerProfile> = repository.workerProfile
    val attendanceList: StateFlow<List<AttendanceDay>> = repository.attendanceList
    val payslip: StateFlow<PayslipSummary> = repository.payslip
    val tickets: StateFlow<List<TicketItem>> = repository.tickets
    val jobListings: StateFlow<List<JobOpening>> = repository.jobListings
    val ctvCandidates: StateFlow<List<CtvCandidate>> = repository.ctvCandidates
    val bodMetrics: StateFlow<BodOverviewStats> = repository.bodMetrics

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
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

    // ==================== AUTH METHODS ====================

    fun login(phone: String, pass: String) {
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
                _toastMessage.value = "Đăng nhập thành công! Chào mừng trở lại."
            }.onFailure { err ->
                _loginErrorMessage.value = err.message ?: "Đăng nhập thất bại. Vui lòng thử lại."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _currentTab.value = NavigationTab.HOME
            _toastMessage.value = "Đã đăng xuất khỏi tài khoản."
        }
    }

    fun clearLoginError() {
        _loginErrorMessage.value = null
    }

    // ==================== NAVIGATION & DIALOGS ====================

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
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

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==================== WORKER ACTIONS ====================

    /**
     * Executes check-in with GPS verification
     */
    fun checkIn(location: GpsLocationResult? = null) {
        viewModelScope.launch {
            val siteName = location?.matchedSite ?: "Cổng B - Samsung SEVT (Thái Nguyên)"
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
                val accuracyInfo = if (accuracy != null) " (Độ chuẩn xác: %.1fm)".format(accuracy) else ""
                _toastMessage.value = "Chấm công vào ca thành công qua GPS!$accuracyInfo"
            }.onFailure {
                _toastMessage.value = "Lỗi khi chấm công: ${it.message}"
            }
        }
    }

    fun checkOut(location: GpsLocationResult? = null) {
        viewModelScope.launch {
            val result = repository.performCheckOut(
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            result.onSuccess {
                _toastMessage.value = "Chấm công kết thúc ca thành công! Đã ghi nhận 8.0 giờ công."
            }.onFailure {
                _toastMessage.value = "Lỗi khi kết thúc ca: ${it.message}"
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
        viewModelScope.launch {
            val result = repository.createTicket(type, title, description, amountVnd, requestedDays)
            _showCreateTicketDialog.value = false
            _showAdvanceSalaryDialog.value = false

            result.onSuccess {
                _toastMessage.value = "Đã gửi yêu cầu ${type.label} thành công!"
            }.onFailure {
                _toastMessage.value = "Lỗi khi gửi yêu cầu: ${it.message}"
            }
        }
    }

    // ==================== CANDIDATE / JOB APPLICATION ====================

    fun submitApplication(jobTitle: String, candidateName: String, phone: String) {
        viewModelScope.launch {
            val result = repository.registerCandidate(candidateName, phone, jobTitle)
            _showApplyJobDialog.value = null
            result.onSuccess {
                _toastMessage.value = "Nộp hồ sơ ứng tuyển thành công! Nhà máy sẽ liên hệ trong 24h."
            }.onFailure {
                _toastMessage.value = "Lỗi nộp hồ sơ: ${it.message}"
            }
        }
    }
}
