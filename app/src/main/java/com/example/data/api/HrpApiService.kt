package com.example.data.api

import com.example.data.api.model.ApiResponse
import com.example.data.api.model.AttendanceDayResponse
import com.example.data.api.model.BodOverviewResponse
import com.example.data.api.model.CandidateRegistrationRequest
import com.example.data.api.model.CheckInRequest
import com.example.data.api.model.CheckInResponse
import com.example.data.api.model.CheckOutRequest
import com.example.data.api.model.CreateTicketRequest
import com.example.data.api.model.CtvCandidateResponse
import com.example.data.api.model.DeviceRegistrationRequest
import com.example.data.api.model.JobOpeningResponse
import com.example.data.api.model.LoginRequest
import com.example.data.api.model.LoginResponse
import com.example.data.api.model.PayslipSummaryResponse
import com.example.data.api.model.TicketItemResponse
import com.example.data.api.model.UpdateTicketStatusRequest
import com.example.data.api.model.WorkerProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API Service for HR Partner (HRP) Backend
 */
interface HrpApiService {

    // ==================== AUTH ====================
    @POST("auth/login")
    suspend fun login(
        @Body req: LoginRequest
    ): LoginResponse

    // ==================== WORKER PROFILE ====================
    @GET("worker/profile")
    suspend fun getProfile(): WorkerProfileResponse

    // ==================== ATTENDANCE ====================
    @POST("attendance/checkin")
    suspend fun checkIn(
        @Body req: CheckInRequest
    ): CheckInResponse

    @POST("attendance/checkout")
    suspend fun checkOut(
        @Body req: CheckOutRequest = CheckOutRequest()
    ): CheckInResponse

    @GET("attendance")
    suspend fun getAttendanceList(): List<AttendanceDayResponse>

    @POST("attendance/approve-all")
    suspend fun approveAllAttendance(): ApiResponse<String>

    // ==================== PAYSLIP ====================
    @GET("payroll/summary")
    suspend fun getPayslip(): PayslipSummaryResponse

    // ==================== TICKETS ====================
    @GET("tickets")
    suspend fun getTickets(): List<TicketItemResponse>

    @POST("tickets")
    suspend fun createTicket(
        @Body req: CreateTicketRequest
    ): TicketItemResponse

    @PUT("tickets/{id}/status")
    suspend fun updateTicketStatus(
        @Path("id") id: String,
        @Body req: UpdateTicketStatusRequest
    ): TicketItemResponse

    // ==================== JOBS & CTV ====================
    @GET("jobs")
    suspend fun getJobListings(): List<JobOpeningResponse>

    @GET("candidates")
    suspend fun getCandidates(): List<CtvCandidateResponse>

    @POST("candidates/register")
    suspend fun registerCandidate(
        @Body req: CandidateRegistrationRequest
    ): CtvCandidateResponse

    // ==================== BOD OVERVIEW ====================
    @GET("bod/overview")
    suspend fun getBodOverview(): BodOverviewResponse

    // ==================== PUSH NOTIFICATIONS ====================
    @POST("push/register-device")
    suspend fun registerDevice(
        @Body req: DeviceRegistrationRequest
    ): ApiResponse<String>
}
