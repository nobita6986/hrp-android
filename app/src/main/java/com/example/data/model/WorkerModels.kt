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
    IN_SHIFT_ACTIVE,
    COMPLETED_TODAY
}

enum class AttendancePrimaryAction {
    CHECK_IN,
    CHECK_OUT,
    NONE
}

data class CheckInGateState(
    val isCheckInEnabled: Boolean,
    val isCheckOutEnabled: Boolean,
    val phase: CheckInPhase,
    val primaryAction: AttendancePrimaryAction = when {
        phase == CheckInPhase.IN_SHIFT_ACTIVE -> AttendancePrimaryAction.CHECK_OUT
        isCheckInEnabled -> AttendancePrimaryAction.CHECK_IN
        isCheckOutEnabled -> AttendancePrimaryAction.CHECK_OUT
        else -> AttendancePrimaryAction.NONE
    },
    val isPrimaryButtonEnabled: Boolean = isCheckInEnabled || isCheckOutEnabled,
    val buttonTitle: String = when (phase) {
        CheckInPhase.IDLE_READY -> "VÀO CA"
        CheckInPhase.IN_SHIFT_ACTIVE -> "TAN CA"
        CheckInPhase.COMPLETED_TODAY -> "ĐÃ HOÀN TẤT"
    },
    val buttonSubtitle: String = "Chấm công GPS",
    val statusHint: String? = null
)

/**
 * Project Shift Attendance Policy (Đồng bộ theo cơ chế Ca làm việc thực tế của Dự án `ProjectShift` trên Backend):
 * - Bỏ cơ chế khóa 6h–12h–20h cũ.
 * - Khi chưa Vào ca trong ngày (và không có ca đêm đang mở từ hôm qua): hiển thị nút "VÀO CA" (mở).
 * - Ngay sau khi bấm "VÀO CA" thành công: chuyển ngay sang nút "TAN CA" (mở) để công nhân bấm khi kết thúc ca thực tế
 *   (hỗ trợ cả ca ngày và ca đêm qua 00:00 ngày hôm sau).
 * - Sau khi đã bấm "TAN CA": hoàn tất ca trong ngày ("ĐÃ HOÀN TẤT", tạm khóa trong ngày và tự mở lại vào ngày mới).
 */
object RewardAttendancePolicy {
    const val OVERNIGHT_SHIFT_MAX_WINDOW_MS = 18L * 60L * 60L * 1000L

    private val vnTimeZone: java.util.TimeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    fun formatDateIsoVn(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.timeZone = vnTimeZone
        return sdf.format(java.util.Date(epochMs))
    }

    fun formatTimeHhMmVn(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        sdf.timeZone = vnTimeZone
        return sdf.format(java.util.Date(epochMs))
    }

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
        if (outMs <= inMs) return 0.0
        val rawHours = (outMs - inMs).toDouble() / (60.0 * 60.0 * 1000.0)
        return Math.round(rawHours * 10.0) / 10.0
    }

    /**
     * Evaluates button state for Vào Ca (GPS) and Tan Ca (GPS) aligned with Backend ProjectShift rules.
     */
    fun evaluateButtonGate(
        attendanceList: List<AttendanceDay>,
        cachedCheckInMs: Long = 0L,
        cachedCheckOutMs: Long = 0L,
        nowMs: Long = System.currentTimeMillis()
    ): CheckInGateState {
        val todayIso = formatDateIsoVn(nowMs)
        val yesterdayIso = formatDateIsoVn(nowMs - 24L * 60L * 60L * 1000L)

        val todayRecord = attendanceList.firstOrNull { it.dateString == todayIso }
        val yesterdayRecord = attendanceList.firstOrNull { it.dateString == yesterdayIso }

        val cachedInDate = if (cachedCheckInMs > 0L) formatDateIsoVn(cachedCheckInMs) else ""
        val cachedHasCheckOut = cachedCheckOutMs >= cachedCheckInMs && cachedCheckOutMs > 0L

        val hasTodayCheckIn = !todayRecord?.checkInTime.isNullOrBlank() || cachedInDate == todayIso
        val hasTodayCheckOut = !todayRecord?.checkOutTime.isNullOrBlank() ||
            (cachedInDate == todayIso && cachedHasCheckOut)

        // 1. Today's shift has both Check-In and Check-Out -> Completed for today
        if (hasTodayCheckIn && hasTodayCheckOut) {
            val outTimeText = todayRecord?.checkOutTime?.takeIf { it.isNotBlank() }
                ?: if (cachedCheckOutMs > 0L) formatTimeHhMmVn(cachedCheckOutMs) else ""
            val subText = if (outTimeText.isNotBlank()) {
                "Đã tan ca lúc $outTimeText"
            } else {
                "Đã chấm đủ công hôm nay"
            }
            return CheckInGateState(
                isCheckInEnabled = false,
                isCheckOutEnabled = false,
                phase = CheckInPhase.COMPLETED_TODAY,
                primaryAction = AttendancePrimaryAction.NONE,
                isPrimaryButtonEnabled = false,
                buttonTitle = "ĐÃ HOÀN TẤT",
                buttonSubtitle = subText,
                statusHint = "Đã hoàn tất chấm công Vào ca và Tan ca hôm nay theo ca làm việc của dự án."
            )
        }

        // 2. Today has Check-In without Check-Out -> In Shift, immediately ready for Check-Out ("TAN CA")
        if (hasTodayCheckIn) {
            val inTimeText = todayRecord?.checkInTime?.takeIf { it.isNotBlank() }
                ?: if (cachedCheckInMs > 0L) formatTimeHhMmVn(cachedCheckInMs) else ""
            val shiftBadge = todayRecord?.shiftName?.takeIf {
                it.isNotBlank() && it != "Ca tiêu chuẩn" && it != "Ca dự án"
            }
            val subText = when {
                shiftBadge != null && inTimeText.isNotBlank() -> "$shiftBadge • Vào $inTimeText"
                inTimeText.isNotBlank() -> "Đã vào ca lúc $inTimeText"
                else -> "Bấm khi kết thúc ca"
            }
            return CheckInGateState(
                isCheckInEnabled = false,
                isCheckOutEnabled = true,
                phase = CheckInPhase.IN_SHIFT_ACTIVE,
                primaryAction = AttendancePrimaryAction.CHECK_OUT,
                isPrimaryButtonEnabled = true,
                buttonTitle = "TAN CA",
                buttonSubtitle = subText,
                statusHint = "Đang trong ca làm việc${if (inTimeText.isNotBlank()) " (vào ca lúc $inTimeText)" else ""}. Bấm Tan Ca khi kết thúc ca làm việc."
            )
        }

        // 3. Support overnight shift from yesterday (open Check-In on yesterday within 18h window)
        val hasYesterdayOpenRecord = yesterdayRecord != null &&
            !yesterdayRecord.checkInTime.isNullOrBlank() &&
            yesterdayRecord.checkOutTime.isNullOrBlank()
        val hasYesterdayOpenCache = cachedInDate == yesterdayIso &&
            !cachedHasCheckOut &&
            (yesterdayRecord == null || yesterdayRecord.checkOutTime.isNullOrBlank())

        val yesterdayInMs = yesterdayRecord?.let { parseDateAndTimeEpochMs(it.dateString, it.checkInTime) }
            ?: if (cachedInDate == yesterdayIso) cachedCheckInMs else 0L
        val isOvernightActive = yesterdayInMs > 0L && (nowMs - yesterdayInMs) in 0L..OVERNIGHT_SHIFT_MAX_WINDOW_MS

        if ((hasYesterdayOpenRecord || hasYesterdayOpenCache) && isOvernightActive) {
            val inTimeText = yesterdayRecord?.checkInTime?.takeIf { it.isNotBlank() }
                ?: formatTimeHhMmVn(yesterdayInMs)
            return CheckInGateState(
                isCheckInEnabled = false,
                isCheckOutEnabled = true,
                phase = CheckInPhase.IN_SHIFT_ACTIVE,
                primaryAction = AttendancePrimaryAction.CHECK_OUT,
                isPrimaryButtonEnabled = true,
                buttonTitle = "TAN CA",
                buttonSubtitle = if (inTimeText.isNotBlank()) "Ca đêm • Vào $inTimeText" else "Kết thúc ca đêm",
                statusHint = "Đang trong ca đêm từ hôm qua${if (inTimeText.isNotBlank()) " (vào ca lúc $inTimeText)" else ""}. Bấm Tan Ca để chốt công ca đêm."
            )
        }

        // 4. Default: Ready to Check-In ("VÀO CA")
        return CheckInGateState(
            isCheckInEnabled = true,
            isCheckOutEnabled = false,
            phase = CheckInPhase.IDLE_READY,
            primaryAction = AttendancePrimaryAction.CHECK_IN,
            isPrimaryButtonEnabled = true,
            buttonTitle = "VÀO CA",
            buttonSubtitle = "Chấm công GPS",
            statusHint = null
        )
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
