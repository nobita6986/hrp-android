package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedbackCategory
import com.example.data.model.JobOpening
import com.example.data.model.PublicProject
import com.example.data.model.TicketType
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerVerificationInfo
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSuccessContainer
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning

@Composable
fun CreateTicketModal(
    initialType: TicketType = TicketType.OTHER,
    phoneVerified: Boolean = false,
    onSubmit: (type: TicketType, title: String, description: String, amount: Long?, dates: String?) -> Unit,
    onSubmitFeedback: ((category: FeedbackCategory, content: String) -> Unit)? = null,
    onOpenPhoneOtp: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(
            when (initialType) {
                TicketType.ADVANCE_SALARY, TicketType.TIMESHEET_DISPUTE -> FeedbackCategory.SALARY_BENEFITS
                TicketType.LEAVE_REQUEST -> FeedbackCategory.WORK_ENVIRONMENT
                TicketType.OTHER -> FeedbackCategory.SUGGESTION
            }
        )
    }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Gửi Góp Ý & Phản Ánh",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Chuyển trực tiếp tới Ban Nhân sự & Quản trị HRP",
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!phoneVerified) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HrpWarning.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HrpWarning.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_feedback_otp_warning")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = HrpWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tài khoản chưa xác thực số điện thoại (phoneVerified = false). Cần xác thực OTP trước khi gửi góp ý — ứng dụng không giả lập gửi thành công.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = HrpNavyDark,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            if (onOpenPhoneOtp != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenPhoneOtp()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("modal_feedback_open_otp_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Xác minh OTP ngay",
                                        fontWeight = FontWeight.Bold,
                                        color = HrpPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Chọn nhóm nội dung:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = HrpNavyDark)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedbackCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HrpPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nội dung chi tiết (tối thiểu 10 ký tự)") },
                    placeholder = { Text("Mô tả rõ sự việc, ca làm hoặc đề xuất để HR phản hồi nhanh chóng...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("ticket_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phoneVerified && description.isNotBlank()) {
                        if (onSubmitFeedback != null) {
                            onSubmitFeedback(selectedCategory, description)
                        } else {
                            onSubmit(
                                initialType,
                                selectedCategory.label,
                                description,
                                null,
                                null
                            )
                        }
                    }
                },
                enabled = phoneVerified && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                modifier = Modifier.testTag("submit_ticket_confirm_button")
            ) {
                Text("Gửi Góp Ý", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = HrpTextSecondary)
            }
        }
    )
}

@Composable
fun ApplyJobModal(
    job: JobOpening,
    defaultName: String = "",
    defaultPhone: String = "",
    isAuthenticated: Boolean = true,
    phoneVerified: Boolean = false,
    onSubmit: (candidateName: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var candidateName by remember(defaultName) { mutableStateOf(defaultName) }
    var phone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var consentChecked by remember { mutableStateOf(true) }

    val canSubmit = if (isAuthenticated) {
        consentChecked
    } else {
        candidateName.isNotBlank() && phone.isNotBlank() && consentChecked
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Ứng Tuyển Việc Làm",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpPrimary, fontWeight = FontWeight.Medium)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpPrimarySoft)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(job.factoryCompany, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(job.industrialZone, color = HrpTextSecondary, fontSize = 11.sp)
                        Text("Thu nhập: ${job.salaryRange}", color = HrpPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (isAuthenticated) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HrpSurfaceSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_authenticated_account_info")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Thông tin tài khoản ứng tuyển:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HrpNavyDark
                                )
                            )
                            Text(
                                text = "Họ và tên: ${defaultName.ifBlank { "Theo tài khoản đăng nhập" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HrpNavyDark,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Số điện thoại: ${defaultPhone.ifBlank { "Theo tài khoản đăng nhập" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HrpNavyDark,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = if (phoneVerified) {
                                    "Trạng thái SĐT: Đã xác minh OTP"
                                } else {
                                    "Trạng thái SĐT: Chưa xác minh OTP (phoneVerified=false)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (phoneVerified) HrpSuccess else HrpWarning,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hệ thống tự động lấy Họ tên và Số điện thoại từ tài khoản đang đăng nhập — bạn không cần nhập lại.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HrpTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = candidateName,
                        onValueChange = { candidateName = it },
                        label = { Text("Họ và tên ứng viên") },
                        placeholder = { Text("Nhập họ và tên của bạn") },
                        modifier = Modifier.fillMaxWidth().testTag("apply_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại liên hệ") },
                        placeholder = { Text("0987xxxxxx") },
                        modifier = Modifier.fillMaxWidth().testTag("apply_phone_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { consentChecked = !consentChecked },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consentChecked,
                        onCheckedChange = { consentChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = HrpPrimary)
                    )
                    Text(
                        text = "Tôi đồng ý cung cấp thông tin cá nhân để bộ phận tuyển dụng HRP liên hệ tư vấn công việc.",
                        color = HrpTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        onSubmit(candidateName, phone)
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                modifier = Modifier.testTag("apply_job_confirm_button")
            ) {
                Text("Xác Nhận Ứng Tuyển", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = HrpTextSecondary)
            }
        }
    )
}

@Composable
fun PhoneOtpVerificationModal(
    phone: String,
    cooldownSeconds: Int,
    isSendingOtp: Boolean,
    isVerifyingOtp: Boolean,
    errorMessage: String?,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var phoneInput by remember(phone) { mutableStateOf(phone) }
    var otpCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Xác Thực Số Điện Thoại (OTP)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Bảo mật tài khoản Người lao động HRP",
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Số điện thoại nhận mã (theo tài khoản)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("otp_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                OutlinedButton(
                    onClick = { onSendOtp(phoneInput.trim()) },
                    enabled = !isSendingOtp && cooldownSeconds <= 0 && phoneInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("send_otp_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSendingOtp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang yêu cầu mã OTP...")
                    } else if (cooldownSeconds > 0) {
                        Text("Gửi lại mã sau ${cooldownSeconds}s")
                    } else {
                        Text("Nhận Mã OTP", fontWeight = FontWeight.Bold, color = HrpPrimary)
                    }
                }

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= 6) {
                            otpCode = digitsOnly
                        }
                    },
                    label = { Text("Mã OTP (6 chữ số)") },
                    placeholder = { Text("Nhập 6 chữ số OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("otp_code_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HrpError.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HrpError.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = HrpError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HrpError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifyOtp(otpCode) },
                enabled = !isVerifyingOtp && otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                modifier = Modifier.testTag("verify_otp_confirm_button")
            ) {
                if (isVerifyingOtp) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Xác Nhận Mã OTP", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = HrpTextSecondary)
            }
        }
    )
}

@Composable
fun WorkerVerificationModal(
    defaultFullName: String,
    defaultEmployeeCode: String,
    publicProjects: List<PublicProject>,
    existingVerification: WorkerVerificationInfo?,
    isSubmitting: Boolean,
    onSubmit: (fullName: String, cccdNumber: String, dob: String, projectId: String, employeeCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf(defaultFullName) }
    var cccdNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var selectedProjectId by remember(publicProjects, existingVerification) {
        mutableStateOf(
            existingVerification?.projectId
                ?: publicProjects.firstOrNull()?.id
                ?: ""
        )
    }
    var employeeCode by remember {
        mutableStateOf(existingVerification?.employeeCode ?: defaultEmployeeCode)
    }

    val isPendingReview = existingVerification?.status == VerificationStatus.PENDING_REVIEW

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Xác Thực Công Nhân HRP",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Liên kết hồ sơ công nhân để mở khóa Chấm công GPS & Giới thiệu",
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                existingVerification?.let { ver ->
                    val statusColor = when (ver.status) {
                        VerificationStatus.APPROVED -> HrpSuccess
                        VerificationStatus.PENDING_REVIEW -> HrpWarning
                        VerificationStatus.NEEDS_INFO, VerificationStatus.REJECTED -> HrpError
                        VerificationStatus.NONE -> HrpTextSecondary
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Trạng thái hiện tại: ${ver.status.label}",
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 12.sp
                            )
                            if (!ver.reviewNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ghi chú từ HR: ${ver.reviewNote}",
                                    color = HrpNavyDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên (theo CCCD)") },
                    enabled = !isPendingReview,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("verify_fullname_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                OutlinedTextField(
                    value = cccdNumber,
                    onValueChange = { cccdNumber = it },
                    label = { Text("Số CCCD (12 chữ số)") },
                    placeholder = { Text("00109xxxxxxx") },
                    enabled = !isPendingReview,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("verify_cccd_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Ngày sinh (YYYY-MM-DD)") },
                    placeholder = { Text("1998-05-20") },
                    enabled = !isPendingReview,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("verify_dob_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                Text(
                    text = "Chọn Dự Án / Nhà Máy đang làm việc:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = HrpNavyDark)
                )

                if (publicProjects.isEmpty()) {
                    Text(
                        text = "Chưa tải được danh sách dự án đang hoạt động từ máy chủ.",
                        style = MaterialTheme.typography.bodySmall.copy(color = HrpError, fontSize = 11.sp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        publicProjects.forEach { proj ->
                            val selected = selectedProjectId == proj.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) HrpPrimarySoft else HrpSurfaceSubtle,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) HrpPrimary else HrpSurfaceBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isPendingReview) { selectedProjectId = proj.id }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = proj.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = HrpNavyDark
                                    )
                                    if (proj.location.isNotBlank()) {
                                        Text(
                                            text = proj.location,
                                            fontSize = 11.sp,
                                            color = HrpTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = employeeCode,
                    onValueChange = { employeeCode = it },
                    label = { Text("Mã nhân viên tại dự án (Employee Code)") },
                    placeholder = { Text("VD: HRP-01234") },
                    enabled = !isPendingReview,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("verify_empcode_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )
            }
        },
        confirmButton = {
            if (!isPendingReview) {
                Button(
                    onClick = {
                        onSubmit(fullName, cccdNumber, dob, selectedProjectId, employeeCode)
                    },
                    enabled = !isSubmitting &&
                        fullName.isNotBlank() &&
                        cccdNumber.isNotBlank() &&
                        dob.isNotBlank() &&
                        selectedProjectId.isNotBlank() &&
                        employeeCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                    modifier = Modifier.testTag("submit_worker_verification_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Gửi Xác Thực", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = HrpTextSecondary)
            }
        }
    )
}

@Composable
fun CheckInSuccessModal(
    gpsInfo: com.example.utils.GpsLocationResult? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(HrpSuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HrpSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Đã Ghi Nhận Sự Kiện Chấm Công!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val coordsText = if (gpsInfo != null) {
                    "Tọa độ thiết bị: %.5f, %.5f (Sai số: ±%.1fm)".format(
                        gpsInfo.latitude,
                        gpsInfo.longitude,
                        gpsInfo.accuracyMeters
                    )
                } else {
                    "Đã gửi tọa độ GPS thực tế lên máy chủ HRP."
                }

                Text(
                    text = "Hệ thống HRP đã tiếp nhận yêu cầu chấm công qua định vị phần cứng GPS.\n$coordsText",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HrpTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
            ) {
                Text("Xác Nhận", fontWeight = FontWeight.Bold)
            }
        }
    )
}
