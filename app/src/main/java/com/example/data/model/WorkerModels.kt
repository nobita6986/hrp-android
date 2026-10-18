package com.example.data.model

enum class UserRole(val displayName: String, val badgeTitle: String) {
    WORKER("Người Lao Động / Công Nhân", "Worker Portal"),
    HR_STAFF("Nhân Sự / Quản Lý Ca", "HR Staff"),
    BOD("Ban Giám Đốc", "BoD Control Tower"),
    CTV("Cộng Tác Viên Tuyển Dụng", "Affiliate / CTV")
}

data class WorkerProfile(
    val id: String,
    val employeeCode: String,
    val fullName: String,
    val cccdNumber: String,
    val phone: String,
    val factoryLocation: String,     // e.g. "Nhà máy Samsung SEVT - Thái Nguyên"
    val projectCode: String,         // e.g. "PRJ-SAMS-01"
    val department: String,          // e.g. "Xưởng Lắp Ráp Điện Tử A3"
    val position: String,            // e.g. "Kỹ thuật viên đứng chuyền"
    val baseSalaryPerDayVnd: Long,   // e.g. 380_000L
    val joinedDate: String,          // e.g. "15/03/2023"
    val bankAccount: String,         // e.g. "Techcombank - 19038291038"
    val status: String = "ACTIVE"
)

enum class AttendanceStatus {
    APPROVED,   // Đã duyệt
    PENDING,    // Chờ HR chốt
    DISPUTED,   // Đang khiếu nại
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
    val note: String = ""
)

data class PayslipSummary(
    val monthPeriod: String,         // "Tháng 10/2026"
    val standardWorkDays: Int,       // 24
    val actualWorkDays: Double,      // 24.5
    val otHours: Double,             // 28.0
    val baseSalaryVnd: Long,         // 9_310_000
    val otPayVnd: Long,              // 2_050_000
    val attendanceBonusVnd: Long,    // 500_000 (Chuyên cần)
    val mealAllowanceVnd: Long,      // 750_000 (Tiền ăn ca)
    val transportAllowanceVnd: Long, // 400_000 (Xăng xe/nhà trọ)
    val grossSalaryVnd: Long,        // 13_010_000
    val socialInsuranceVnd: Long,    // BHXH 8% = 744_800
    val healthInsuranceVnd: Long,    // BHYT 1.5% = 139_650
    val unemploymentInsVnd: Long,    // BHTN 1% = 93_100
    val personalIncomeTaxVnd: Long,  // Thuế TNCN = 0
    val advancePaidVnd: Long,        // Tạm ứng đã nhận = 2_000_000
    val netSalaryVnd: Long,          // Thực lĩnh = 10_032_450
    val isFinalized: Boolean,        // Đã khóa sổ / Chuyển khoản
    val payDate: String              // "05/11/2026"
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
    val code: String,                // "TK-8492"
    val type: TicketType,
    val title: String,
    val description: String,
    val amountVnd: Long? = null,     // Cho Tạm ứng
    val requestedDays: String? = null, // Cho Nghỉ phép
    val createdDate: String,
    val status: TicketStatus,
    val requesterName: String,
    val requesterCode: String,
    val hrNote: String = ""
)

data class JobOpening(
    val id: String,
    val title: String,               // "Công nhân lắp ráp linh kiện điện tử"
    val factoryCompany: String,      // "Công ty TNHH Samsung Electronics VN"
    val industrialZone: String,      // "KCN Yên Phong, Bắc Ninh"
    val salaryRange: String,         // "9.000.000 - 13.500.000 đ/tháng"
    val slotsRemaining: Int,         // 85
    val commissionVnd: Long,         // 800_000 đ (Hoa hồng cho CTV khi giới thiệu thành công)
    val benefits: List<String>,      // "Bao ăn 2 bữa", "Ký túc xá miễn phí", "Thưởng lễ tết"
    val requirements: String,        // "Nam/Nữ 18 - 35 tuổi, có CCCD gốc"
    val urgent: Boolean = false
)

data class CtvCandidate(
    val id: String,
    val candidateName: String,
    val phone: String,
    val appliedJob: String,
    val referralDate: String,
    val status: String,              // "Đang đi làm", "Phỏng vấn đạt", "Chờ xếp ca"
    val rewardVnd: Long,             // 800_000
    val isPaid: Boolean
)

data class BodOverviewStats(
    val activeWorkersCount: Int,     // 2,450
    val workerGrowthPercent: Double, // +4.8%
    val fillRatePercent: Double,     // 96.2%
    val totalPayrollVnd: Long,       // 21_850_000_000 (21.85 Tỷ VND)
    val recruitmentSlaDays: Double,  // 2.8 ngày
    val pendingTicketsCount: Int,    // 14
    val attendanceComplianceRate: Double // 98.4%
)
