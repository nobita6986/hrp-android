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
            id = "w-0192",
            employeeCode = "HRP-8839",
            fullName = "Nguyễn Văn Hùng",
            cccdNumber = "034098012398",
            phone = "0987 654 321",
            factoryLocation = "Nhà máy Samsung SEVT - Thái Nguyên",
            projectCode = "PRJ-SAMS-01",
            department = "Phân xưởng Lắp Ráp Điện Tử Module C2",
            position = "Kỹ thuật viên đứng chuyền chính",
            baseSalaryPerDayVnd = 380_000L,
            joinedDate = "15/03/2023",
            bankAccount = "Techcombank - 19038291038"
        )
    )
    val workerProfile: StateFlow<WorkerProfile> = _workerProfile.asStateFlow()

    private val _attendanceList = MutableStateFlow(
        listOf(
            AttendanceDay(
                id = "att-18",
                dateString = "18/10/2026",
                dayOfWeek = "Chủ Nhật",
                shiftName = "Ca Sáng (08:00 - 17:00)",
                checkInTime = "07:51",
                checkOutTime = null,
                standardHours = 8.0,
                otHours = 2.0,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.PENDING,
                note = "Đăng ký làm thêm OT 2h tối"
            ),
            AttendanceDay(
                id = "att-17",
                dateString = "17/10/2026",
                dayOfWeek = "Thứ Bảy",
                shiftName = "Ca Sáng (08:00 - 17:00)",
                checkInTime = "07:48",
                checkOutTime = "19:05",
                standardHours = 8.0,
                otHours = 2.0,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.APPROVED,
                note = "Hoàn thành chỉ tiêu chuyền lắp ráp"
            ),
            AttendanceDay(
                id = "att-16",
                dateString = "16/10/2026",
                dayOfWeek = "Thứ Sáu",
                shiftName = "Ca Sáng (08:00 - 17:00)",
                checkInTime = "07:55",
                checkOutTime = "17:03",
                standardHours = 8.0,
                otHours = 0.0,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.APPROVED,
                note = "Đủ công chuẩn"
            ),
            AttendanceDay(
                id = "att-15",
                dateString = "15/10/2026",
                dayOfWeek = "Thứ Năm",
                shiftName = "Ca Đêm (20:00 - 05:00)",
                checkInTime = "19:50",
                checkOutTime = "05:10",
                standardHours = 8.0,
                otHours = 1.5,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.APPROVED,
                note = "Phụ cấp ca đêm 30%"
            ),
            AttendanceDay(
                id = "att-14",
                dateString = "14/10/2026",
                dayOfWeek = "Thứ Tư",
                shiftName = "Ca Sáng (08:00 - 17:00)",
                checkInTime = "07:50",
                checkOutTime = "17:00",
                standardHours = 8.0,
                otHours = 0.0,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.APPROVED
            ),
            AttendanceDay(
                id = "att-13",
                dateString = "13/10/2026",
                dayOfWeek = "Thứ Ba",
                shiftName = "Ca Sáng (08:00 - 17:00)",
                checkInTime = "08:15",
                checkOutTime = "17:02",
                standardHours = 7.5,
                otHours = 0.0,
                locationVerified = true,
                factorySite = "Cổng B - Samsung SEVT",
                status = AttendanceStatus.DISPUTED,
                note = "Vào trễ do kẹt xe đưa đón nhà máy (Đã gửi khiếu nại)"
            )
        )
    )
    val attendanceList: StateFlow<List<AttendanceDay>> = _attendanceList.asStateFlow()

    private val _payslip = MutableStateFlow(
        PayslipSummary(
            monthPeriod = "Tháng 09/2026",
            standardWorkDays = 26,
            actualWorkDays = 25.5,
            otHours = 34.0,
            baseSalaryVnd = 9_690_000L,
            otPayVnd = 2_470_000L,
            attendanceBonusVnd = 600_000L,
            mealAllowanceVnd = 780_000L,
            transportAllowanceVnd = 500_000L,
            grossSalaryVnd = 14_040_000L,
            socialInsuranceVnd = 775_200L,
            healthInsuranceVnd = 145_350L,
            unemploymentInsVnd = 96_900L,
            personalIncomeTaxVnd = 0L,
            advancePaidVnd = 2_000_000L,
            netSalaryVnd = 11_022_550L,
            isFinalized = true,
            payDate = "05/10/2026"
        )
    )
    val payslip: StateFlow<PayslipSummary> = _payslip.asStateFlow()

    private val _tickets = MutableStateFlow(
        listOf(
            TicketItem(
                id = "tk-101",
                code = "TK-ADV-09",
                type = TicketType.ADVANCE_SALARY,
                title = "Tạm ứng chi phí sinh hoạt giữa tháng",
                description = "Xin tạm ứng tiền để đóng học phí cho con và tiền nhà trọ tháng 10",
                amountVnd = 2_500_000L,
                createdDate = "14/10/2026",
                status = TicketStatus.HR_APPROVED,
                requesterName = "Nguyễn Văn Hùng",
                requesterCode = "HRP-8839",
                hrNote = "Đủ điều kiện làm việc trên 15 công. Đã duyệt chuyển Kế toán chi."
            ),
            TicketItem(
                id = "tk-102",
                code = "TK-DISP-04",
                type = TicketType.TIMESHEET_DISPUTE,
                title = "Phản ánh điểm danh trễ ngày 13/10",
                description = "Xe đưa đón số 14 tuyến Sông Công - SEVT bị thủng lốp dẫn đến cả xe vào cổng lúc 08:14. Kính nhờ HR xác nhận đủ công.",
                createdDate = "13/10/2026",
                status = TicketStatus.PENDING,
                requesterName = "Nguyễn Văn Hùng",
                requesterCode = "HRP-8839",
                hrNote = "Chờ xác nhận danh sách xe buýt từ ban vận tải."
            ),
            TicketItem(
                id = "tk-103",
                code = "TK-LEAVE-01",
                type = TicketType.LEAVE_REQUEST,
                title = "Xin nghỉ phép việc riêng (Về quê)",
                description = "Gia đình có việc hiếu, xin nghỉ phép 1 ngày 22/10",
                requestedDays = "22/10/2026 (1 ngày)",
                createdDate = "10/10/2026",
                status = TicketStatus.APPROVED,
                requesterName = "Nguyễn Văn Hùng",
                requesterCode = "HRP-8839",
                hrNote = "Quản lý ca đã duyệt và bố trí nhân sự thay thế."
            ),
            TicketItem(
                id = "tk-104",
                code = "TK-ADV-08",
                type = TicketType.ADVANCE_SALARY,
                title = "Tạm ứng viện phí khám bệnh",
                description = "Tạm ứng mua thuốc và kiểm tra sức khỏe",
                amountVnd = 1_500_000L,
                createdDate = "28/09/2026",
                status = TicketStatus.PAID,
                requesterName = "Trần Thị Mai",
                requesterCode = "HRP-7120",
                hrNote = "Kế toán đã giải ngân tiền mặt ngày 29/09."
            )
        )
    )
    val tickets: StateFlow<List<TicketItem>> = _tickets.asStateFlow()

    private val _jobListings = MutableStateFlow(
        listOf(
            JobOpening(
                id = "job-01",
                title = "Công nhân lắp ráp Module Camera",
                factoryCompany = "Tập đoàn Khoa học Kỹ thuật Foxconn",
                industrialZone = "KCN Quang Châu, Bắc Giang",
                salaryRange = "9.500.000 - 13.000.000 đ/tháng",
                slotsRemaining = 120,
                commissionVnd = 900_000L,
                benefits = listOf("Bao ăn ở 3 bữa", "Ký túc xá máy lạnh", "Thưởng năng suất", "Đóng BHXH 100%"),
                requirements = "Nam/Nữ 18 - 38 tuổi, không yêu cầu kinh nghiệm, được đào tạo",
                urgent = true
            ),
            JobOpening(
                id = "job-02",
                title = "Kỹ thuật viên chuyền SMT & Kiểm phẩm QA",
                factoryCompany = "Samsung Electronics Việt Nam (SEVT)",
                industrialZone = "KCN Yên Bình, Phổ Yên, Thái Nguyên",
                salaryRange = "10.000.000 - 15.000.000 đ/tháng",
                slotsRemaining = 60,
                commissionVnd = 1_200_000L,
                benefits = listOf("Xe đưa đón 40 tuyến", "Bảo hiểm Samsung Care", "Tiền tăng ca x1.5-x2.0"),
                requirements = "Tốt nghiệp THPT trở lên, chăm chỉ, cẩn thận",
                urgent = true
            ),
            JobOpening(
                id = "job-03",
                title = "Công nhân đóng gói & kho vận linh kiện",
                factoryCompany = "Pegatron Technology Hải Phòng",
                industrialZone = "KCN Deep C, Đình Vũ, Hải Phòng",
                salaryRange = "8.500.000 - 12.000.000 đ/tháng",
                slotsRemaining = 80,
                commissionVnd = 750_000L,
                benefits = listOf("Hỗ trợ nhà trọ 800k/tháng", "Phụ cấp xăng xe", "Cơm trưa tại căn-tin"),
                requirements = "Nam/Nữ 18 - 42 tuổi, sức khỏe tốt",
                urgent = false
            ),
            JobOpening(
                id = "job-04",
                title = "Vận hành máy gia công cơ khí chính xác CNC",
                factoryCompany = "Công ty CP Sản xuất VinFast",
                industrialZone = "Tổ hợp Cát Hải, Hải Phòng",
                salaryRange = "12.000.000 - 18.000.000 đ/tháng",
                slotsRemaining = 35,
                commissionVnd = 1_500_000L,
                benefits = listOf("Khu lưu trú chuyên gia", "Bảo hiểm sức khỏe Vingroup", "Thưởng tháng 13"),
                requirements = "Trung cấp cơ khí hoặc kinh nghiệm CNC từ 6 tháng",
                urgent = false
            )
        )
    )
    val jobListings: StateFlow<List<JobOpening>> = _jobListings.asStateFlow()

    private val _ctvCandidates = MutableStateFlow(
        listOf(
            CtvCandidate(
                id = "cand-01",
                candidateName = "Lê Thị Thảo",
                phone = "0971***882",
                appliedJob = "Foxconn - Lắp ráp Module Camera",
                referralDate = "05/10/2026",
                status = "Đang đi làm (Đủ 15 công)",
                rewardVnd = 900_000L,
                isPaid = true
            ),
            CtvCandidate(
                id = "cand-02",
                candidateName = "Đặng Hoàng Nam",
                phone = "0982***114",
                appliedJob = "Samsung SEVT - Chuyền SMT",
                referralDate = "12/10/2026",
                status = "Phỏng vấn đạt (Chờ xếp ca)",
                rewardVnd = 1_200_000L,
                isPaid = false
            ),
            CtvCandidate(
                id = "cand-03",
                candidateName = "Bùi Văn Kiên",
                phone = "0356***998",
                appliedJob = "Pegatron Hải Phòng - Kho vận",
                referralDate = "14/10/2026",
                status = "Đã nộp hồ sơ",
                rewardVnd = 750_000L,
                isPaid = false
            )
        )
    )
    val ctvCandidates: StateFlow<List<CtvCandidate>> = _ctvCandidates.asStateFlow()

    private val _bodMetrics = MutableStateFlow(
        BodOverviewStats(
            activeWorkersCount = 2450,
            workerGrowthPercent = 4.8,
            fillRatePercent = 96.2,
            totalPayrollVnd = 21_850_000_000L,
            recruitmentSlaDays = 2.8,
            pendingTicketsCount = 14,
            attendanceComplianceRate = 98.4
        )
    )
    val bodMetrics: StateFlow<BodOverviewStats> = _bodMetrics.asStateFlow()

    fun switchRole(role: UserRole) {
        _currentRole.value = role
    }

    // ==================== AUTH ACTIONS ====================

    suspend fun login(phone: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.login(LoginRequest(phone, password))
            val hasSession = !response?.token.isNullOrBlank() || tokenManager?.isLoggedIn() == true
            if (response != null && (response.success || response.token != null || hasSession)) {
                val token = response.token ?: tokenManager?.getToken() ?: "session_valid"
                tokenManager?.saveUserSession(
                    token = token,
                    phone = phone,
                    role = response.role ?: "WORKER",
                    fullName = response.user?.fullName ?: "Người Lao Động"
                )

                // Update role according to returned role
                response.role?.let { roleStr ->
                    val userRole = when (roleStr.uppercase()) {
                        "HR", "HR_STAFF", "HR_MANAGER" -> UserRole.HR_STAFF
                        "BOD", "DIRECTOR", "ADMIN" -> UserRole.BOD
                        "CTV", "AFFILIATE" -> UserRole.CTV
                        else -> UserRole.WORKER
                    }
                    _currentRole.value = userRole
                }

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
            if (com.example.utils.AppConfig.isDemoMode) {
                // Seamless Fallback strictly for Demo build variant
                val mockRole = when {
                    phone.endsWith("1") -> UserRole.BOD
                    phone.endsWith("8") -> UserRole.HR_STAFF
                    phone.endsWith("9") -> UserRole.CTV
                    else -> UserRole.WORKER
                }
                val mockToken = "hrp_demo_jwt_${UUID.randomUUID()}"
                tokenManager?.saveUserSession(mockToken, phone, mockRole.name, _workerProfile.value.fullName)
                _currentRole.value = mockRole

                val demoResponse = LoginResponse(
                    success = true,
                    token = mockToken,
                    role = mockRole.name,
                    message = "Đăng nhập thành công (Chế độ Demo)"
                )
                Result.success(demoResponse)
            } else {
                // Strictly return REAL network/auth error in production and debug
                Result.failure(Exception("Lỗi máy chủ: ${e.localizedMessage ?: "Không thể kết nối tới backend"}"))
            }
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
        fetchPayslip()
        fetchTickets()
        fetchJobs()
        fetchCandidates()
        fetchBodOverview()
    }

    suspend fun fetchProfile(): Result<WorkerProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getProfile()
            if (response != null) {
                val domain = response.toDomain()
                _workerProfile.value = domain
                Result.success(domain)
            } else {
                Result.success(_workerProfile.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile: ${e.message}")
            Result.success(_workerProfile.value)
        }
    }

    suspend fun fetchAttendance(): Result<List<AttendanceDay>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getAttendanceList()
            if (response != null && response.isNotEmpty()) {
                val domainList = response.map { it.toDomain() }
                _attendanceList.value = domainList
                Result.success(domainList)
            } else {
                Result.success(_attendanceList.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance: ${e.message}")
            Result.success(_attendanceList.value)
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
                Result.success(_payslip.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payslip: ${e.message}")
            Result.success(_payslip.value)
        }
    }

    suspend fun fetchTickets(): Result<List<TicketItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getTickets()
            if (response != null && response.isNotEmpty()) {
                val domainList = response.map { it.toDomain() }
                _tickets.value = domainList
                Result.success(domainList)
            } else {
                Result.success(_tickets.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tickets: ${e.message}")
            Result.success(_tickets.value)
        }
    }

    suspend fun fetchJobs(): Result<List<JobOpening>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getJobListings()
            if (response != null && response.isNotEmpty()) {
                val domainList = response.map { it.toDomain() }
                _jobListings.value = domainList
                Result.success(domainList)
            } else {
                Result.success(_jobListings.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching jobs: ${e.message}")
            Result.success(_jobListings.value)
        }
    }

    suspend fun fetchCandidates(): Result<List<CtvCandidate>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getCandidates()
            if (response != null && response.isNotEmpty()) {
                val domainList = response.map { it.toDomain() }
                _ctvCandidates.value = domainList
                Result.success(domainList)
            } else {
                Result.success(_ctvCandidates.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching candidates: ${e.message}")
            Result.success(_ctvCandidates.value)
        }
    }

    suspend fun fetchBodOverview(): Result<BodOverviewStats> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getBodOverview()
            if (response != null) {
                val domain = response.toDomain()
                _bodMetrics.value = domain
                Result.success(domain)
            } else {
                Result.success(_bodMetrics.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching BOD stats: ${e.message}")
            Result.success(_bodMetrics.value)
        }
    }

    // ==================== WORKER ACTIONS ====================

    suspend fun performCheckIn(
        site: String = "Cổng B - Samsung SEVT",
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        val today = "18/10/2026"
        val current = _attendanceList.value.toMutableList()

        if (!com.example.utils.AppConfig.isDemoMode && (latitude == null || longitude == null)) {
            return@withContext Result.failure(Exception("Yêu cầu định vị GPS thực tế từ thiết bị để chấm công."))
        }

        try {
            val req = CheckInRequest(
                latitude = latitude ?: 21.4925,
                longitude = longitude ?: 105.8239,
                accuracy = accuracy,
                factorySite = site,
                note = "Vào ca qua định vị phần cứng GPS"
            )
            val apiResult = apiService?.checkIn(req)
            val dayResult = apiResult?.attendance?.toDomain()

            if (dayResult != null) {
                val existingIndex = current.indexOfFirst { it.dateString == dayResult.dateString }
                if (existingIndex >= 0) {
                    current[existingIndex] = dayResult
                } else {
                    current.add(0, dayResult)
                }
                _attendanceList.value = current
                return@withContext Result.success(dayResult)
            } else if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception(apiResult?.message ?: "Chấm công thất bại từ máy chủ"))
            }
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi chấm công: ${e.localizedMessage ?: e.message}"))
            }
        }

        // Demo fallback strictly restricted to demo variant
        if (com.example.utils.AppConfig.isDemoMode) {
            val existingIndex = current.indexOfFirst { it.dateString == today }
            val updatedDay = if (existingIndex >= 0) {
                val old = current[existingIndex]
                val updated = old.copy(
                    checkInTime = "07:55",
                    locationVerified = true,
                    factorySite = site,
                    status = AttendanceStatus.PENDING
                )
                current[existingIndex] = updated
                updated
            } else {
                val newDay = AttendanceDay(
                    id = UUID.randomUUID().toString(),
                    dateString = today,
                    dayOfWeek = "Hôm nay",
                    shiftName = "Ca Sáng (08:00 - 17:00)",
                    checkInTime = "07:55",
                    checkOutTime = null,
                    standardHours = 8.0,
                    otHours = 0.0,
                    locationVerified = true,
                    factorySite = site,
                    status = AttendanceStatus.PENDING
                )
                current.add(0, newDay)
                newDay
            }
            _attendanceList.value = current
            return@withContext Result.success(updatedDay)
        }

        Result.failure(Exception("Chấm công thất bại"))
    }

    suspend fun performCheckOut(
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<AttendanceDay> = withContext(Dispatchers.IO) {
        val today = "18/10/2026"
        val current = _attendanceList.value.toMutableList()

        try {
            apiService?.checkOut(CheckOutRequest(latitude, longitude))
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi máy chủ khi tan ca: ${e.localizedMessage ?: e.message}"))
            }
        }

        val existingIndex = current.indexOfFirst { it.dateString == today }
        if (existingIndex >= 0) {
            val old = current[existingIndex]
            val updated = old.copy(
                checkOutTime = "17:05",
                status = AttendanceStatus.APPROVED
            )
            current[existingIndex] = updated
            _attendanceList.value = current
            Result.success(updated)
        } else {
            Result.failure(Exception("Chưa có lượt vào ca hôm nay để tan ca"))
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
                return@withContext Result.success(newTicket)
            } else if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Gửi yêu cầu thất bại từ máy chủ"))
            }
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi gửi yêu cầu: ${e.localizedMessage ?: e.message}"))
            }
        }

        if (com.example.utils.AppConfig.isDemoMode) {
            val newCode = "TK-${(1000..9999).random()}"
            val localTicket = TicketItem(
                id = UUID.randomUUID().toString(),
                code = newCode,
                type = type,
                title = title,
                description = description,
                amountVnd = amountVnd,
                requestedDays = requestedDays,
                createdDate = "18/10/2026",
                status = TicketStatus.PENDING,
                requesterName = _workerProfile.value.fullName,
                requesterCode = _workerProfile.value.employeeCode,
                hrNote = "Mới tạo, đang chờ HR điều phối xác nhận."
            )
            _tickets.value = listOf(localTicket) + _tickets.value
            return@withContext Result.success(localTicket)
        }

        Result.failure(Exception("Không thể gửi yêu cầu"))
    }

    // ==================== HR ACTIONS ====================

    suspend fun approveTicket(ticketId: String, note: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        try {
            apiService?.updateTicketStatus(
                ticketId,
                UpdateTicketStatusRequest(status = "APPROVED", note = note)
            )
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi duyệt yêu cầu: ${e.localizedMessage ?: e.message}"))
            }
        }

        val updated = _tickets.value.map {
            if (it.id == ticketId) {
                val nextStatus = if (it.type == TicketType.ADVANCE_SALARY) {
                    TicketStatus.HR_APPROVED
                } else {
                    TicketStatus.APPROVED
                }
                it.copy(
                    status = nextStatus,
                    hrNote = note.ifBlank { "HR đã kiểm tra và phê duyệt yêu cầu." }
                )
            } else it
        }
        _tickets.value = updated
        val ticket = updated.firstOrNull { it.id == ticketId }
        if (ticket != null) Result.success(ticket) else Result.failure(Exception("Không tìm thấy yêu cầu"))
    }

    suspend fun markTicketPaid(ticketId: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        try {
            apiService?.updateTicketStatus(
                ticketId,
                UpdateTicketStatusRequest(status = "PAID", note = "Kế toán đã giải ngân tiền tạm ứng qua tài khoản ngân hàng.")
            )
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi giải ngân tiền tạm ứng: ${e.localizedMessage ?: e.message}"))
            }
        }

        val updated = _tickets.value.map {
            if (it.id == ticketId) {
                it.copy(
                    status = TicketStatus.PAID,
                    hrNote = "Kế toán đã giải ngân tiền tạm ứng qua tài khoản ngân hàng."
                )
            } else it
        }
        _tickets.value = updated
        val ticket = updated.firstOrNull { it.id == ticketId }
        if (ticket != null) Result.success(ticket) else Result.failure(Exception("Không tìm thấy yêu cầu"))
    }

    suspend fun rejectTicket(ticketId: String, reason: String): Result<TicketItem> = withContext(Dispatchers.IO) {
        try {
            apiService?.updateTicketStatus(
                ticketId,
                UpdateTicketStatusRequest(status = "REJECTED", note = reason)
            )
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi từ chối yêu cầu: ${e.localizedMessage ?: e.message}"))
            }
        }

        val updated = _tickets.value.map {
            if (it.id == ticketId) {
                it.copy(
                    status = TicketStatus.REJECTED,
                    hrNote = reason.ifBlank { "Từ chối yêu cầu do chưa đủ điều kiện công." }
                )
            } else it
        }
        _tickets.value = updated
        val ticket = updated.firstOrNull { it.id == ticketId }
        if (ticket != null) Result.success(ticket) else Result.failure(Exception("Không tìm thấy yêu cầu"))
    }

    suspend fun approveAllAttendanceToday(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService?.approveAllAttendance()
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi phê duyệt bảng công: ${e.localizedMessage ?: e.message}"))
            }
        }

        val updated = _attendanceList.value.map {
            if (it.status == AttendanceStatus.PENDING) {
                it.copy(status = AttendanceStatus.APPROVED)
            } else it
        }
        _attendanceList.value = updated
        Result.success(Unit)
    }

    // ==================== CTV ACTIONS ====================

    suspend fun registerCandidate(
        candidateName: String,
        phone: String,
        jobTitle: String
    ): Result<CtvCandidate> = withContext(Dispatchers.IO) {
        try {
            val req = CandidateRegistrationRequest(candidateName, phone, jobTitle)
            val apiResult = apiService?.registerCandidate(req)
            val newCand = apiResult?.toDomain()

            if (newCand != null) {
                _ctvCandidates.value = listOf(newCand) + _ctvCandidates.value
                return@withContext Result.success(newCand)
            } else if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Đăng ký ứng viên thất bại từ máy chủ"))
            }
        } catch (e: Exception) {
            if (!com.example.utils.AppConfig.isDemoMode) {
                return@withContext Result.failure(Exception("Lỗi đăng ký ứng viên: ${e.localizedMessage ?: e.message}"))
            }
        }

        if (com.example.utils.AppConfig.isDemoMode) {
            val localCand = CtvCandidate(
                id = UUID.randomUUID().toString(),
                candidateName = candidateName,
                phone = phone,
                appliedJob = jobTitle,
                referralDate = "18/10/2026",
                status = "Đã tiếp nhận hồ sơ",
                rewardVnd = 900_000L,
                isPaid = false
            )
            _ctvCandidates.value = listOf(localCand) + _ctvCandidates.value
            return@withContext Result.success(localCand)
        }

        Result.failure(Exception("Không thể đăng ký ứng viên"))
    }
}
