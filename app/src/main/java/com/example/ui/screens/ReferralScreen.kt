package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.data.model.ReferralItem
import com.example.data.model.ReferralProgram
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.data.repository.HrpFormatters
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpInfo
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning

@Composable
fun ReferralScreen(
    program: ReferralProgram? = null,
    referralList: List<ReferralItem> = emptyList(),
    workerProfile: WorkerProfile? = null,
    isSubmitting: Boolean = false,
    onSubmitReferral: (referredName: String, referredPhone: String, jobInterest: String?, consentToContact: Boolean) -> Unit = { _, _, _, _ -> },
    onOpenWorkerVerification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isProgramEnabled = program?.isEnabled == true
    val canSubmit = isProgramEnabled && (program?.canSubmit == true)
    val isApprovedWorker = workerProfile?.verificationStatus == VerificationStatus.APPROVED

    var referredName by remember { mutableStateOf("") }
    var referredPhone by remember { mutableStateOf("") }
    var jobInterest by remember { mutableStateOf("") }
    var consentChecked by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp)
            .testTag("referral_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header Banner driven dynamically by GET /api/referrals/program
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHƯƠNG TRÌNH ĐỒNG ĐỘI HRP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HrpPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = program?.title?.takeIf { it.isNotBlank() } ?: "Giới Thiệu Người Làm",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isProgramEnabled) HrpSuccess.copy(alpha = 0.2f) else HrpWarning.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isProgramEnabled) "ĐANG MỞ" else "CHƯA MỞ",
                                color = if (isProgramEnabled) HrpSuccess else HrpWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isProgramEnabled) {
                            program?.rewardPolicyDescription?.takeIf { it.isNotBlank() }
                                ?: "Chương trình giới thiệu người làm đang mở theo chính sách phê duyệt của Quản trị viên HRP."
                        } else {
                            program?.blockedReason?.takeIf { it.isNotBlank() }
                                ?: "Chính sách giới thiệu ứng viên đang trong quá trình hoàn thiện và phê duyệt bởi Quản trị viên HRP. Hệ thống sẽ kích hoạt khi chính sách chính thức được công bố."
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    if (!program?.policyVersion.isNullOrBlank() || !program?.effectiveFrom.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val versionTag = program?.policyVersion?.let { "Phiên bản: $it" } ?: ""
                        val dateTag = program?.effectiveFrom?.let { "Hiệu lực từ: $it" } ?: ""
                        Text(
                            text = listOf(versionTag, dateTag).filter { it.isNotBlank() }.joinToString(" • "),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Dynamic Policy & Rules Card (never hardcodes commission amounts)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HrpSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Chính sách & Điều kiện công bố từ HRP:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HrpNavyDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.Policy,
                        title = "Mức & cách tính quyền lợi",
                        description = program?.rewardPolicyDescription?.takeIf { it.isNotBlank() }
                            ?: "Mức thưởng và điều kiện chi trả được áp dụng theo chính sách chính thức do Owner/Admin HRP công bố trên hệ thống. Ứng dụng không tự ý đặt con số."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Sự kiện được tính là thành công",
                        description = program?.successEventDescription?.takeIf { it.isNotBlank() }
                            ?: "Người được giới thiệu đồng ý tiếp nhận liên hệ, hoàn tất tuyển dụng vào hồ sơ canonical và đáp ứng đủ điều kiện làm việc theo chính sách hiện hành."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.People,
                        title = "Điều kiện người giới thiệu & sự đồng thuận",
                        description = program?.eligibilityCriteria?.takeIf { it.isNotBlank() }
                            ?: "Chỉ dành cho Công nhân HRP đã xác thực và đang có phân công làm việc. Người giới thiệu phải xác nhận ứng viên đã đồng ý để HRP liên hệ (App không tự ứng tuyển thay)."
                    )

                    if (!program?.termsAndConditions.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Điều khoản chi tiết: ${program?.termsAndConditions}",
                            fontSize = 11.sp,
                            color = HrpTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Submission Form or Gate Card
        item {
            if (!isProgramEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(HrpPrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = HrpPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chưa mở tiếp nhận giới thiệu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HrpNavyDark
                            )
                            Text(
                                text = program?.blockedReason?.takeIf { it.isNotBlank() }
                                    ?: "Biểu mẫu gửi lời giới thiệu sẽ tự động hiển thị ngay khi Quản trị viên HRP kích hoạt chính sách.",
                                color = HrpTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            } else if (!canSubmit) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpWarning.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = HrpWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tài khoản chưa đủ điều kiện gửi giới thiệu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HrpNavyDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = program?.blockedReason?.takeIf { it.isNotBlank() }
                                ?: "Chức năng gửi lời giới thiệu chỉ áp dụng cho Công nhân HRP đã được xác thực (APPROVED) và đang có phân công làm việc hoạt động.",
                            color = HrpTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        if (!isApprovedWorker) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onOpenWorkerVerification,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Thực hiện xác thực công nhân HRP", fontWeight = FontWeight.Bold, color = HrpPrimary)
                            }
                        }
                    }
                }
            } else {
                // Active Referral Submission Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = HrpPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gửi Thông Tin Người Được Giới Thiệu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = HrpNavyDark
                            )
                        }

                        OutlinedTextField(
                            value = referredName,
                            onValueChange = { referredName = it },
                            label = { Text("Họ và tên người được giới thiệu *") },
                            placeholder = { Text("Nhập họ tên") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("referral_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                        )

                        OutlinedTextField(
                            value = referredPhone,
                            onValueChange = { referredPhone = it },
                            label = { Text("Số điện thoại liên hệ *") },
                            placeholder = { Text("0987xxxxxx") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("referral_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                        )

                        OutlinedTextField(
                            value = jobInterest,
                            onValueChange = { jobInterest = it },
                            label = { Text("Công việc / Khu vực mong muốn (Tùy chọn)") },
                            placeholder = { Text("VD: Công nhân lắp ráp điện tử") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("referral_job_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HrpSurfaceSubtle)
                                .clickable { consentChecked = !consentChecked }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = consentChecked,
                                onCheckedChange = { consentChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = HrpPrimary),
                                modifier = Modifier.testTag("referral_consent_checkbox")
                            )
                            Text(
                                text = "Tôi xác nhận người được giới thiệu đã đồng ý để HRP liên hệ tư vấn việc làm. (Ứng dụng không tự động tạo đơn ứng tuyển thay họ).",
                                fontSize = 11.sp,
                                color = HrpNavyDark,
                                lineHeight = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                onSubmitReferral(
                                    referredName,
                                    referredPhone,
                                    jobInterest.takeIf { it.isNotBlank() },
                                    consentChecked
                                )
                                referredName = ""
                                referredPhone = ""
                                jobInterest = ""
                                consentChecked = false
                            },
                            enabled = !isSubmitting &&
                                referredName.isNotBlank() &&
                                referredPhone.isNotBlank() &&
                                consentChecked,
                            colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_referral_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Gửi Lời Giới Thiệu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Minimal Privacy-Safe Referral History (GET /api/referrals/mine)
        if (referralList.isNotEmpty()) {
            item {
                HrpSectionHeader(
                    title = "Danh Sách Đã Giới Thiệu (${referralList.size})",
                    subtitle = "Trạng thái tối thiểu đã được bảo mật thông tin cá nhân"
                )
            }

            items(referralList, key = { it.id }) { ref ->
                ReferralHistoryCard(item = ref)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ReferralHistoryCard(
    item: ReferralItem
) {
    val (statusLabel, statusColor) = when (item.status.uppercase()) {
        "PAID" -> Pair("Đã chi trả thưởng", HrpSuccess)
        "REWARD_APPROVED" -> Pair("Đã duyệt thưởng", HrpSuccess)
        "QUALIFIED" -> Pair("Đạt điều kiện", HrpInfo)
        "IN_REVIEW" -> Pair("Đang liên hệ / đối soát", HrpWarning)
        "REJECTED" -> Pair("Không đạt điều kiện", HrpError)
        else -> Pair("Đã tiếp nhận", HrpPrimary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.maskedReferredName.ifBlank { "Ứng viên giới thiệu" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = HrpNavyDark
                    )
                    Text(
                        text = "SĐT: ${item.maskedReferredPhone} • Ngày gửi: ${item.submittedAt}",
                        fontSize = 11.sp,
                        color = HrpTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (item.benefitAmountVnd != null && item.benefitAmountVnd > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = HrpSurfaceBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quyền lợi ghi nhận:", fontSize = 11.sp, color = HrpTextSecondary)
                    Text(
                        text = HrpFormatters.formatVnd(item.benefitAmountVnd),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HrpSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralRuleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HrpSurfaceSubtle)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HrpPrimary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = HrpNavyDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = HrpTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
