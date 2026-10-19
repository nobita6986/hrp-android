package com.example.data.api.model

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
import com.example.data.model.TicketItem
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.data.model.WorkerVerificationInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Standardized Mobile API Response Envelope per web-android-contract.md (Section 6.1 & 14.3.A):
 * Success: { "ok": true, "data": { ... } }
 * Error:   { "ok": false, "error": { "code": "...", "message": "...", "details": {} } }
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @param:Json(name = "ok") val ok: Boolean = true,
    @param:Json(name = "success") val success: Boolean = true,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "data") val data: T? = null,
    @param:Json(name = "error") val error: ApiError? = null
) {
    val isSuccessful: Boolean
        get() = ok && error == null
}

@JsonClass(generateAdapter = true)
data class ApiError(
    @param:Json(name = "code") val code: String = "UNKNOWN_ERROR",
    @param:Json(name = "message") val message: String = "Đã xảy ra lỗi không xác định",
    @param:Json(name = "details") val details: Map<String, Any?>? = null
)

/**
 * Domain exception carrying contract error code and HTTP status
 */
class HrpApiException(
    val code: String,
    override val message: String,
    val httpCode: Int = 0
) : Exception(message)

/**
 * Utility to parse Retrofit HttpException or envelope errors into standardized HrpApiException
 */
object ApiErrorParser {

    fun fromApiError(error: ApiError?, httpCode: Int = 400, fallbackMessage: String = "Yêu cầu không thành công"): HrpApiException {
        val code = error?.code ?: mapHttpCodeToDefaultErrorCode(httpCode)
        val rawMessage = error?.message?.takeIf { it.isNotBlank() }
        val friendlyMessage = translateErrorCode(code, rawMessage, fallbackMessage)
        return HrpApiException(code = code, message = friendlyMessage, httpCode = httpCode)
    }

    fun parseThrowable(t: Throwable, fallbackMessage: String = "Không thể kết nối máy chủ. Vui lòng thử lại."): HrpApiException {
        if (t is HrpApiException) return t

        if (t is HttpException) {
            val httpCode = t.code()
            val errorBodyStr = try {
                t.response()?.errorBody()?.string()
            } catch (_: Throwable) {
                null
            }

            if (!errorBodyStr.isNullOrBlank()) {
                try {
                    val root = JSONObject(errorBodyStr)
                    val errObj = root.optJSONObject("error")
                    if (errObj != null) {
                        val code = errObj.optString("code", mapHttpCodeToDefaultErrorCode(httpCode))
                        val msg = errObj.optString("message", "").takeIf { it.isNotBlank() }
                        return HrpApiException(
                            code = code,
                            message = translateErrorCode(code, msg, fallbackMessage),
                            httpCode = httpCode
                        )
                    }
                    // Fallback for legacy root JSON errors: { "error": "...", "message": "...", "code": "..." }
                    val topCode = root.optString("code", "").ifBlank { mapHttpCodeToDefaultErrorCode(httpCode) }
                    val topMsg = root.optString("message", "").ifBlank { root.optString("error", "") }.takeIf { it.isNotBlank() }
                    return HrpApiException(
                        code = topCode,
                        message = translateErrorCode(topCode, topMsg, fallbackMessage),
                        httpCode = httpCode
                    )
                } catch (_: Throwable) {
                    // Ignore JSON parse failure and use HTTP status mapping
                }
            }

            val defaultCode = mapHttpCodeToDefaultErrorCode(httpCode)
            return HrpApiException(
                code = defaultCode,
                message = translateErrorCode(defaultCode, null, fallbackMessage),
                httpCode = httpCode
            )
        }

        return HrpApiException(
            code = "NETWORK_ERROR",
            message = t.message?.takeIf { it.isNotBlank() } ?: fallbackMessage,
            httpCode = 0
        )
    }

    private fun mapHttpCodeToDefaultErrorCode(httpCode: Int): String = when (httpCode) {
        401 -> "UNAUTHORIZED"
        403 -> "FORBIDDEN_NOT_VERIFIED"
        404 -> "NOT_FOUND"
        409 -> "CONFLICT"
        410 -> "ENDPOINT_RETIRED"
        422 -> "VALIDATION_ERROR"
        429 -> "RATE_LIMIT_EXCEEDED"
        503 -> "SERVICE_UNAVAILABLE"
        else -> "HTTP_$httpCode"
    }

    fun translateErrorCode(code: String, serverMessage: String?, fallback: String): String {
        return when (code.uppercase()) {
            "UNAUTHORIZED" -> serverMessage ?: "Phiên đăng nhập đã hết hạn hoặc thông tin xác thực không hợp lệ."
            "FORBIDDEN_NOT_VERIFIED" -> serverMessage ?: "Tài khoản chưa hoàn tất xác thực công nhân HRP để thực hiện chức năng này."
            "OTP_SERVICE_UNAVAILABLE" -> "Hệ thống gửi mã OTP đang bảo trì, vui lòng thử lại sau."
            "OTP_INVALID_OR_EXPIRED" -> serverMessage ?: "Mã OTP không chính xác hoặc đã hết hạn."
            "WORKER_VERIFICATION_PENDING" -> serverMessage ?: "Bạn đang có một hồ sơ xác thực chờ HR xét duyệt, không thể gửi trùng."
            "REFERRAL_PROGRAM_DISABLED" -> serverMessage ?: "Chương trình giới thiệu người làm hiện chưa mở hoặc đã hết hiệu lực."
            "REFERRAL_CONSENT_REQUIRED" -> serverMessage ?: "Vui lòng xác nhận người được giới thiệu đã đồng ý chia sẻ thông tin."
            "GEOFENCE_OUT_OF_RANGE" -> serverMessage ?: "Vị trí GPS của bạn nằm ngoài bán kính khu vực nhà máy/dự án được phân công."
            "GPS_ACCURACY_TOO_LOW" -> serverMessage ?: "Tín hiệu GPS quá yếu (sai số lớn). Vui lòng di chuyển ra khu vực thoáng và thử lại."
            "SITE_GEOFENCE_NOT_CONFIGURED" -> serverMessage ?: "Địa điểm dự án chưa được cấu hình tọa độ chấm công trên hệ thống. Vui lòng báo quản lý."
            "RATE_LIMIT_EXCEEDED" -> serverMessage ?: "Bạn thao tác quá nhanh. Vui lòng đợi ít phút trước khi thử lại."
            else -> serverMessage ?: fallback
        }
    }
}

// ==================== 1 & 2. AUTH & OTP MODELS ====================

/**
 * Endpoint 1: POST /api/auth/register
 */
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @param:Json(name = "phone") val phone: String,
    @param:Json(name = "password") val password: String,
    @param:Json(name = "fullName") val fullName: String
)

@JsonClass(generateAdapter = true)
data class RegisterData(
    @param:Json(name = "userId") val userId: String = "",
    @param:Json(name = "phone") val phone: String = "",
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false
)

/**
 * Endpoint 2: POST /api/auth/login (Root JSON - Web additive compatibility)
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @param:Json(name = "phone") val phone: String,
    @param:Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AccountUserDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "phone") val phone: String? = null,
    @param:Json(name = "fullName") val fullName: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @param:Json(name = "verificationStatus") val verificationStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @param:Json(name = "ok") val ok: Boolean = true,
    @param:Json(name = "success") val success: Boolean = true,
    @param:Json(name = "redirectTo") val redirectTo: String? = null,
    @param:Json(name = "token") val token: String? = null,
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "user") val user: AccountUserDto? = null,
    @param:Json(name = "worker") val worker: WorkerProfileResponse? = null,
    @param:Json(name = "message") val message: String? = null
)

/**
 * Endpoint 3: POST /api/auth/phone-otp/send
 */
@JsonClass(generateAdapter = true)
data class OtpSendRequest(
    @param:Json(name = "phone") val phone: String
)

@JsonClass(generateAdapter = true)
data class OtpSendData(
    @param:Json(name = "sent") val sent: Boolean = false,
    @param:Json(name = "cooldownSeconds") val cooldownSeconds: Int = 60
)

/**
 * Endpoint 4: POST /api/auth/phone-otp/verify
 */
@JsonClass(generateAdapter = true)
data class OtpVerifyRequest(
    @param:Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class OtpVerifyData(
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @param:Json(name = "verifiedAt") val verifiedAt: String? = null
)

// ==================== 5, 6, 7. PROJECTS & WORKER VERIFICATION ====================

/**
 * Endpoint 5: GET /api/public/projects
 */
@JsonClass(generateAdapter = true)
data class PublicProjectDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "code") val code: String = "",
    @param:Json(name = "name") val name: String,
    @param:Json(name = "location") val location: String? = null,
    @param:Json(name = "siteAddress") val siteAddress: String? = null
) {
    fun toDomain(): PublicProject {
        return PublicProject(
            id = id,
            code = code,
            name = name,
            location = location ?: siteAddress ?: ""
        )
    }
}

@JsonClass(generateAdapter = true)
data class PublicProjectsData(
    @param:Json(name = "items") val items: List<PublicProjectDto>? = null,
    @param:Json(name = "projects") val projects: List<PublicProjectDto>? = null
) {
    fun resolvedProjects(): List<PublicProject> {
        return (items ?: projects ?: emptyList()).map { it.toDomain() }
    }
}

/**
 * Endpoint 6: POST /api/worker-verifications
 */
@JsonClass(generateAdapter = true)
data class WorkerVerificationRequest(
    @param:Json(name = "fullName") val fullName: String,
    @param:Json(name = "cccdNumber") val cccdNumber: String,
    @param:Json(name = "dob") val dob: String,
    @param:Json(name = "projectId") val projectId: String,
    @param:Json(name = "employeeCode") val employeeCode: String
)

@JsonClass(generateAdapter = true)
data class WorkerVerificationSubmitData(
    @param:Json(name = "verificationId") val verificationId: String = "",
    @param:Json(name = "status") val status: String = "PENDING_REVIEW",
    @param:Json(name = "submittedAt") val submittedAt: String? = null
)

/**
 * Endpoint 7: GET /api/worker-verifications/me
 */
@JsonClass(generateAdapter = true)
data class WorkerVerificationDetailDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "status") val status: String = "PENDING_REVIEW",
    @param:Json(name = "reviewNote") val reviewNote: String? = null,
    @param:Json(name = "projectId") val projectId: String? = null,
    @param:Json(name = "projectName") val projectName: String? = null,
    @param:Json(name = "employeeCode") val employeeCode: String? = null,
    @param:Json(name = "submittedAt") val submittedAt: String? = null,
    @param:Json(name = "reviewedAt") val reviewedAt: String? = null
) {
    fun toDomain(): WorkerVerificationInfo {
        return WorkerVerificationInfo(
            id = id,
            status = VerificationStatus.fromString(status),
            reviewNote = reviewNote,
            projectId = projectId,
            projectName = projectName,
            employeeCode = employeeCode,
            submittedAt = submittedAt,
            reviewedAt = reviewedAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class WorkerVerificationMeData(
    @param:Json(name = "hasVerification") val hasVerification: Boolean = false,
    @param:Json(name = "verification") val verification: WorkerVerificationDetailDto? = null
)

/**
 * Worker Profile (GET /api/workers/me or nested in LoginResponse)
 */
@JsonClass(generateAdapter = true)
data class WorkerProfileResponse(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "employeeCode") val employeeCode: String = "",
    @param:Json(name = "fullName") val fullName: String = "",
    @param:Json(name = "cccdNumber") val cccdNumber: String = "",
    @param:Json(name = "phone") val phone: String = "",
    @param:Json(name = "factoryLocation") val factoryLocation: String = "",
    @param:Json(name = "projectCode") val projectCode: String = "",
    @param:Json(name = "department") val department: String = "",
    @param:Json(name = "position") val position: String = "",
    @param:Json(name = "baseSalaryPerDayVnd") val baseSalaryPerDayVnd: Long = 0L,
    @param:Json(name = "joinedDate") val joinedDate: String = "",
    @param:Json(name = "bankAccount") val bankAccount: String = "",
    @param:Json(name = "status") val status: String = "ACTIVE",
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @param:Json(name = "verificationStatus") val verificationStatus: String? = null,
    @param:Json(name = "hasActiveAssignment") val hasActiveAssignment: Boolean = false
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
            status = status,
            phoneVerified = phoneVerified,
            verificationStatus = VerificationStatus.fromString(verificationStatus),
            hasActiveAssignment = hasActiveAssignment
        )
    }
}

// ==================== 8 & 9. PUBLIC JOBS & APPLICATIONS ====================

/**
 * Endpoint 8: GET /api/jobs (Root object { jobs, nextOffset, total, facets, overview })
 */
@JsonClass(generateAdapter = true)
data class JobsRootResponse(
    @param:Json(name = "jobs") val jobs: List<JobOpeningResponse> = emptyList(),
    @param:Json(name = "nextOffset") val nextOffset: Int? = null,
    @param:Json(name = "total") val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class JobOpeningResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "slug") val slug: String? = null,
    @param:Json(name = "title") val title: String = "",
    @param:Json(name = "companyName") val companyName: String? = null,
    @param:Json(name = "factoryCompany") val factoryCompany: String? = null,
    @param:Json(name = "position") val position: String? = null,
    @param:Json(name = "shift") val shift: String? = null,
    @param:Json(name = "location") val location: String? = null,
    @param:Json(name = "industrialZone") val industrialZone: String? = null,
    @param:Json(name = "shiftType") val shiftType: String? = null,
    @param:Json(name = "jobType") val jobType: String? = null,
    @param:Json(name = "availableSlots") val availableSlots: Int? = null,
    @param:Json(name = "slotsRemaining") val slotsRemaining: Int? = null,
    @param:Json(name = "deadline") val deadline: String? = null,
    @param:Json(name = "statusLabel") val statusLabel: String? = null,
    @param:Json(name = "salaryMinVnd") val salaryMinVnd: Long? = null,
    @param:Json(name = "salaryMaxVnd") val salaryMaxVnd: Long? = null,
    @param:Json(name = "urgency") val urgency: String? = null,
    @param:Json(name = "isHot") val isHot: Boolean = false,
    @param:Json(name = "isUrgent") val isUrgent: Boolean = false,
    @param:Json(name = "urgent") val urgent: Boolean = false,
    @param:Json(name = "isHighReward") val isHighReward: Boolean = false,
    @param:Json(name = "isExpiringSoon") val isExpiringSoon: Boolean = false,
    @param:Json(name = "salaryDisplay") val salaryDisplay: String? = null,
    @param:Json(name = "salaryRange") val salaryRange: String? = null,
    @param:Json(name = "postedAt") val postedAt: String? = null,
    @param:Json(name = "positionTitles") val positionTitles: List<String> = emptyList(),
    @param:Json(name = "locations") val locations: List<String> = emptyList(),
    @param:Json(name = "shifts") val shifts: List<String> = emptyList(),
    @param:Json(name = "commissionVnd") val commissionVnd: Long = 0L,
    @param:Json(name = "benefits") val benefits: List<String> = emptyList(),
    @param:Json(name = "requirements") val requirements: String = ""
) {
    fun toDomain(): JobOpening {
        val resolvedSalary = salaryDisplay
            ?: salaryRange
            ?: if (salaryMinVnd != null && salaryMaxVnd != null) {
                "${salaryMinVnd / 1_000_000} - ${salaryMaxVnd / 1_000_000} Triệu đ/tháng"
            } else {
                "Thỏa thuận"
            }

        val resolvedBenefits = benefits.ifEmpty {
            listOfNotNull(
                shift?.let { "Ca: $it" },
                jobType?.let { "Loại: $it" },
                statusLabel
            )
        }

        return JobOpening(
            id = id,
            slug = slug?.takeIf { it.isNotBlank() } ?: id,
            title = title,
            factoryCompany = companyName ?: factoryCompany ?: "Đối tác nhà máy HRP",
            industrialZone = location ?: industrialZone ?: locations.firstOrNull() ?: "Khu công nghiệp",
            salaryRange = resolvedSalary,
            slotsRemaining = availableSlots ?: slotsRemaining ?: 0,
            commissionVnd = commissionVnd,
            benefits = resolvedBenefits,
            requirements = requirements,
            urgent = isUrgent || urgent || isHot,
            shift = shift,
            position = position,
            deadline = deadline
        )
    }
}

/**
 * Endpoint 9: POST /api/public/jobs/{slug}/applications (Root JSON response { trackingCode, status })
 */
@JsonClass(generateAdapter = true)
data class JobApplicationRequest(
    @param:Json(name = "fullName") val fullName: String,
    @param:Json(name = "phone") val phone: String? = null,
    @param:Json(name = "experience") val experience: String? = null,
    @param:Json(name = "dateOfBirth") val dateOfBirth: String? = null,
    @param:Json(name = "gender") val gender: String? = null,
    @param:Json(name = "consent") val consent: Boolean = true
)

@JsonClass(generateAdapter = true)
data class JobApplicationResponse(
    @param:Json(name = "trackingCode") val trackingCode: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "message") val message: String? = null
)

// ==================== 10 & 11. ATTENDANCE & CHECK-IN EVENTS ====================

/**
 * Endpoint 10: GET /api/worker/attendance (Root JSON { items, total })
 */
@JsonClass(generateAdapter = true)
data class WorkerAttendanceRootResponse(
    @param:Json(name = "items") val items: List<AttendanceDayResponse> = emptyList(),
    @param:Json(name = "total") val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class AttendanceDayResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "workDate") val workDate: String? = null,
    @param:Json(name = "dateString") val dateString: String? = null,
    @param:Json(name = "dayOfWeek") val dayOfWeek: String = "",
    @param:Json(name = "shiftName") val shiftName: String = "Ca tiêu chuẩn",
    @param:Json(name = "checkInTime") val checkInTime: String? = null,
    @param:Json(name = "checkOutTime") val checkOutTime: String? = null,
    @param:Json(name = "standardHours") val standardHours: Double = 8.0,
    @param:Json(name = "otHours") val otHours: Double = 0.0,
    @param:Json(name = "source") val source: String? = "GPS",
    @param:Json(name = "geofenceResult") val geofenceResult: String? = null,
    @param:Json(name = "riskFlag") val riskFlag: Boolean = false,
    @param:Json(name = "locationVerified") val locationVerified: Boolean? = null,
    @param:Json(name = "factorySite") val factorySite: String = "Nhà máy HRP",
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "note") val note: String = ""
) {
    fun toDomain(): AttendanceDay {
        val resolvedDate = workDate ?: dateString ?: ""
        val isInsideGeofence = locationVerified ?: (geofenceResult.equals("INSIDE", ignoreCase = true))
        val domainStatus = when {
            status != null -> when (status.uppercase()) {
                "APPROVED" -> AttendanceStatus.APPROVED
                "DISPUTED" -> AttendanceStatus.DISPUTED
                "ABSENT" -> AttendanceStatus.ABSENT
                else -> AttendanceStatus.PENDING
            }
            riskFlag || geofenceResult.equals("OUTSIDE", ignoreCase = true) -> AttendanceStatus.DISPUTED
            checkInTime != null && isInsideGeofence -> AttendanceStatus.APPROVED
            else -> AttendanceStatus.PENDING
        }

        return AttendanceDay(
            id = id,
            dateString = resolvedDate,
            dayOfWeek = dayOfWeek.ifBlank { resolvedDate },
            shiftName = shiftName,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            standardHours = standardHours,
            otHours = otHours,
            locationVerified = isInsideGeofence,
            factorySite = factorySite,
            status = domainStatus,
            note = note,
            geofenceResult = geofenceResult,
            riskFlag = riskFlag
        )
    }
}

/**
 * Endpoint 11: POST /api/worker/checkins/events (Mobile Envelope)
 * Header: Idempotency-Key: <UUID>
 */
@JsonClass(generateAdapter = true)
data class CheckInEventRequest(
    @param:Json(name = "workDate") val workDate: String,
    @param:Json(name = "action") val action: String, // "CHECK_IN" | "CHECK_OUT"
    @param:Json(name = "gpsLatitude") val gpsLatitude: Double,
    @param:Json(name = "gpsLongitude") val gpsLongitude: Double,
    @param:Json(name = "gpsAccuracyMeters") val gpsAccuracyMeters: Float,
    @param:Json(name = "capturedAt") val capturedAt: String
)

@JsonClass(generateAdapter = true)
data class CheckInEventData(
    @param:Json(name = "attendanceId") val attendanceId: String = "",
    @param:Json(name = "action") val action: String = "CHECK_IN",
    @param:Json(name = "recordedAt") val recordedAt: String = "",
    @param:Json(name = "geofenceResult") val geofenceResult: String? = null
)

// Legacy Check-in DTOs kept for compatibility
@JsonClass(generateAdapter = true)
data class CheckInRequest(
    @param:Json(name = "latitude") val latitude: Double,
    @param:Json(name = "longitude") val longitude: Double,
    @param:Json(name = "accuracy") val accuracy: Float? = null,
    @param:Json(name = "factorySite") val factorySite: String? = null,
    @param:Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckOutRequest(
    @param:Json(name = "latitude") val latitude: Double? = null,
    @param:Json(name = "longitude") val longitude: Double? = null,
    @param:Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckInResponse(
    @param:Json(name = "success") val success: Boolean = true,
    @param:Json(name = "message") val message: String = "",
    @param:Json(name = "attendance") val attendance: AttendanceDayResponse? = null
)

// ==================== 12 & 13. FEEDBACK MODELS ====================

/**
 * Endpoint 12: POST /api/feedback
 */
@JsonClass(generateAdapter = true)
data class FeedbackSubmitRequest(
    @param:Json(name = "category") val category: String, // WORK_ENVIRONMENT | SALARY_BENEFITS | SUGGESTION | COMPLAINT | OTHER
    @param:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class FeedbackSubmitData(
    @param:Json(name = "feedbackId") val feedbackId: String = "",
    @param:Json(name = "status") val status: String = "RECEIVED",
    @param:Json(name = "submittedAt") val submittedAt: String? = null
)

/**
 * Endpoint 13: GET /api/feedback/mine
 */
@JsonClass(generateAdapter = true)
data class FeedbackItemDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "category") val category: String = "OTHER",
    @param:Json(name = "content") val content: String = "",
    @param:Json(name = "status") val status: String = "RECEIVED",
    @param:Json(name = "adminResponse") val adminResponse: String? = null,
    @param:Json(name = "respondedAt") val respondedAt: String? = null,
    @param:Json(name = "createdAt") val createdAt: String? = null
) {
    fun toDomain(): FeedbackItem {
        return FeedbackItem(
            id = id,
            category = FeedbackCategory.fromCode(category),
            content = content,
            status = status,
            adminResponse = adminResponse,
            respondedAt = respondedAt,
            createdAt = createdAt ?: ""
        )
    }

    fun toTicketItem(): TicketItem {
        val cat = FeedbackCategory.fromCode(category)
        val ticketStatus = when (status.uppercase()) {
            "RESOLVED" -> TicketStatus.APPROVED
            "IN_REVIEW" -> TicketStatus.HR_APPROVED
            "REJECTED" -> TicketStatus.REJECTED
            else -> TicketStatus.PENDING
        }
        return TicketItem(
            id = id,
            code = "FB-${id.takeLast(4).uppercase()}",
            type = TicketType.OTHER,
            title = cat.label,
            description = content,
            createdDate = createdAt ?: respondedAt ?: "Gần đây",
            status = ticketStatus,
            requesterName = "Người lao động",
            requesterCode = "",
            hrNote = adminResponse ?: ""
        )
    }
}

@JsonClass(generateAdapter = true)
data class FeedbackListData(
    @param:Json(name = "items") val items: List<FeedbackItemDto> = emptyList()
)

// ==================== 14, 15, 16. REFERRAL MODELS ====================

/**
 * Endpoint 14: GET /api/referrals/program
 */
@JsonClass(generateAdapter = true)
data class ReferralProgramDto(
    @param:Json(name = "isEnabled") val isEnabled: Boolean = false,
    @param:Json(name = "policyVersion") val policyVersion: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "eligibilityCriteria") val eligibilityCriteria: String? = null,
    @param:Json(name = "successEventDescription") val successEventDescription: String? = null,
    @param:Json(name = "rewardPolicyDescription") val rewardPolicyDescription: String? = null,
    @param:Json(name = "termsAndConditions") val termsAndConditions: String? = null,
    @param:Json(name = "effectiveFrom") val effectiveFrom: String? = null,
    @param:Json(name = "effectiveUntil") val effectiveUntil: String? = null,
    @param:Json(name = "canSubmit") val canSubmit: Boolean = false,
    @param:Json(name = "blockedReason") val blockedReason: String? = null
) {
    fun toDomain(): ReferralProgram {
        return ReferralProgram(
            isEnabled = isEnabled,
            policyVersion = policyVersion,
            title = title,
            eligibilityCriteria = eligibilityCriteria,
            successEventDescription = successEventDescription,
            rewardPolicyDescription = rewardPolicyDescription,
            termsAndConditions = termsAndConditions,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil,
            canSubmit = canSubmit,
            blockedReason = blockedReason
        )
    }
}

@JsonClass(generateAdapter = true)
data class ReferralProgramWrapper(
    @param:Json(name = "program") val program: ReferralProgramDto? = null,
    @param:Json(name = "isEnabled") val isEnabled: Boolean? = null,
    @param:Json(name = "policyVersion") val policyVersion: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "eligibilityCriteria") val eligibilityCriteria: String? = null,
    @param:Json(name = "successEventDescription") val successEventDescription: String? = null,
    @param:Json(name = "rewardPolicyDescription") val rewardPolicyDescription: String? = null,
    @param:Json(name = "termsAndConditions") val termsAndConditions: String? = null,
    @param:Json(name = "effectiveFrom") val effectiveFrom: String? = null,
    @param:Json(name = "effectiveUntil") val effectiveUntil: String? = null,
    @param:Json(name = "canSubmit") val canSubmit: Boolean? = null,
    @param:Json(name = "blockedReason") val blockedReason: String? = null
) {
    fun resolvedProgram(): ReferralProgram {
        program?.let { return it.toDomain() }
        return ReferralProgram(
            isEnabled = isEnabled ?: false,
            policyVersion = policyVersion,
            title = title,
            eligibilityCriteria = eligibilityCriteria,
            successEventDescription = successEventDescription,
            rewardPolicyDescription = rewardPolicyDescription,
            termsAndConditions = termsAndConditions,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil,
            canSubmit = canSubmit ?: false,
            blockedReason = blockedReason
        )
    }
}

/**
 * Endpoint 15: POST /api/referrals
 * Header: Idempotency-Key: <UUID>
 */
@JsonClass(generateAdapter = true)
data class ReferralSubmitRequest(
    @param:Json(name = "referredName") val referredName: String,
    @param:Json(name = "referredPhone") val referredPhone: String,
    @param:Json(name = "jobInterest") val jobInterest: String? = null,
    @param:Json(name = "consentToContact") val consentToContact: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReferralSubmittedDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "status") val status: String = "SUBMITTED",
    @param:Json(name = "submittedAt") val submittedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ReferralSubmitWrapper(
    @param:Json(name = "referral") val referral: ReferralSubmittedDto? = null
)

/**
 * Endpoint 16: GET /api/referrals/mine
 */
@JsonClass(generateAdapter = true)
data class ReferralItemDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "maskedReferredName") val maskedReferredName: String = "",
    @param:Json(name = "maskedReferredPhone") val maskedReferredPhone: String = "",
    @param:Json(name = "status") val status: String = "SUBMITTED",
    @param:Json(name = "benefitStatus") val benefitStatus: String = "NONE",
    @param:Json(name = "benefitAmountVnd") val benefitAmountVnd: Long? = null,
    @param:Json(name = "submittedAt") val submittedAt: String? = null,
    @param:Json(name = "updatedAt") val updatedAt: String? = null
) {
    fun toDomain(): ReferralItem {
        return ReferralItem(
            id = id,
            maskedReferredName = maskedReferredName,
            maskedReferredPhone = maskedReferredPhone,
            status = status,
            benefitStatus = benefitStatus,
            benefitAmountVnd = benefitAmountVnd,
            submittedAt = submittedAt ?: "",
            updatedAt = updatedAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class ReferralListData(
    @param:Json(name = "items") val items: List<ReferralItemDto> = emptyList()
)

// ==================== 17. PUSH NOTIFICATION MODELS ====================

/**
 * Endpoint 17: POST /api/push/register-device
 */
@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    @param:Json(name = "fcmToken") val fcmToken: String,
    @param:Json(name = "platform") val platform: String = "ANDROID"
)

@JsonClass(generateAdapter = true)
data class PushRegisterData(
    @param:Json(name = "registered") val registered: Boolean = true
)

// ==================== LEGACY / DEFERRED MODELS ====================

@JsonClass(generateAdapter = true)
data class PayslipSummaryResponse(
    @param:Json(name = "monthPeriod") val monthPeriod: String,
    @param:Json(name = "standardWorkDays") val standardWorkDays: Int,
    @param:Json(name = "actualWorkDays") val actualWorkDays: Double,
    @param:Json(name = "otHours") val otHours: Double,
    @param:Json(name = "baseSalaryVnd") val baseSalaryVnd: Long,
    @param:Json(name = "otPayVnd") val otPayVnd: Long,
    @param:Json(name = "attendanceBonusVnd") val attendanceBonusVnd: Long,
    @param:Json(name = "mealAllowanceVnd") val mealAllowanceVnd: Long,
    @param:Json(name = "transportAllowanceVnd") val transportAllowanceVnd: Long,
    @param:Json(name = "grossSalaryVnd") val grossSalaryVnd: Long,
    @param:Json(name = "socialInsuranceVnd") val socialInsuranceVnd: Long,
    @param:Json(name = "healthInsuranceVnd") val healthInsuranceVnd: Long,
    @param:Json(name = "unemploymentInsVnd") val unemploymentInsVnd: Long,
    @param:Json(name = "personalIncomeTaxVnd") val personalIncomeTaxVnd: Long,
    @param:Json(name = "advancePaidVnd") val advancePaidVnd: Long,
    @param:Json(name = "netSalaryVnd") val netSalaryVnd: Long,
    @param:Json(name = "isFinalized") val isFinalized: Boolean,
    @param:Json(name = "payDate") val payDate: String
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

@JsonClass(generateAdapter = true)
data class CreateTicketRequest(
    @param:Json(name = "type") val type: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "amountVnd") val amountVnd: Long? = null,
    @param:Json(name = "requestedDays") val requestedDays: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTicketStatusRequest(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class TicketItemResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "code") val code: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "amountVnd") val amountVnd: Long? = null,
    @param:Json(name = "requestedDays") val requestedDays: String? = null,
    @param:Json(name = "createdDate") val createdDate: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "requesterName") val requesterName: String,
    @param:Json(name = "requesterCode") val requesterCode: String,
    @param:Json(name = "hrNote") val hrNote: String = ""
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

@JsonClass(generateAdapter = true)
data class CandidateRegistrationRequest(
    @param:Json(name = "candidateName") val candidateName: String,
    @param:Json(name = "phone") val phone: String,
    @param:Json(name = "jobTitle") val jobTitle: String
)

@JsonClass(generateAdapter = true)
data class CtvCandidateResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "candidateName") val candidateName: String,
    @param:Json(name = "phone") val phone: String,
    @param:Json(name = "appliedJob") val appliedJob: String,
    @param:Json(name = "referralDate") val referralDate: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "rewardVnd") val rewardVnd: Long,
    @param:Json(name = "isPaid") val isPaid: Boolean
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

@JsonClass(generateAdapter = true)
data class BodOverviewResponse(
    @param:Json(name = "activeWorkersCount") val activeWorkersCount: Int,
    @param:Json(name = "workerGrowthPercent") val workerGrowthPercent: Double,
    @param:Json(name = "fillRatePercent") val fillRatePercent: Double,
    @param:Json(name = "totalPayrollVnd") val totalPayrollVnd: Long,
    @param:Json(name = "recruitmentSlaDays") val recruitmentSlaDays: Double,
    @param:Json(name = "pendingTicketsCount") val pendingTicketsCount: Int,
    @param:Json(name = "attendanceComplianceRate") val attendanceComplianceRate: Double
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
