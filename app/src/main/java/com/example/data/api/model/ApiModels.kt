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
import com.example.data.model.RewardAttendancePolicy
import com.example.data.model.TicketItem
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerAssignmentSite
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
    @param:Json(name = "ok") val ok: Boolean = false,
    @param:Json(name = "success") val success: Boolean = false,
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
    private val RAW_ERROR_CODE_REGEX = Regex("^[A-Z0-9_]+$")

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
                    val topCode = root.optString("code", "").ifBlank {
                        root.optString("error", "").takeIf { it.all { ch -> ch.isUpperCase() || ch.isDigit() || ch == '_' } }
                            ?: mapHttpCodeToDefaultErrorCode(httpCode)
                    }
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
        val cleanMessage = serverMessage
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(code, ignoreCase = true) && !RAW_ERROR_CODE_REGEX.matches(it) }

        return when (code.uppercase()) {
            "UNAUTHORIZED" -> cleanMessage ?: "Phiên đăng nhập đã hết hạn hoặc thông tin xác thực không hợp lệ."
            "PHONE_ALREADY_REGISTERED" ->
                cleanMessage ?: "Không thể tạo tài khoản với thông tin này. Nếu đã có tài khoản, vui lòng đăng nhập."
            "REGISTRATION_UNAVAILABLE" ->
                cleanMessage ?: "Dịch vụ đăng ký tạm thời gián đoạn. Vui lòng thử lại sau."
            "PROFILE_INCOMPLETE" ->
                cleanMessage ?: "Tài khoản chưa đủ họ tên và số điện thoại hợp lệ để ứng tuyển."
            "VALIDATION_ERROR", "VALIDATION", "INVALID_INPUT" ->
                cleanMessage ?: "Thông tin nhập vào chưa hợp lệ. Vui lòng kiểm tra lại."
            "FORBIDDEN_NOT_VERIFIED", "FORBIDDEN", "NOT_A_WORKER" ->
                cleanMessage ?: "Tài khoản chưa được Quản trị viên liên kết với hồ sơ Người lao động và phân công dự án (ACTIVE)."
            "WORKER_PROFILE_NOT_FOUND", "WORKER_NOT_FOUND" ->
                cleanMessage ?: "Tài khoản chưa được Quản trị viên liên kết với hồ sơ Người lao động."
            "PHONE_NOT_VERIFIED" ->
                cleanMessage ?: "Cần xác thực số điện thoại bằng mã OTP trước khi sử dụng tính năng Góp ý."
            "OTP_SERVICE_UNAVAILABLE" ->
                cleanMessage ?: "Hệ thống gửi mã OTP đang bảo trì, vui lòng thử lại sau."
            "OTP_INVALID_OR_EXPIRED" ->
                cleanMessage ?: "Mã OTP không chính xác hoặc đã hết hạn."
            "WORKER_VERIFICATION_PENDING" -> cleanMessage ?: "Bạn đang có một hồ sơ xác thực chờ HR xét duyệt, không thể gửi trùng."
            "REFERRAL_PROGRAM_DISABLED" -> cleanMessage ?: "Chương trình giới thiệu người làm hiện chưa mở hoặc đã hết hiệu lực."
            "REFERRAL_CONSENT_REQUIRED" -> cleanMessage ?: "Vui lòng xác nhận người được giới thiệu đã đồng ý chia sẻ thông tin."
            "GEOFENCE_OUT_OF_RANGE" -> cleanMessage ?: "Vị trí GPS của bạn nằm ngoài bán kính khu vực nhà máy/dự án được phân công."
            "GPS_ACCURACY_TOO_LOW" -> cleanMessage ?: "Tín hiệu GPS quá yếu (sai số lớn). Vui lòng di chuyển ra khu vực thoáng và thử lại."
            "SITE_GEOFENCE_NOT_CONFIGURED" ->
                cleanMessage ?: "Địa điểm dự án (Site) chưa được Quản trị viên HRP xác nhận tọa độ & bán kính chấm công GPS."
            "CHECK_IN_REQUIRED" -> cleanMessage ?: "Cần ghi nhận Vào ca trước khi thực hiện Tan ca."
            "ATTENDANCE_ACTION_ALREADY_RECORDED" -> cleanMessage ?: "Thao tác chấm công này đã được ghi nhận trong ngày hôm nay."
            "IDEMPOTENCY_REQUIRED", "IDEMPOTENCY_CONFLICT", "IDEMPOTENCY_KEY_REQUIRED" ->
                cleanMessage ?: "Yêu cầu đang được xử lý hoặc bị trùng lặp. Vui lòng thử lại."
            "IDEMPOTENCY_PAYLOAD_MISMATCH" ->
                cleanMessage ?: "Thông tin ứng tuyển đã thay đổi so với lần gửi trước, vui lòng gửi lại."
            "DUPLICATE_APPLICATION" ->
                cleanMessage ?: "Bạn đã ứng tuyển vị trí này rồi. Hãy dùng mã tra cứu để xem trạng thái."
            "JOB_NOT_AVAILABLE" ->
                cleanMessage ?: "Vị trí này hiện không còn nhận hồ sơ."
            "CONSENT_REQUIRED" ->
                cleanMessage ?: "Vui lòng đồng ý cho phép xử lý thông tin trước khi gửi."
            "POSSIBLE_MATCH_NOT_RESOLVED" ->
                cleanMessage ?: "Thông tin họ tên hoặc số điện thoại chưa khớp với hồ sơ ứng viên sẵn có trên hệ thống (cần bộ phận Tuyển dụng HRP đối soát)."
            "CV_UPLOAD_DISABLED", "CV_MIME_NOT_ALLOWED", "CV_TOO_LARGE", "CV_METADATA_INCOMPLETE" ->
                cleanMessage ?: "Tính năng tải CV hiện đang tắt. Vui lòng gửi hồ sơ không kèm CV."
            "APPLY_ENDPOINT_RETIRED", "ENDPOINT_RETIRED" ->
                cleanMessage ?: "Không thể gửi đơn trên luồng cũ, vui lòng thử lại."
            "PAYLOAD_TOO_LARGE" ->
                cleanMessage ?: "Dữ liệu gửi lên quá lớn. Vui lòng rút ngắn thông tin."
            "RATE_LIMIT_EXCEEDED", "RATE_LIMITED" ->
                cleanMessage ?: "Bạn thao tác quá nhanh hoặc đã vượt giới hạn gửi. Vui lòng đợi ít phút trước khi thử lại."
            "RATE_LIMIT_UNAVAILABLE" ->
                cleanMessage ?: "Hệ thống đang tạm thời quá tải. Vui lòng thử lại sau ít phút."
            else -> cleanMessage ?: fallback
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
    @param:Json(name = "fullName") val fullName: String = "",
    @param:Json(name = "role") val role: String = "WORKER",
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @param:Json(name = "token") val token: String? = null
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
    @param:Json(name = "ok") val ok: Boolean = false,
    @param:Json(name = "success") val success: Boolean = false,
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
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
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

@JsonClass(generateAdapter = true)
data class WorkerAssignmentSiteDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "latitude") val latitude: Double? = null,
    @param:Json(name = "longitude") val longitude: Double? = null,
    @param:Json(name = "geofenceRadiusMeters") val geofenceRadiusMeters: Int? = null,
    @param:Json(name = "radiusMeters") val radiusMeters: Int? = null,
    @param:Json(name = "geoConfirmedAt") val geoConfirmedAt: String? = null
) {
    val isGeoConfirmed: Boolean
        get() = latitude != null && longitude != null && !geoConfirmedAt.isNullOrBlank()

    fun toDomain(): WorkerAssignmentSite {
        return WorkerAssignmentSite(
            id = id,
            name = name.orEmpty(),
            latitude = latitude,
            longitude = longitude,
            radiusMeters = geofenceRadiusMeters ?: radiusMeters ?: 200
        )
    }
}

@JsonClass(generateAdapter = true)
data class WorkerAssignmentDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "assignmentId") val assignmentId: String? = null,
    @param:Json(name = "projectId") val projectId: String = "",
    @param:Json(name = "projectName") val projectName: String? = null,
    @param:Json(name = "projectCode") val projectCode: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "employeeCode") val employeeCode: String? = null,
    @param:Json(name = "employmentType") val employmentType: String? = null,
    @param:Json(name = "workSetting") val workSetting: String? = null,
    @param:Json(name = "position") val position: String? = null,
    @param:Json(name = "startDate") val startDate: String? = null,
    @param:Json(name = "validFrom") val validFrom: String? = null,
    @param:Json(name = "validTo") val validTo: String? = null,
    @param:Json(name = "sites") val sites: List<WorkerAssignmentSiteDto>? = null
)

@JsonClass(generateAdapter = true)
data class MobileAccessDto(
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @param:Json(name = "hasActiveAssignment") val hasActiveAssignment: Boolean = false,
    @param:Json(name = "hasConfiguredSiteGps") val hasConfiguredSiteGps: Boolean? = null,
    @param:Json(name = "siteGeofenceNotConfigured") val siteGeofenceNotConfigured: Boolean? = null,
    @param:Json(name = "canCheckIn") val canCheckIn: Boolean = false
)

/**
 * Worker Profile (GET /api/workers/me or nested in LoginResponse)
 */
@JsonClass(generateAdapter = true)
data class WorkerProfileResponse(
    @param:Json(name = "id") val id: String? = "",
    @param:Json(name = "employeeCode") val employeeCode: String? = "",
    @param:Json(name = "fullName") val fullName: String? = "",
    @param:Json(name = "cccdNumber") val cccdNumber: String? = "",
    @param:Json(name = "phone") val phone: String? = "",
    @param:Json(name = "factoryLocation") val factoryLocation: String? = "",
    @param:Json(name = "projectCode") val projectCode: String? = "",
    @param:Json(name = "department") val department: String? = "",
    @param:Json(name = "position") val position: String? = "",
    @param:Json(name = "baseSalaryPerDayVnd") val baseSalaryPerDayVnd: Long? = 0L,
    @param:Json(name = "joinedDate") val joinedDate: String? = "",
    @param:Json(name = "bankAccount") val bankAccount: String? = "",
    @param:Json(name = "status") val status: String? = "ACTIVE",
    @param:Json(name = "profileStatus") val profileStatus: String? = null,
    @param:Json(name = "employmentStatus") val employmentStatus: String? = null,
    @param:Json(name = "phoneVerified") val phoneVerified: Boolean? = null,
    @param:Json(name = "verificationStatus") val verificationStatus: String? = null,
    @param:Json(name = "hasActiveAssignment") val hasActiveAssignment: Boolean? = null,
    @param:Json(name = "canCheckIn") val canCheckIn: Boolean? = null,
    @param:Json(name = "assignment") val assignment: WorkerAssignmentDto? = null,
    @param:Json(name = "mobileAccess") val mobileAccess: MobileAccessDto? = null
) {
    fun toDomain(
        overrideAssignment: WorkerAssignmentDto? = null,
        overrideMobileAccess: MobileAccessDto? = null
    ): WorkerProfile {
        val resolvedAssignment = overrideAssignment ?: assignment
        val resolvedMobileAccess = overrideMobileAccess ?: mobileAccess

        val resolvedPhoneVerified = resolvedMobileAccess?.phoneVerified ?: phoneVerified ?: false
        val resolvedHasAssignment = resolvedMobileAccess?.hasActiveAssignment
            ?: hasActiveAssignment
            ?: (resolvedAssignment != null && (resolvedAssignment.status == null || resolvedAssignment.status.equals("ACTIVE", ignoreCase = true)))
        val resolvedCanCheckIn = resolvedMobileAccess?.canCheckIn
            ?: canCheckIn
            ?: (resolvedHasAssignment || VerificationStatus.fromString(verificationStatus) == VerificationStatus.APPROVED)

        val resolvedEmpCode = employeeCode?.takeIf { it.isNotBlank() }
            ?: resolvedAssignment?.employeeCode.orEmpty()
        val resolvedProjCode = projectCode?.takeIf { it.isNotBlank() }
            ?: resolvedAssignment?.projectCode.orEmpty()
        val resolvedProjName = resolvedAssignment?.projectName.orEmpty()
        val rawSites = resolvedAssignment?.sites ?: emptyList()
        val resolvedSites = rawSites.map { it.toDomain() }

        val resolvedLocation = factoryLocation?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                resolvedProjName.takeIf { it.isNotBlank() },
                resolvedSites.firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")

        val resolvedPosition = position?.takeIf { it.isNotBlank() }
            ?: resolvedAssignment?.position.orEmpty()

        // Prefer mobileAccess.hasConfiguredSiteGps when returned by backend, otherwise derive from assignment.sites
        val calculatedSiteGps = if (resolvedAssignment?.sites != null) {
            rawSites.any {
                it.latitude != null && it.longitude != null &&
                    (it.geoConfirmedAt != null || (it.geofenceRadiusMeters ?: it.radiusMeters ?: 0) > 0) &&
                    (it.geoConfirmedAt == null || it.geoConfirmedAt.isNotBlank())
            } && rawSites.any { it.isGeoConfirmed || (it.geoConfirmedAt == null && it.latitude != null && it.longitude != null) }
        } else {
            true
        }
        val hasSiteGps = resolvedMobileAccess?.hasConfiguredSiteGps ?: calculatedSiteGps
        val siteGeofenceMissing = resolvedMobileAccess?.siteGeofenceNotConfigured
            ?: (!hasSiteGps && resolvedHasAssignment && resolvedAssignment?.sites != null)

        val resolvedVerificationStatus = when {
            verificationStatus != null -> VerificationStatus.fromString(verificationStatus)
            resolvedCanCheckIn || resolvedHasAssignment -> VerificationStatus.APPROVED
            else -> VerificationStatus.NONE
        }

        val resolvedJoinedDate = joinedDate?.takeIf { it.isNotBlank() }
            ?: resolvedAssignment?.startDate?.take(10)
            ?: resolvedAssignment?.validFrom.orEmpty()

        return WorkerProfile(
            id = id.orEmpty(),
            employeeCode = resolvedEmpCode,
            fullName = fullName.orEmpty(),
            cccdNumber = cccdNumber.orEmpty(),
            phone = phone.orEmpty(),
            factoryLocation = resolvedLocation,
            projectCode = resolvedProjCode,
            department = department.orEmpty(),
            position = resolvedPosition,
            baseSalaryPerDayVnd = baseSalaryPerDayVnd ?: 0L,
            joinedDate = resolvedJoinedDate,
            bankAccount = bankAccount.orEmpty(),
            status = status ?: employmentStatus ?: profileStatus ?: "ACTIVE",
            phoneVerified = resolvedPhoneVerified,
            verificationStatus = resolvedVerificationStatus,
            hasActiveAssignment = resolvedHasAssignment,
            canCheckIn = resolvedCanCheckIn,
            assignmentId = resolvedAssignment?.assignmentId ?: resolvedAssignment?.id?.takeIf { it.isNotBlank() },
            projectId = resolvedAssignment?.projectId,
            projectName = resolvedProjName,
            sites = resolvedSites,
            hasConfiguredSiteGps = hasSiteGps,
            siteGeofenceNotConfigured = siteGeofenceMissing
        )
    }
}

/**
 * Response DTO for GET /api/workers/me
 * Supports both root JSON { worker, assignment, mobileAccess } and envelope { ok, data, error }
 */
@JsonClass(generateAdapter = true)
data class WorkersMeResponse(
    @param:Json(name = "ok") val ok: Boolean = true,
    @param:Json(name = "worker") val worker: WorkerProfileResponse? = null,
    @param:Json(name = "assignment") val assignment: WorkerAssignmentDto? = null,
    @param:Json(name = "mobileAccess") val mobileAccess: MobileAccessDto? = null,
    @param:Json(name = "data") val data: WorkerProfileResponse? = null,
    @param:Json(name = "error") val error: ApiError? = null,
    @param:Json(name = "message") val message: String? = null
) {
    val isSuccessful: Boolean
        get() = ok && error == null && (worker != null || data != null)

    fun resolvedProfile(): WorkerProfile? {
        val baseWorker = worker ?: data
        return baseWorker?.toDomain(
            overrideAssignment = assignment ?: baseWorker.assignment,
            overrideMobileAccess = mobileAccess ?: baseWorker.mobileAccess
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
data class JobDetailRootResponse(
    @param:Json(name = "job") val job: JobOpeningResponse? = null
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
    @param:Json(name = "requirements") val requirements: String = "",
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "benefitsDetail") val benefitsDetail: String? = null,
    @param:Json(name = "requirementsDetail") val requirementsDetail: String? = null,
    @param:Json(name = "applicationGuide") val applicationGuide: String? = null,
    @param:Json(name = "employerAddress") val employerAddress: String? = null,
    @param:Json(name = "filledQuota") val filledQuota: String? = null
) {
    fun toDomain(): JobOpening {
        val resolvedSalary = salaryDisplay
            ?: salaryRange
            ?: if (salaryMinVnd != null && salaryMaxVnd != null) {
                "${salaryMinVnd / 1_000_000} - ${salaryMaxVnd / 1_000_000} Triệu đ/tháng"
            } else {
                "Thỏa thuận"
            }

        val resolvedJobTypeLabel = when (jobType?.lowercase()?.trim()) {
            "toan_thoi_gian", "full_time", "fulltime" -> "Toàn thời gian"
            "thoi_vu", "seasonal", "temporary" -> "Thời vụ"
            "ban_thoi_gian", "part_time", "parttime" -> "Bán thời gian"
            null, "" -> "Toàn thời gian"
            else -> jobType
        }

        val resolvedStatusLabel = statusLabel?.takeIf { it.isNotBlank() } ?: "Đang tuyển"

        val filteredBenefits = benefits.filterNot {
            val lower = it.trim().lowercase()
            lower.startsWith("ca:") || lower.startsWith("ca làm") || lower.startsWith("ca lam")
        }

        val resolvedBenefits = filteredBenefits.ifEmpty {
            listOfNotNull(
                resolvedJobTypeLabel.takeIf { it.isNotBlank() },
                resolvedStatusLabel
            )
        }

        val formattedDeadline = JobDetailHtmlParser.formatIsoDateToVn(deadline)
        val hasRichDetails = !description.isNullOrBlank() ||
            !benefitsDetail.isNullOrBlank() ||
            !requirementsDetail.isNullOrBlank() ||
            !applicationGuide.isNullOrBlank()

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
            shift = shift ?: shifts.firstOrNull(),
            position = position ?: positionTitles.firstOrNull(),
            deadline = deadline,
            jobTypeLabel = resolvedJobTypeLabel,
            statusLabel = resolvedStatusLabel,
            filledQuotaText = filledQuota.orEmpty(),
            deadlineDisplay = formattedDeadline,
            description = description.orEmpty(),
            benefitsDetail = benefitsDetail.orEmpty(),
            requirementsDetail = requirementsDetail ?: requirements,
            applicationGuide = applicationGuide.orEmpty(),
            employerAddress = employerAddress.orEmpty(),
            isDetailLoaded = hasRichDetails
        )
    }
}

/**
 * Parses rich-text sections and summary metadata from the public job detail page (/viec-lam/{slug}).
 */
object JobDetailHtmlParser {
    private val SECTION_REGEX = Regex(
        """<section[^>]*data-section=["']rich-text["'][^>]*aria-label=["']([^"']+)["'][^>]*>(.*?)</section>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val PROSE_DIV_REGEX = Regex(
        """<div[^>]*class=["'][^"']*prose-like[^"']*["'][^>]*>(.*?)</div>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val DL_PAIR_REGEX = Regex(
        """<dt[^>]*>(.*?)</dt>\s*<dd[^>]*>(.*?)</dd>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val EMPLOYER_SIDEBAR_REGEX = Regex(
        """<aside[^>]*data-section=["']employer-sidebar["'][^>]*>(.*?)</aside>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val EMPLOYER_LOCATION_REGEX = Regex(
        """location_on</span>\s*<span[^>]*>(.*?)</span>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    fun formatIsoDateToVn(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        val trimmed = rawDate.trim()
        // Already dd/MM/yyyy
        if (Regex("""^\d{2}/\d{2}/\d{4}$""").matches(trimmed)) return trimmed
        // ISO yyyy-MM-dd...
        if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            val year = trimmed.substring(0, 4)
            val month = trimmed.substring(5, 7)
            val day = trimmed.substring(8, 10)
            return "$day/$month/$year"
        }
        return trimmed
    }

    fun enrichJobWithHtml(baseJob: JobOpening, html: String): JobOpening {
        if (html.isBlank()) return baseJob

        var description = baseJob.description
        var benefitsDetail = baseJob.benefitsDetail
        var requirementsDetail = baseJob.requirementsDetail
        var applicationGuide = baseJob.applicationGuide

        for (match in SECTION_REGEX.findAll(html)) {
            val label = cleanHtmlToText(match.groupValues[1]).lowercase()
            val sectionContent = match.groupValues[2]
            val innerHtml = PROSE_DIV_REGEX.find(sectionContent)?.groupValues?.get(1)
                ?: sectionContent.substringAfter("</h2>", sectionContent)
            val cleanedText = cleanHtmlToText(innerHtml)
            if (cleanedText.isNotBlank()) {
                when {
                    label.contains("mô tả công việc") -> description = cleanedText
                    label.contains("phúc lợi") -> benefitsDetail = cleanedText
                    label.contains("yêu cầu") -> requirementsDetail = cleanedText
                    label.contains("hướng dẫn ứng tuyển") -> applicationGuide = cleanedText
                }
            }
        }

        var salaryRange = baseJob.salaryRange
        var slotsRemaining = baseJob.slotsRemaining
        var filledQuotaText = baseJob.filledQuotaText
        var deadlineDisplay = baseJob.deadlineDisplay

        for (match in DL_PAIR_REGEX.findAll(html)) {
            val dt = cleanHtmlToText(match.groupValues[1]).lowercase()
            val dd = cleanHtmlToText(match.groupValues[2])
            if (dd.isNotBlank()) {
                when {
                    dt.contains("mức lương") -> salaryRange = dd
                    dt.contains("chỗ trống") -> {
                        val digits = Regex("""\d+""").find(dd)?.value?.toIntOrNull()
                        if (digits != null) slotsRemaining = digits
                    }
                    dt.contains("chỉ tiêu đã tuyển") -> filledQuotaText = dd
                    dt.contains("hạn nhận hồ sơ") -> deadlineDisplay = dd
                }
            }
        }

        var employerAddress = baseJob.employerAddress
        val sidebarMatch = EMPLOYER_SIDEBAR_REGEX.find(html)
        if (sidebarMatch != null) {
            val locMatch = EMPLOYER_LOCATION_REGEX.find(sidebarMatch.groupValues[1])
            if (locMatch != null) {
                val parsedAddr = cleanHtmlToText(locMatch.groupValues[1])
                if (parsedAddr.isNotBlank()) {
                    employerAddress = parsedAddr
                }
            }
        }

        val hasLoadedAnyDetail = description.isNotBlank() ||
            benefitsDetail.isNotBlank() ||
            requirementsDetail.isNotBlank() ||
            applicationGuide.isNotBlank() ||
            filledQuotaText.isNotBlank()

        return baseJob.copy(
            salaryRange = salaryRange,
            slotsRemaining = slotsRemaining,
            filledQuotaText = filledQuotaText,
            deadlineDisplay = deadlineDisplay.ifBlank { formatIsoDateToVn(baseJob.deadline) },
            description = description,
            benefitsDetail = benefitsDetail,
            requirementsDetail = requirementsDetail,
            applicationGuide = applicationGuide,
            employerAddress = employerAddress,
            isDetailLoaded = hasLoadedAnyDetail || baseJob.isDetailLoaded
        )
    }

    fun cleanHtmlToText(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""
        val withLineBreaks = rawHtml
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p\s*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</div\s*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</li\s*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<li[^>]*>""", RegexOption.IGNORE_CASE), "• ")
            .replace(Regex("""/-li\s*""", RegexOption.IGNORE_CASE), "\n• ")

        val withoutTags = withLineBreaks.replace(Regex("""<[^>]+>"""), "")
        val decoded = decodeHtmlEntities(withoutTags)

        return decoded
            .lines()
            .map { it.replace(Regex("""[^\S\r\n]+"""), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private fun decodeHtmlEntities(input: String): String {
        var result = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        result = Regex("""&#(\d+);""").replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null) code.toChar().toString() else match.value
        }
        result = Regex("""&#x([0-9a-fA-F]+);""").replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null) code.toChar().toString() else match.value
        }
        return result
    }
}

/**
 * Endpoint 9: POST /api/public/jobs/{slug}/applications (Root JSON response { trackingCode, status })
 * When authenticated as WORKER, fullName and phone can be omitted because backend reads them from the session account.
 */
@JsonClass(generateAdapter = true)
data class JobApplicationRequest(
    @param:Json(name = "fullName") val fullName: String? = null,
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
data class AttendanceTotalsDto(
    @param:Json(name = "regularHours") val regularHours: Double? = 0.0,
    @param:Json(name = "otHours") val otHours: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class WorkerAttendanceRootResponse(
    @param:Json(name = "items") val items: List<AttendanceDayResponse> = emptyList(),
    @param:Json(name = "total") val total: Int = 0,
    @param:Json(name = "totals") val totals: AttendanceTotalsDto? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceDayResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "workDate") val workDate: String? = null,
    @param:Json(name = "dateString") val dateString: String? = null,
    @param:Json(name = "dayOfWeek") val dayOfWeek: String? = "",
    @param:Json(name = "shift") val shift: String? = null,
    @param:Json(name = "shiftName") val shiftName: String? = "Ca tiêu chuẩn",
    @param:Json(name = "checkInTime") val checkInTime: String? = null,
    @param:Json(name = "checkOutTime") val checkOutTime: String? = null,
    @param:Json(name = "regularHours") val regularHours: Double? = null,
    @param:Json(name = "standardHours") val standardHours: Double? = null,
    @param:Json(name = "otHours") val otHours: Double? = 0.0,
    @param:Json(name = "source") val source: String? = "GPS",
    @param:Json(name = "geofenceResult") val geofenceResult: String? = null,
    @param:Json(name = "riskFlag") val riskFlag: Boolean = false,
    @param:Json(name = "locationVerified") val locationVerified: Boolean? = null,
    @param:Json(name = "factorySite") val factorySite: String? = "Nhà máy HRP",
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "note") val note: String? = ""
) {
    fun toDomain(): AttendanceDay {
        val rawDate = workDate ?: dateString ?: ""
        val resolvedDate = if (rawDate.length >= 10 && rawDate[4] == '-' && rawDate[7] == '-') {
            rawDate.take(10)
        } else {
            rawDate
        }
        val isInsideGeofence = locationVerified ?: (geofenceResult.equals("INSIDE", ignoreCase = true))
        val hasCheckIn = !checkInTime.isNullOrBlank()
        val hasCheckOut = !checkOutTime.isNullOrBlank()

        val calculatedHours = RewardAttendancePolicy.calculateShiftHours(
            workDate = resolvedDate,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime
        )

        val backendHours = regularHours ?: standardHours
        val resolvedStandardHours = when {
            hasCheckIn && !hasCheckOut -> 0.0
            backendHours != null && backendHours > 0.0 -> backendHours
            hasCheckIn && hasCheckOut -> (calculatedHours ?: 0.0).coerceAtLeast(0.0)
            else -> 0.0
        }

        val domainStatus = when {
            status?.uppercase() in listOf("DISPUTED", "REJECTED") -> AttendanceStatus.DISPUTED
            status?.uppercase() == "ABSENT" -> AttendanceStatus.ABSENT
            riskFlag || geofenceResult.equals("OUTSIDE", ignoreCase = true) -> AttendanceStatus.DISPUTED
            hasCheckIn && !hasCheckOut -> AttendanceStatus.PENDING
            hasCheckIn && hasCheckOut && resolvedStandardHours <= 0.0 -> AttendanceStatus.ABSENT
            (hasCheckIn && hasCheckOut && resolvedStandardHours > 0.0 && isInsideGeofence) ||
                (status?.uppercase() in listOf("APPROVED", "APPENDED") && resolvedStandardHours > 0.0 && isInsideGeofence) ->
                AttendanceStatus.APPROVED
            else -> AttendanceStatus.PENDING
        }

        val resolvedNote = when {
            !note.isNullOrBlank() -> note
            hasCheckIn && !hasCheckOut -> "Đang trong ca (chờ bấm Tan ca)"
            hasCheckIn && hasCheckOut && resolvedStandardHours <= 0.0 ->
                "Thời gian Vào ca và Tan ca trùng nhau — chưa phát sinh giờ công"
            else -> ""
        }

        val resolvedShiftName = when (val rawShift = shift?.trim()?.takeIf { it.isNotEmpty() && !it.equals("DEFAULT", ignoreCase = true) }) {
            null -> shiftName?.takeIf { it.isNotBlank() } ?: "Ca tiêu chuẩn"
            "MORNING" -> "Ca sáng"
            "AFTERNOON" -> "Ca chiều"
            "NIGHT" -> "Ca đêm"
            "HC" -> "Ca hành chính (HC)"
            else -> if (rawShift.startsWith("Ca ", ignoreCase = true)) rawShift else "Ca $rawShift"
        }

        return AttendanceDay(
            id = id,
            dateString = resolvedDate,
            dayOfWeek = dayOfWeek?.takeIf { it.isNotBlank() } ?: resolvedDate,
            shiftName = resolvedShiftName,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            standardHours = resolvedStandardHours,
            otHours = if (domainStatus == AttendanceStatus.APPROVED) (otHours ?: 0.0) else 0.0,
            locationVerified = isInsideGeofence,
            factorySite = factorySite?.takeIf { it.isNotBlank() } ?: "Nhà máy HRP",
            status = domainStatus,
            note = resolvedNote,
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
    @param:Json(name = "geofenceResult") val geofenceResult: String? = null,
    @param:Json(name = "shiftCode") val shiftCode: String? = null
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
    @param:Json(name = "benefitAmountVnd") val benefitAmountVndRaw: String? = null,
    @param:Json(name = "submittedAt") val submittedAt: String? = null,
    @param:Json(name = "updatedAt") val updatedAt: String? = null
) {
    val benefitAmountVnd: Long?
        get() = benefitAmountVndRaw?.toLongOrNull()

    constructor(
        id: String,
        maskedReferredName: String = "",
        maskedReferredPhone: String = "",
        status: String = "SUBMITTED",
        benefitStatus: String = "NONE",
        benefitAmountVnd: Long?,
        submittedAt: String? = null,
        updatedAt: String? = null
    ) : this(
        id = id,
        maskedReferredName = maskedReferredName,
        maskedReferredPhone = maskedReferredPhone,
        status = status,
        benefitStatus = benefitStatus,
        benefitAmountVndRaw = benefitAmountVnd?.toString(),
        submittedAt = submittedAt,
        updatedAt = updatedAt
    )

    fun toDomain(): ReferralItem {
        val rawDate = submittedAt ?: ""
        val formattedDate = if (rawDate.length >= 10 && rawDate[4] == '-' && rawDate[7] == '-') {
            rawDate.take(10)
        } else {
            rawDate
        }
        return ReferralItem(
            id = id,
            maskedReferredName = maskedReferredName,
            maskedReferredPhone = maskedReferredPhone,
            status = status,
            benefitStatus = benefitStatus,
            benefitAmountVnd = benefitAmountVnd,
            submittedAt = formattedDate,
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
