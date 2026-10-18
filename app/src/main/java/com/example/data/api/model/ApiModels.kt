package com.example.data.api.model

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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic API response envelope
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null
)

// ==================== AUTH MODELS ====================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "token") val token: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "user") val user: WorkerProfileResponse? = null,
    @Json(name = "message") val message: String? = null
)

// ==================== WORKER PROFILE ====================

@JsonClass(generateAdapter = true)
data class WorkerProfileResponse(
    @Json(name = "id") val id: String,
    @Json(name = "employeeCode") val employeeCode: String,
    @Json(name = "fullName") val fullName: String,
    @Json(name = "cccdNumber") val cccdNumber: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "factoryLocation") val factoryLocation: String,
    @Json(name = "projectCode") val projectCode: String,
    @Json(name = "department") val department: String,
    @Json(name = "position") val position: String,
    @Json(name = "baseSalaryPerDayVnd") val baseSalaryPerDayVnd: Long,
    @Json(name = "joinedDate") val joinedDate: String,
    @Json(name = "bankAccount") val bankAccount: String,
    @Json(name = "status") val status: String = "ACTIVE"
) {
    fun toDomain(): WorkerProfile {
        return WorkerProfile(
            id = id,
            employeeCode = employeeCode,
            fullName = fullName,
            cccdNumber = cccdNumber,
            phone = phone,
            factoryLocation = factoryLocation,
            projectCode = projectCode,
            department = department,
            position = position,
            baseSalaryPerDayVnd = baseSalaryPerDayVnd,
            joinedDate = joinedDate,
            bankAccount = bankAccount,
            status = status
        )
    }
}

// ==================== ATTENDANCE MODELS ====================

@JsonClass(generateAdapter = true)
data class CheckInRequest(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "accuracy") val accuracy: Float? = null,
    @Json(name = "factorySite") val factorySite: String? = null,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckOutRequest(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckInResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String,
    @Json(name = "attendance") val attendance: AttendanceDayResponse? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceDayResponse(
    @Json(name = "id") val id: String,
    @Json(name = "dateString") val dateString: String,
    @Json(name = "dayOfWeek") val dayOfWeek: String,
    @Json(name = "shiftName") val shiftName: String,
    @Json(name = "checkInTime") val checkInTime: String?,
    @Json(name = "checkOutTime") val checkOutTime: String?,
    @Json(name = "standardHours") val standardHours: Double,
    @Json(name = "otHours") val otHours: Double,
    @Json(name = "locationVerified") val locationVerified: Boolean,
    @Json(name = "factorySite") val factorySite: String,
    @Json(name = "status") val status: String,
    @Json(name = "note") val note: String = ""
) {
    fun toDomain(): AttendanceDay {
        val domainStatus = when (status.uppercase()) {
            "APPROVED" -> AttendanceStatus.APPROVED
            "DISPUTED" -> AttendanceStatus.DISPUTED
            "ABSENT" -> AttendanceStatus.ABSENT
            else -> AttendanceStatus.PENDING
        }
        return AttendanceDay(
            id = id,
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            shiftName = shiftName,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            standardHours = standardHours,
            otHours = otHours,
            locationVerified = locationVerified,
            factorySite = factorySite,
            status = domainStatus,
            note = note
        )
    }
}

// ==================== PAYSLIP MODELS ====================

@JsonClass(generateAdapter = true)
data class PayslipSummaryResponse(
    @Json(name = "monthPeriod") val monthPeriod: String,
    @Json(name = "standardWorkDays") val standardWorkDays: Int,
    @Json(name = "actualWorkDays") val actualWorkDays: Double,
    @Json(name = "otHours") val otHours: Double,
    @Json(name = "baseSalaryVnd") val baseSalaryVnd: Long,
    @Json(name = "otPayVnd") val otPayVnd: Long,
    @Json(name = "attendanceBonusVnd") val attendanceBonusVnd: Long,
    @Json(name = "mealAllowanceVnd") val mealAllowanceVnd: Long,
    @Json(name = "transportAllowanceVnd") val transportAllowanceVnd: Long,
    @Json(name = "grossSalaryVnd") val grossSalaryVnd: Long,
    @Json(name = "socialInsuranceVnd") val socialInsuranceVnd: Long,
    @Json(name = "healthInsuranceVnd") val healthInsuranceVnd: Long,
    @Json(name = "unemploymentInsVnd") val unemploymentInsVnd: Long,
    @Json(name = "personalIncomeTaxVnd") val personalIncomeTaxVnd: Long,
    @Json(name = "advancePaidVnd") val advancePaidVnd: Long,
    @Json(name = "netSalaryVnd") val netSalaryVnd: Long,
    @Json(name = "isFinalized") val isFinalized: Boolean,
    @Json(name = "payDate") val payDate: String
) {
    fun toDomain(): PayslipSummary {
        return PayslipSummary(
            monthPeriod = monthPeriod,
            standardWorkDays = standardWorkDays,
            actualWorkDays = actualWorkDays,
            otHours = otHours,
            baseSalaryVnd = baseSalaryVnd,
            otPayVnd = otPayVnd,
            attendanceBonusVnd = attendanceBonusVnd,
            mealAllowanceVnd = mealAllowanceVnd,
            transportAllowanceVnd = transportAllowanceVnd,
            grossSalaryVnd = grossSalaryVnd,
            socialInsuranceVnd = socialInsuranceVnd,
            healthInsuranceVnd = healthInsuranceVnd,
            unemploymentInsVnd = unemploymentInsVnd,
            personalIncomeTaxVnd = personalIncomeTaxVnd,
            advancePaidVnd = advancePaidVnd,
            netSalaryVnd = netSalaryVnd,
            isFinalized = isFinalized,
            payDate = payDate
        )
    }
}

// ==================== TICKET MODELS ====================

@JsonClass(generateAdapter = true)
data class CreateTicketRequest(
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "amountVnd") val amountVnd: Long? = null,
    @Json(name = "requestedDays") val requestedDays: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTicketStatusRequest(
    @Json(name = "status") val status: String,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class TicketItemResponse(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String,
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "amountVnd") val amountVnd: Long? = null,
    @Json(name = "requestedDays") val requestedDays: String? = null,
    @Json(name = "createdDate") val createdDate: String,
    @Json(name = "status") val status: String,
    @Json(name = "requesterName") val requesterName: String,
    @Json(name = "requesterCode") val requesterCode: String,
    @Json(name = "hrNote") val hrNote: String = ""
) {
    fun toDomain(): TicketItem {
        val domainType = when (type.uppercase()) {
            "ADVANCE_SALARY" -> TicketType.ADVANCE_SALARY
            "TIMESHEET_DISPUTE" -> TicketType.TIMESHEET_DISPUTE
            "LEAVE_REQUEST" -> TicketType.LEAVE_REQUEST
            else -> TicketType.OTHER
        }

        val domainStatus = when (status.uppercase()) {
            "HR_APPROVED" -> TicketStatus.HR_APPROVED
            "APPROVED" -> TicketStatus.APPROVED
            "PAID" -> TicketStatus.PAID
            "REJECTED" -> TicketStatus.REJECTED
            "CLOSED" -> TicketStatus.CLOSED
            else -> TicketStatus.PENDING
        }

        return TicketItem(
            id = id,
            code = code,
            type = domainType,
            title = title,
            description = description,
            amountVnd = amountVnd,
            requestedDays = requestedDays,
            createdDate = createdDate,
            status = domainStatus,
            requesterName = requesterName,
            requesterCode = requesterCode,
            hrNote = hrNote
        )
    }
}

// ==================== JOBS & CTV MODELS ====================

@JsonClass(generateAdapter = true)
data class JobOpeningResponse(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "factoryCompany") val factoryCompany: String,
    @Json(name = "industrialZone") val industrialZone: String,
    @Json(name = "salaryRange") val salaryRange: String,
    @Json(name = "slotsRemaining") val slotsRemaining: Int,
    @Json(name = "commissionVnd") val commissionVnd: Long,
    @Json(name = "benefits") val benefits: List<String>,
    @Json(name = "requirements") val requirements: String,
    @Json(name = "urgent") val urgent: Boolean = false
) {
    fun toDomain(): JobOpening {
        return JobOpening(
            id = id,
            title = title,
            factoryCompany = factoryCompany,
            industrialZone = industrialZone,
            salaryRange = salaryRange,
            slotsRemaining = slotsRemaining,
            commissionVnd = commissionVnd,
            benefits = benefits,
            requirements = requirements,
            urgent = urgent
        )
    }
}

@JsonClass(generateAdapter = true)
data class CandidateRegistrationRequest(
    @Json(name = "candidateName") val candidateName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "jobTitle") val jobTitle: String
)

@JsonClass(generateAdapter = true)
data class CtvCandidateResponse(
    @Json(name = "id") val id: String,
    @Json(name = "candidateName") val candidateName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "appliedJob") val appliedJob: String,
    @Json(name = "referralDate") val referralDate: String,
    @Json(name = "status") val status: String,
    @Json(name = "rewardVnd") val rewardVnd: Long,
    @Json(name = "isPaid") val isPaid: Boolean
) {
    fun toDomain(): CtvCandidate {
        return CtvCandidate(
            id = id,
            candidateName = candidateName,
            phone = phone,
            appliedJob = appliedJob,
            referralDate = referralDate,
            status = status,
            rewardVnd = rewardVnd,
            isPaid = isPaid
        )
    }
}

// ==================== BOD OVERVIEW ====================

@JsonClass(generateAdapter = true)
data class BodOverviewResponse(
    @Json(name = "activeWorkersCount") val activeWorkersCount: Int,
    @Json(name = "workerGrowthPercent") val workerGrowthPercent: Double,
    @Json(name = "fillRatePercent") val fillRatePercent: Double,
    @Json(name = "totalPayrollVnd") val totalPayrollVnd: Long,
    @Json(name = "recruitmentSlaDays") val recruitmentSlaDays: Double,
    @Json(name = "pendingTicketsCount") val pendingTicketsCount: Int,
    @Json(name = "attendanceComplianceRate") val attendanceComplianceRate: Double
) {
    fun toDomain(): BodOverviewStats {
        return BodOverviewStats(
            activeWorkersCount = activeWorkersCount,
            workerGrowthPercent = workerGrowthPercent,
            fillRatePercent = fillRatePercent,
            totalPayrollVnd = totalPayrollVnd,
            recruitmentSlaDays = recruitmentSlaDays,
            pendingTicketsCount = pendingTicketsCount,
            attendanceComplianceRate = attendanceComplianceRate
        )
    }
}

// ==================== PUSH NOTIFICATION MODELS ====================

@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    @Json(name = "token") val token: String,
    @Json(name = "deviceType") val deviceType: String = "ANDROID",
    @Json(name = "model") val model: String? = null
)
