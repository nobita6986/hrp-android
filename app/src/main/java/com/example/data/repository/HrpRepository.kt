package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.HrpApiService
import com.example.data.api.model.CandidateRegistrationRequest
import com.example.data.api.model.CheckInRequest
import com.example.data.api.model.CheckOutRequest
import com.example.data.api.model.CreateTicketRequest
import com.example.data.api.model.LoginRequest
import com.example.data.api.model.LoginResponse
import com.example.data.api.model.UpdateTicketStatusRequest
import com.example.data.local.TokenManager
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.BodOverviewStats
import com.example.data.model.CtvCandidate
import com.example.data.model.JobOpening
import com.example.data.model.PayslipSummary
import com.example.data.model.TicketItem
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.data.model.WorkerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

object HrpFormatters {
    private val vnLocale = Locale("vi", "VN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(vnLocale)

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
            status = "PENDING_VERIFICATION"
        )
    )
    val workerProfile: StateFlow<WorkerProfile> = _workerProfile.asStateFlow()

    private val _attendanceList = MutableStateFlow<List<AttendanceDay>>(emptyList())
    val attendanceList: StateFlow<List<AttendanceDay>> = _attendanceList.asStateFlow()

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

    // ==================== AUTH ACTIONS ====================

    suspend fun login(phone: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.login(LoginRequest(phone, password))
            val hasSession = !response?.token.isNullOrBlank() || tokenManager?.isLoggedIn() == true
            if (response != null && (response.success || response.token != null || hasSession)) {
                val roleStr = response.role ?: "WORKER"
                if (!roleStr.equals("WORKER", ignoreCase = true)) {
                    tokenManager?.clearToken()
                    return@withContext Result.failure(
                        Exception("Ứng dụng chỉ dành cho Người lao động. Tài khoản có vai trò '$roleStr' không được phép đăng nhập trên mobile.")
                    )
                }

                val token = response.token ?: tokenManager?.getToken() ?: "session_valid"
                tokenManager?.saveUserSession(
                    token = token,
                    phone = phone,
                    role = "WORKER",
                    fullName = response.user?.fullName ?: "Người Lao Động"
                )
                _currentRole.value = UserRole.WORKER

                response.user?.let {
                    _workerProfile.value = it.toDomain()
                }

                // Refresh app data after successful login
                loadInitialData()

                Result.success(response)
            } else {
                // Return real error from backend response
                Result.failure(Exception(response?.message ?: "Sai số điện thoại hoặc mật khẩu"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi máy chủ: ${e.localizedMessage ?: "Không thể kết nối tới backend"}"))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            tokenManager?.clearToken()
        } catch (_: Exception) {
        }
    }

    // ==================== DATA SYNC / REFRESH ====================

    suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        fetchProfile()
        fetchAttendance()
        fetchTickets()
        fetchJobs()
    }

    suspend fun fetchProfile(): Result<WorkerProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getProfile()
            if (response != null) {
                val domain = response.toDomain()
                _workerProfile.value = domain
                Result.success(domain)
            } else {
                Result.failure(Exception("Không lấy được thông tin hồ sơ"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchAttendance(): Result<List<AttendanceDay>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getAttendanceList()
            if (response != null) {
                val domainList = response.map { it.toDomain() }
                _attendanceList.value = domainList
                Result.success(domainList)
            } else {
                Result.failure(Exception("Không lấy được dữ liệu chấm công"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchPayslip(): Result<PayslipSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getPayslip()
            if (response != null) {
                val domain = response.toDomain()
                _payslip.value = domain
                Result.success(domain)
            } else {
                Result.failure(Exception("Phiếu lương tạm thời chưa khả dụng"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payslip: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchTickets(): Result<List<TicketItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getTickets()
            if (response != null) {
                val domainList = response.map { it.toDomain() }
                _tickets.value = domainList
                Result.success(domainList)
            } else {
                Result.failure(Exception("Không lấy được danh sách góp ý"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tickets: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchJobs(): Result<List<JobOpening>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getJobListings()
            if (response != null) {
                val domainList = response.map { it.toDomain() }
                _jobListings.value = domainList
                Result.success(domainList)
            } else {
                Result.failure(Exception("Không lấy được danh sách việc làm"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching jobs: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchCandidates(): Result<List<CtvCandidate>> = withContext(Dispatchers.IO) {
        _ctvCandidates.value = emptyList()
        Result.success(emptyList())
    }

    suspend fun fetchBodOverview(): Result<BodOverviewStats> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Không có quyền truy cập dữ liệu ban giám đốc"))
    }

    // ==================== WORKER ACTIONS ====================

    suspend fun performCheckIn(
        site: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        if (latitude == null || longitude == null) {
            return@withContext Result.failure(Exception("Yêu cầu định vị GPS thực tế từ thiết bị để chấm công."))
        }

        try {
            val req = CheckInRequest(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                factorySite = site,
                note = "Vào ca qua định vị phần cứng GPS"
            )
            val apiResult = apiService?.checkIn(req)
            val dayResult = apiResult?.attendance?.toDomain()

            if (dayResult != null) {
                val current = _attendanceList.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.dateString == dayResult.dateString }
                if (existingIndex >= 0) {
                    current[existingIndex] = dayResult
                } else {
                    current.add(0, dayResult)
                }
                _attendanceList.value = current
                Result.success(dayResult)
            } else {
                Result.failure(Exception(apiResult?.message ?: "Chấm công thất bại từ máy chủ"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi chấm công: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun performCheckOut(
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        if (latitude == null || longitude == null) {
            return@withContext Result.failure(Exception("Yêu cầu định vị GPS thực tế từ thiết bị để kết thúc ca."))
        }

        try {
            val apiResult = apiService?.checkOut(CheckOutRequest(latitude, longitude))
            Result.failure(Exception(apiResult?.message ?: "Chức năng tan ca chưa có phản hồi từ máy chủ"))
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi máy chủ khi tan ca: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun createTicket(
        type: TicketType,
        title: String,
        description: String,
        amountVnd: Long? = null,
        requestedDays: String? = null
    ): Result<TicketItem> = withContext(Dispatchers.IO) {
        try {
            val req = CreateTicketRequest(
                type = type.name,
                title = title,
                description = description,
                amountVnd = amountVnd,
                requestedDays = requestedDays
            )
            val apiResult = apiService?.createTicket(req)
            val newTicket = apiResult?.toDomain()

            if (newTicket != null) {
                _tickets.value = listOf(newTicket) + _tickets.value
                Result.success(newTicket)
            } else {
                Result.failure(Exception("Gửi yêu cầu thất bại từ máy chủ"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi gửi yêu cầu: ${e.localizedMessage ?: e.message}"))
        }
    }

    // ==================== DEFERRED / REMOVED HR & CTV ACTIONS ====================

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
