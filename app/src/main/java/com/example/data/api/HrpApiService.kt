package com.example.data.api

import com.example.data.api.model.ApiResponse
import com.example.data.api.model.CheckInEventData
import com.example.data.api.model.CheckInEventRequest
import com.example.data.api.model.DeviceRegistrationRequest
import com.example.data.api.model.FeedbackListData
import com.example.data.api.model.FeedbackSubmitData
import com.example.data.api.model.FeedbackSubmitRequest
import com.example.data.api.model.JobApplicationRequest
import com.example.data.api.model.JobApplicationResponse
import com.example.data.api.model.JobDetailRootResponse
import com.example.data.api.model.JobsRootResponse
import com.example.data.api.model.LoginRequest
import com.example.data.api.model.LoginResponse
import com.example.data.api.model.OtpSendData
import com.example.data.api.model.OtpSendRequest
import com.example.data.api.model.OtpVerifyData
import com.example.data.api.model.OtpVerifyRequest
import com.example.data.api.model.PublicProjectsData
import com.example.data.api.model.PushRegisterData
import com.example.data.api.model.ReferralListData
import com.example.data.api.model.ReferralProgramWrapper
import com.example.data.api.model.ReferralSubmitRequest
import com.example.data.api.model.ReferralSubmitWrapper
import com.example.data.api.model.RegisterData
import com.example.data.api.model.RegisterRequest
import com.example.data.api.model.WorkerAttendanceRootResponse
import com.example.data.api.model.WorkerVerificationMeData
import com.example.data.api.model.WorkerVerificationRequest
import com.example.data.api.model.WorkerVerificationSubmitData
import com.example.data.api.model.WorkersMeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API Service for HR Partner (HRP) Backend
 * Strictly aligned with the 17 endpoints in web-android-contract.md (SHA: 28C70D9B01B20BB1051FECFDD206D1D3255B66062FF4EC2C76FF13A4F906DF81)
 */
interface HrpApiService {

    // ==================== 1-4. AUTH & PHONE OTP ====================

    /**
     * 1. POST /api/auth/register (Envelope)
     * Header: X-HRP-Client: android (returns data.token in envelope)
     */
    @POST("auth/register")
    suspend fun register(
        @Body req: RegisterRequest,
        @Header("X-HRP-Client") clientHeader: String = "android"
    ): ApiResponse<RegisterData>

    /**
     * 2. POST /api/auth/login (Root JSON - Web additive compatibility)
     */
    @POST("auth/login")
    suspend fun login(
        @Body req: LoginRequest
    ): LoginResponse

    /**
     * 3. POST /api/auth/phone-otp/send (Envelope)
     */
    @POST("auth/phone-otp/send")
    suspend fun sendPhoneOtp(
        @Body req: OtpSendRequest
    ): ApiResponse<OtpSendData>

    /**
     * 4. POST /api/auth/phone-otp/verify (Envelope)
     */
    @POST("auth/phone-otp/verify")
    suspend fun verifyPhoneOtp(
        @Body req: OtpVerifyRequest
    ): ApiResponse<OtpVerifyData>

    // ==================== 5-7. PROJECTS, VERIFICATION & PROFILE ====================

    /**
     * 5. GET /api/public/projects (Envelope)
     */
    @GET("public/projects")
    suspend fun getPublicProjects(
        @Query("status") status: String = "ACTIVE"
    ): ApiResponse<PublicProjectsData>

    /**
     * 6. POST /api/worker-verifications (Envelope)
     */
    @POST("worker-verifications")
    suspend fun submitWorkerVerification(
        @Body req: WorkerVerificationRequest
    ): ApiResponse<WorkerVerificationSubmitData>

    /**
     * 7. GET /api/worker-verifications/me (Envelope)
     */
    @GET("worker-verifications/me")
    suspend fun getMyWorkerVerification(): ApiResponse<WorkerVerificationMeData>

    /**
     * GET /api/workers/me (Root JSON { worker, assignment, mobileAccess } or Envelope)
     */
    @GET("workers/me")
    suspend fun getProfile(): WorkersMeResponse

    // ==================== 8-9. PUBLIC JOBS & APPLICATIONS ====================

    /**
     * 8. GET /api/jobs (Root JSON { jobs, nextOffset, total, facets, overview })
     */
    @GET("jobs")
    suspend fun getPublicJobs(
        @Query("q") q: String? = null,
        @Query("area") area: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("shift") shift: String? = null,
        @Query("shiftType") shiftType: String? = null,
        @Query("jobType") jobType: String? = null,
        @Query("urgency") urgency: String? = null
    ): JobsRootResponse

    /**
     * 8b. GET /api/jobs/{slug} (Root JSON { job })
     */
    @GET("jobs/{slug}")
    suspend fun getPublicJobDetail(
        @Path("slug") slug: String
    ): JobDetailRootResponse

    /**
     * 9. POST /api/public/jobs/{slug}/applications (Root JSON { trackingCode, status })
     */
    @POST("public/jobs/{slug}/applications")
    suspend fun applyPublicJob(
        @Path("slug") slug: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body req: JobApplicationRequest
    ): JobApplicationResponse

    // ==================== 10-11. WORKER ATTENDANCE & GPS CHECK-IN ====================

    /**
     * 10. GET /api/worker/attendance (Root JSON { items, total })
     */
    @GET("worker/attendance")
    suspend fun getWorkerAttendance(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("limit") limit: Int = 31
    ): WorkerAttendanceRootResponse

    /**
     * 11. POST /api/worker/checkins/events (Envelope)
     */
    @POST("worker/checkins/events")
    suspend fun submitCheckInEvent(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body req: CheckInEventRequest
    ): ApiResponse<CheckInEventData>

    // ==================== 12-13. WORKER FEEDBACK ====================

    /**
     * 12. POST /api/feedback (Envelope)
     */
    @POST("feedback")
    suspend fun submitFeedback(
        @Body req: FeedbackSubmitRequest
    ): ApiResponse<FeedbackSubmitData>

    /**
     * 13. GET /api/feedback/mine (Envelope)
     */
    @GET("feedback/mine")
    suspend fun getMyFeedback(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<FeedbackListData>

    // ==================== 14-16. DYNAMIC REFERRAL PROGRAM ====================

    /**
     * 14. GET /api/referrals/program (Envelope)
     */
    @GET("referrals/program")
    suspend fun getReferralProgram(): ApiResponse<ReferralProgramWrapper>

    /**
     * 15. POST /api/referrals (Envelope)
     */
    @POST("referrals")
    suspend fun submitReferral(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body req: ReferralSubmitRequest
    ): ApiResponse<ReferralSubmitWrapper>

    /**
     * 16. GET /api/referrals/mine (Envelope)
     */
    @GET("referrals/mine")
    suspend fun getMyReferrals(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<ReferralListData>

    // ==================== 17. PUSH NOTIFICATIONS ====================

    /**
     * 17. POST /api/push/register-device (Envelope)
     */
    @POST("push/register-device")
    suspend fun registerDevice(
        @Body req: DeviceRegistrationRequest
    ): ApiResponse<PushRegisterData>
}
