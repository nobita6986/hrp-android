package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.JobOpening
import com.example.data.model.TicketType
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSuccessContainer
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpTextPrimary
import com.example.ui.theme.HrpTextSecondary

@Composable
fun CreateTicketModal(
    initialType: TicketType = TicketType.ADVANCE_SALARY,
    onSubmit: (type: TicketType, title: String, description: String, amount: Long?, dates: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var title by remember {
        mutableStateOf(
            when (initialType) {
                TicketType.ADVANCE_SALARY -> "Xin tạm ứng sinh hoạt phí giữa tháng"
                TicketType.TIMESHEET_DISPUTE -> "Khiếu nại ghi nhận giờ quẹt thẻ"
                TicketType.LEAVE_REQUEST -> "Đơn xin nghỉ phép năm"
                TicketType.OTHER -> "Ý kiến đóng góp cải tiến"
            }
        )
    }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(if (initialType == TicketType.ADVANCE_SALARY) "2000000" else "") }
    var requestedDays by remember { mutableStateOf(if (initialType == TicketType.LEAVE_REQUEST) "25/10/2026 (1 ngày)" else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Gửi Đơn Yêu Cầu Hỗ Trợ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Chuyển trực tiếp tới HR & Ban điều phối",
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TicketType.values().take(3).forEach { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                if (type == TicketType.ADVANCE_SALARY) {
                                    title = "Xin tạm ứng sinh hoạt phí giữa tháng"
                                    amountText = "2000000"
                                } else if (type == TicketType.LEAVE_REQUEST) {
                                    title = "Đơn xin nghỉ phép"
                                    requestedDays = "25/10/2026 (1 ngày)"
                                } else if (type == TicketType.TIMESHEET_DISPUTE) {
                                    title = "Phản ánh sai giờ quẹt ca làm"
                                }
                            },
                            label = { Text(type.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HrpPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề đơn") },
                    modifier = Modifier.fillMaxWidth().testTag("ticket_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                if (selectedType == TicketType.ADVANCE_SALARY) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Số tiền xin tạm ứng (VND)") },
                        modifier = Modifier.fillMaxWidth().testTag("ticket_amount_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                    )
                }

                if (selectedType == TicketType.LEAVE_REQUEST) {
                    OutlinedTextField(
                        value = requestedDays,
                        onValueChange = { requestedDays = it },
                        label = { Text("Ngày xin nghỉ (Từ ngày - Đến ngày)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Lý do / Nội dung chi tiết") },
                    placeholder = { Text("Nhập lý do chi tiết để HR xử lý nhanh chóng...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("ticket_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountLong = amountText.toLongOrNull()
                    onSubmit(
                        selectedType,
                        title.ifBlank { selectedType.label },
                        description.ifBlank { "Không có ghi chú thêm." },
                        amountLong,
                        if (selectedType == TicketType.LEAVE_REQUEST) requestedDays else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                modifier = Modifier.testTag("submit_ticket_confirm_button")
            ) {
                Text("Gửi Đơn Ngay", fontWeight = FontWeight.Bold)
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
    onSubmit: (candidateName: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var candidateName by remember { mutableStateOf("Nguyễn Văn Hùng") }
    var phone by remember { mutableStateOf("0987 654 321") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Ứng Tuyển / Giới Thiệu",
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
                        Text("Mức lương: ${job.salaryRange}", color = HrpPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = candidateName,
                    onValueChange = { candidateName = it },
                    label = { Text("Họ và tên ứng viên") },
                    modifier = Modifier.fillMaxWidth().testTag("apply_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại liên hệ") },
                    modifier = Modifier.fillMaxWidth().testTag("apply_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HrpPrimary)
                )

                Text(
                    text = "Cam kết: Không thu bất kỳ khoản phí nào của người lao động. Đưa đón và bố trí ký túc xá miễn phí.",
                    color = HrpTextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (candidateName.isNotBlank() && phone.isNotBlank()) {
                        onSubmit(candidateName, phone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                modifier = Modifier.testTag("apply_job_confirm_button")
            ) {
                Text("Xác Nhận Nộp", fontWeight = FontWeight.Bold)
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
                    text = "Chấm Công Thành Công!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val siteText = gpsInfo?.matchedSite ?: "Cổng B - Samsung SEVT Thái Nguyên"
                val coordsText = if (gpsInfo != null) {
                    "\nTọa độ GPS: %.5f, %.5f (Sai số: ±%.1fm)".format(
                        gpsInfo.latitude,
                        gpsInfo.longitude,
                        gpsInfo.accuracyMeters
                    )
                } else {
                    ""
                }

                Text(
                    text = "Hệ thống HRP V4 đã ghi nhận thời gian vào ca qua định vị phần cứng GPS tại $siteText.$coordsText",
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
