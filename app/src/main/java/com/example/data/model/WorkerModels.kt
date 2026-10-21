package com.example.data.model

enum class UserRole(val displayName: String, val badgeTitle: String) {
    WORKER("Người Lao Động / Công Nhân", "Worker Portal"),
    HR_STAFF("Nhân Sự / Quản Lý Ca", "HR Staff"),
    BOD("Ban Giám Đốc", "BoD Control Tower"),
    CTV("Cộng Tác Viên Tuyển Dụng", "Affiliate / CTV")
}

enum class VerificationStatus(val label: String) {
    NONE("Chưa xác thực"),
    PENDING_REVIEW("Đang chờ HR đối soát"),
    NEEDS_INFO("Yêu cầu bổ sung thông tin"),
    APPROVED("Đã xác thực công nhân HRP"),
    REJECTED("Từ chối xác thực");

    companion object {
        fun fromString(value: String?): VerificationStatus {
            return when (value?.uppercase()) {
                "PENDING_REVIEW" -> PENDING_REVIEW
                "NEEDS_INFO" -> NEEDS_INFO
                "APPROVED" -> APPROVED
                "REJECTED" -> REJECTED
                else -> NONE
            }
        }
    }
}

data class WorkerVerificationInfo(
    val id: String = "",
    val status: VerificationStatus = VerificationStatus.NONE,
    val reviewNote: String? = null,
    val projectId: String? = null,
    val projectName: String? = null,
    val employeeCode: String? = null,
    val submittedAt: String? = null,
    val reviewedAt: String? = null
)

data class PublicProject(
    val id: String,
    val code: String,
    val name: String,
    val location: String
)

data class WorkerAssignmentSite(
    val id: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 0
)

data class WorkerProfile(
    val id: String,
    val employeeCode: String,
    val fullName: String,
    val cccdNumber: String,
    val phone: String,
    val factoryLocation: String,
    val projectCode: String,
    val department: String,
    val position: String,
    val baseSalaryPerDayVnd: Long,
    val joinedDate: String,
    val bankAccount: String,
    val status: String = "ACTIVE",
    val phoneVerified: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.NONE,
    val hasActiveAssignment: Boolean = false,
    val canCheckIn: Boolean = false,
    val assignmentId: String? = null,
    val projectId: String? = null,
    val projectName: String = "",
    val sites: List<WorkerAssignmentSite> = emptyList(),
    val hasConfiguredSiteGps: Boolean = true,
    val siteGeofenceNotConfigured: Boolean = false
)

enum class AttendanceStatus {
    APPROVED,   // Đã duyệt / HỢP LỆ (INSIDE)
    PENDING,    // Chờ HR chốt
    DISPUTED,   // Đang khiếu nại / Ngoài vùng (OUTSIDE)
    ABSENT      // Vắng mặt
}

data class AttendanceDay(
    val id: String,
    val dateString: String,      // "2026-10-18"
    val dayOfWeek: String,       // "Chủ Nhật", "Thứ Hai"...
    val shiftName: String,       // "Ca Sáng (08:00 - 17:00)", "Ca Đêm"
    val checkInTime: String?,    // "07:54"
    val checkOutTime: String?,   // "17:05"
    val standardHours: Double,   // Actual recorded hours (6.0..12.0 when valid, 0.0 otherwise)
    val otHours: Double,         // 0.0
    val locationVerified: Boolean,
    val factorySite: String,
    val status: AttendanceStatus,
    val note: String = "",
    val geofenceResult: String? = null,
    val riskFlag: Boolean = false
)

enum class CheckInPhase {
    IDLE_READY,
    WAITING_MIN_6H,
    WINDOW_OPEN_6H_TO_12H,
    EXPIRED_OVER_12H_UNCLOSED,
    COMPLETED_WAIT_20H
}

data class CheckInGateState(
    val isCheckInEnabled: Boolean,
    val isCheckOutEnabled: Boolean,
    val phase: CheckInPhase,
    val statusHint: String? = null
)

/**
 * Reward Attendance Policy (Chấm công lấy thưởng):
 * - Mỗi chu kỳ người dùng được chấm 1 lần Vào ca và 1 lần Tan ca.
 * - Khoảng cách giữa Vào ca và Tan ca tối thiểu 6h, tối đa 12h.
 * - Sau khi bấm Vào ca (0h <= t < 6h): làm mờ cả 2 nút Vào ca và Tan ca.
 * - Từ đủ 6h đến 12h (6h <= t <= 12h): mở nút Tan ca, vẫn khóa nút Vào ca.
 * - Nếu quá 12h chưa bấm Tan ca (t > 12h): ca làm KHÔNG được ghi nhận (0.0h), mở lại cả 2 nút.
 * - Nếu đã Tan ca hợp lệ (6h <= duration <= 12h): ca làm được ghi nhận theo số giờ thực tế (không fix cứng 8.0h),
 *   và sau 20h kể từ lần bấm Vào ca trước thì mở lại nút Vào ca cho chu kỳ tiếp theo.
 */
object RewardAttendancePolicy {
    const val MIN_SHIFT_HOURS = 6.0
    const val MAX_SHIFT_HOURS = 12.0
    const val NEXT_CHECKIN_COOLDOWN_HOURS = 20.0

    const val MIN_CHECKOUT_DELAY_MS = 6L * 60L * 60L * 1000L
    const val MAX_CHECKOUT_DELAY_MS = 12L * 60L * 60L * 1000L
    const val NEXT_CHECKIN_COOLDOWN_MS = 20L * 60L * 60L * 1000L

    private val vnTimeZone: java.util.TimeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    fun parseDateAndTimeEpochMs(workDate: String?, timeStr: String?): Long? {
        val rawTime = timeStr?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        // Case 1: full ISO timestamp
        if (rawTime.contains("T")) {
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            for (p in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                    if (p.endsWith("'Z'")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val parsed = sdf.parse(rawTime)
                    if (parsed != null) return parsed.time
                } catch (_: Throwable) {
                }
            }
        }
        // Case 2: "HH:mm" combined with workDate "yyyy-MM-dd"
        val cleanDate = workDate?.trim()?.take(10)?.takeIf { it.length == 10 } ?: return null
        val hhMmMatch = Regex("^(\\d{1,2}):(\\d{2})").find(rawTime) ?: return null
        val hh = hhMmMatch.groupValues[1].padStart(2, '0')
        val mm = hhMmMatch.groupValues[2].padStart(2, '0')
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            sdf.timeZone = vnTimeZone
            sdf.parse("$cleanDate $hh:$mm")?.time
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Calculates actual elapsed hours between checkInTime and checkOutTime.
     * Returns null if either timestamp is missing or unparseable.
     */
    fun calculateShiftHours(workDate: String?, checkInTime: String?, checkOutTime: String?): Double? {
        val inMs = parseDateAndTimeEpochMs(workDate = workDate, timeStr = checkInTime) ?: return null
        var outMs = parseDateAndTimeEpochMs(workDate = workDate, timeStr = checkOutTime) ?: return null
        // Support overnight shift where HH:mm check-out is on the next calendar day
        if (outMs < inMs && checkOutTime != null && !checkOutTime.contains("T")) {
            outMs += 24L * 60L * 60L * 1000L
        }
        if (outMs < inMs) return 0.0
        val rawHours = (outMs - inMs).toDouble() / (60.0 * 60.0 * 1000.0)
        return Math.round(rawHours * 10.0) / 10.0
    }

    fun isValidRewardShiftDuration(hours: Double?): Boolean {
        return hours != null && hours >= MIN_SHIFT_HOURS && hours <= MAX_SHIFT_HOURS
    }

    fun formatDurationText(remainingMs: Long): String {
        val clamped = remainingMs.coerceAtLeast(0L)
        val totalMinutes = (clamped + 59_999L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}p"
            hours > 0L -> "${hours}h"
            else -> "${minutes.coerceAtLeast(1L)}p"
        }
    }

    /**
     * Evaluates button enablement and explanatory hint for Vào Ca (GPS) and Tan Ca (GPS).
     */
    fun evaluateButtonGate(
        attendanceList: List<AttendanceDay>,
        cachedCheckInMs: Long = 0L,
        cachedCheckOutMs: Long = 0L,
        nowMs: Long = System.currentTimeMillis()
    ): CheckInGateState {
        // Resolve latest check-in / check-out from attendanceList and local session cache
        val latestRecord = attendanceList
            .filter { !it.checkInTime.isNullOrBlank() }
            .maxByOrNull { parseDateAndTimeEpochMs(it.dateString, it.checkInTime) ?: 0L }

        val recordInMs = latestRecord?.let { parseDateAndTimeEpochMs(it.dateString, it.checkInTime) } ?: 0L
        val recordOutMs = latestRecord?.let {
            val rawOut = parseDateAndTimeEpochMs(it.dateString, it.checkOutTime)
            if (rawOut != null && recordInMs > 0L && rawOut < recordInMs && it.checkOutTime?.contains("T") != true) {
                rawOut + 24L * 60L * 60L * 1000L
            } else {
                rawOut
            }
        } ?: 0L

        val useCached = cachedCheckInMs > recordInMs
        val activeCheckInMs = if (useCached) cachedCheckInMs else recordInMs
        val activeCheckOutMs = if (useCached) {
            if (cachedCheckOutMs >= cachedCheckInMs) cachedCheckOutMs else 0L
        } else {
            maxOf(recordOutMs, if (cachedCheckOutMs >= recordInMs) cachedCheckOutMs else 0L)
        }

        if (activeCheckInMs <= 0L) {
            return CheckInGateState(
                isCheckInEnabled = true,
                isCheckOutEnabled = false,
                phase = CheckInPhase.IDLE_READY,
                statusHint = null
            )
        }

        val elapsedSinceCheckInMs = (nowMs - activeCheckInMs).coerceAtLeast(0L)

        // Case 1: User already checked out for this shift
        if (activeCheckOutMs >= activeCheckInMs && activeCheckOutMs > 0L) {
            val shiftDurationMs = activeCheckOutMs - activeCheckInMs
            val isValidShift = shiftDurationMs in MIN_CHECKOUT_DELAY_MS..MAX_CHECKOUT_DELAY_MS
            if (isValidShift && elapsedSinceCheckInMs < NEXT_CHECKIN_COOLDOWN_MS) {
                val remainingTo20h = NEXT_CHECKIN_COOLDOWN_MS - elapsedSinceCheckInMs
                return CheckInGateState(
                    isCheckInEnabled = false,
                    isCheckOutEnabled = false,
                    phase = CheckInPhase.COMPLETED_WAIT_20H,
                    statusHint = "Đã hoàn tất ca chấm công hôm nay. Nút Vào Ca sẽ mở lại sau ${formatDurationText(remainingTo20h)} (đủ 20h kể từ lúc Vào ca)."
                )
            }
            return CheckInGateState(
                isCheckInEnabled = true,
                isCheckOutEnabled = false,
                phase = CheckInPhase.IDLE_READY,
                statusHint = if (!isValidShift) {
                    "Lần chấm trước chưa đủ tối thiểu 6h làm việc nên không được ghi nhận."
                } else {
                    null
                }
            )
        }

        // Case 2: User checked in and has NOT checked out yet
        return when {
            elapsedSinceCheckInMs < MIN_CHECKOUT_DELAY_MS -> {
                val remainingTo6h = MIN_CHECKOUT_DELAY_MS - elapsedSinceCheckInMs
                CheckInGateState(
                    isCheckInEnabled = false,
                    isCheckOutEnabled = false,
                    phase = CheckInPhase.WAITING_MIN_6H,
                    statusHint = "Đã ghi nhận Vào ca. Cả 2 nút tạm khóa — nút Tan Ca sẽ mở sau ${formatDurationText(remainingTo6h)} (vào/ra cách nhau 6h–12h)."
                )
            }
            elapsedSinceCheckInMs <= MAX_CHECKOUT_DELAY_MS -> {
                val remainingTo12h = MAX_CHECKOUT_DELAY_MS - elapsedSinceCheckInMs
                CheckInGateState(
                    isCheckInEnabled = false,
                    isCheckOutEnabled = true,
                    phase = CheckInPhase.WINDOW_OPEN_6H_TO_12H,
                    statusHint = "Đã đủ 6h làm việc! Hãy bấm Tan Ca trong vòng ${formatDurationText(remainingTo12h)} nữa (quá 12h không bấm Tan ca sẽ không được ghi nhận)."
                )
            }
            else -> {
                // Over 12h without checking out: shift is NOT recorded, re-open both buttons
                CheckInGateState(
                    isCheckInEnabled = true,
                    isCheckOutEnabled = true,
                    phase = CheckInPhase.EXPIRED_OVER_12H_UNCLOSED,
                    statusHint = "Ca trước đã quá 12h không bấm Tan ca nên không được ghi nhận. Đã mở lại nút để chấm công ca mới."
                )
            }
        }
    }
}

data class PayslipSummary(
    val monthPeriod: String,
    val standardWorkDays: Int,
    val actualWorkDays: Double,
    val otHours: Double,
    val baseSalaryVnd: Long,
    val otPayVnd: Long,
    val attendanceBonusVnd: Long,
    val mealAllowanceVnd: Long,
    val transportAllowanceVnd: Long,
    val grossSalaryVnd: Long,
    val socialInsuranceVnd: Long,
    val healthInsuranceVnd: Long,
    val unemploymentInsVnd: Long,
    val personalIncomeTaxVnd: Long,
    val advancePaidVnd: Long,
    val netSalaryVnd: Long,
    val isFinalized: Boolean,
    val payDate: String
)

enum class TicketType(val label: String) {
    ADVANCE_SALARY("Tạm ứng lương"),
    TIMESHEET_DISPUTE("Khiếu nại sai công"),
    LEAVE_REQUEST("Xin nghỉ phép"),
    OTHER("Ý kiến đóng góp")
}

enum class TicketStatus(val label: String) {
    PENDING("Chờ HR duyệt"),
    HR_APPROVED("HR đã xác nhận"),
    APPROVED("Đã duyệt"),
    PAID("Đã chi tiền"),
    REJECTED("Từ chối"),
    CLOSED("Đã đóng")
}

data class TicketItem(
    val id: String,
    val code: String,
    val type: TicketType,
    val title: String,
    val description: String,
    val amountVnd: Long? = null,
    val requestedDays: String? = null,
    val createdDate: String,
    val status: TicketStatus,
    val requesterName: String,
    val requesterCode: String,
    val hrNote: String = ""
)

enum class FeedbackCategory(val code: String, val label: String) {
    WORK_ENVIRONMENT("WORK_ENVIRONMENT", "Môi trường làm việc"),
    SALARY_BENEFITS("SALARY_BENEFITS", "Tiền lương & Chế độ"),
    SUGGESTION("SUGGESTION", "Đề xuất cải tiến"),
    COMPLAINT("COMPLAINT", "Khiếu nại"),
    OTHER("OTHER", "Góp ý khác");

    companion object {
        fun fromCode(code: String?): FeedbackCategory {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }
    }
}

data class FeedbackItem(
    val id: String,
    val category: FeedbackCategory,
    val content: String,
    val status: String, // RECEIVED, IN_REVIEW, RESOLVED, REJECTED
    val adminResponse: String? = null,
    val respondedAt: String? = null,
    val createdAt: String = ""
)

data class ReferralProgram(
    val isEnabled: Boolean = false,
    val policyVersion: String? = null,
    val title: String? = null,
    val eligibilityCriteria: String? = null,
    val successEventDescription: String? = null,
    val rewardPolicyDescription: String? = null,
    val termsAndConditions: String? = null,
    val effectiveFrom: String? = null,
    val effectiveUntil: String? = null,
    val canSubmit: Boolean = false,
    val blockedReason: String? = null
)

data class ReferralItem(
    val id: String,
    val maskedReferredName: String,
    val maskedReferredPhone: String,
    val status: String,        // SUBMITTED, IN_REVIEW, QUALIFIED, REWARD_APPROVED, PAID, REJECTED
    val benefitStatus: String, // NONE, PENDING_APPROVAL, APPROVED, PAID, CANCELLED
    val benefitAmountVnd: Long? = null,
    val submittedAt: String = "",
    val updatedAt: String? = null
)

data class JobOpening(
    val id: String,
    val slug: String = id,
    val title: String,
    val factoryCompany: String,
    val industrialZone: String,
    val salaryRange: String,
    val slotsRemaining: Int,
    val commissionVnd: Long = 0L,
    val benefits: List<String> = emptyList(),
    val requirements: String = "",
    val urgent: Boolean = false,
    val shift: String? = null,
    val position: String? = null,
    val deadline: String? = null,
    val jobTypeLabel: String = "Toàn thời gian",
    val statusLabel: String = "Đang tuyển",
    val filledQuotaText: String = "",
    val deadlineDisplay: String = "",
    val description: String = "",
    val benefitsDetail: String = "",
    val requirementsDetail: String = "",
    val applicationGuide: String = "",
    val employerAddress: String = "",
    val isDetailLoaded: Boolean = false
)

data class CtvCandidate(
    val id: String,
    val candidateName: String,
    val phone: String,
    val appliedJob: String,
    val referralDate: String,
    val status: String,
    val rewardVnd: Long,
    val isPaid: Boolean
)

data class BodOverviewStats(
    val activeWorkersCount: Int,
    val workerGrowthPercent: Double,
    val fillRatePercent: Double,
    val totalPayrollVnd: Long,
    val recruitmentSlaDays: Double,
    val pendingTicketsCount: Int,
    val attendanceComplianceRate: Double
)
