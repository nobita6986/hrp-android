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
    val hasActiveAssignment: Boolean = false
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
    val standardHours: Double,   // 8.0
    val otHours: Double,         // 2.0
    val locationVerified: Boolean,
    val factorySite: String,
    val status: AttendanceStatus,
    val note: String = "",
    val geofenceResult: String? = null,
    val riskFlag: Boolean = false
)

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
    val deadline: String? = null
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
