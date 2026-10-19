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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.TicketItem
import com.example.data.model.TicketType
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.data.model.WorkerVerificationInfo
import com.example.ui.components.HrpSectionHeader
import com.example.ui.components.TicketStatusBadge
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpInfo
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpNavySurface
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning
import com.example.ui.viewmodel.NavigationTab

@Composable
fun HomeScreen(
    workerProfile: WorkerProfile,
    verificationInfo: WorkerVerificationInfo? = null,
    attendanceList: List<AttendanceDay>,
    tickets: List<TicketItem>,
    onNavigateTab: (NavigationTab) -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onRequestAdvanceSalary: () -> Unit,
    onCreateTicket: (TicketType) -> Unit,
    onOpenPhoneOtp: () -> Unit = {},
    onOpenWorkerVerification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val approvedDays = attendanceList.count { it.status == AttendanceStatus.APPROVED }.toDouble()
    val otHours = attendanceList.sumOf { it.otHours }
    val resolvedVerificationStatus = verificationInfo?.status ?: workerProfile.verificationStatus

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Worker hero card with Check-In / Check-Out
        item {
            WorkerHeroCard(
                profile = workerProfile,
                verificationStatus = resolvedVerificationStatus,
                onCheckIn = onCheckIn,
                onCheckOut = onCheckOut,
                onOpenWorkerVerification = onOpenWorkerVerification
            )
        }

        // Phone OTP Verification Banner (if phone not yet verified)
        if (!workerProfile.phoneVerified) {
            item {
                PhoneVerificationCard(
                    phone = workerProfile.phone,
                    onVerifyClick = onOpenPhoneOtp
                )
            }
        }

        // Worker Verification Status Banner (if not yet APPROVED)
        if (resolvedVerificationStatus != VerificationStatus.APPROVED) {
            item {
                WorkerVerificationStatusCard(
                    status = resolvedVerificationStatus,
                    reviewNote = verificationInfo?.reviewNote,
                    onActionClick = onOpenWorkerVerification
                )
            }
        }

        // Quick attendance statistics
        item {
            WorkerQuickStats(
                actualWorkDays = approvedDays,
                otHours = otHours
            )
        }

        // Quick shortcuts
        item {
            HrpSectionHeader(
                title = "Lối Tắt Hỗ Trợ Công Nhân",
                subtitle = "Gửi yêu cầu hoặc góp ý trực tiếp đến bộ phận hỗ trợ"
            )
        }

        item {
            WorkerQuickActions(
                onAdvanceSalary = onRequestAdvanceSalary,
                onLeaveRequest = { onCreateTicket(TicketType.LEAVE_REQUEST) },
                onDispute = { onCreateTicket(TicketType.TIMESHEET_DISPUTE) }
            )
        }

        // Recent requests / feedback
        item {
            HrpSectionHeader(
                title = "Yêu Cầu & Góp Ý Gần Đây",
                actionText = if (tickets.isNotEmpty()) "Xem tất cả (${tickets.size})" else null,
                onActionClick = { onNavigateTab(NavigationTab.FEEDBACK) }
            )
        }

        item {
            RecentTicketsCard(
                tickets = tickets.take(3),
                onViewAll = { onNavigateTab(NavigationTab.FEEDBACK) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PhoneVerificationCard(
    phone: String,
    onVerifyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpWarning.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HrpWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = HrpWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xác thực số điện thoại",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark
                    )
                )
                Text(
                    text = if (phone.isNotBlank()) "SĐT $phone chưa xác thực OTP" else "Xác thực OTP để bảo vệ tài khoản",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HrpTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
            OutlinedButton(
                onClick = onVerifyClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("home_verify_phone_button")
            ) {
                Text("Xác thực", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HrpPrimary)
            }
        }
    }
}

@Composable
private fun WorkerVerificationStatusCard(
    status: VerificationStatus,
    reviewNote: String?,
    onActionClick: () -> Unit
) {
    val (badgeColor, titleText, subtitleText, buttonLabel) = when (status) {
        VerificationStatus.PENDING_REVIEW -> listOf(
            HrpWarning,
            "Hồ sơ công nhân đang chờ HR đối soát",
            "Bộ phận Nhân sự đang kiểm tra thông tin CCCD và mã nhân viên dự án của bạn.",
            "Xem trạng thái"
        )
        VerificationStatus.NEEDS_INFO -> listOf(
            HrpError,
            "Cần bổ sung thông tin xác thực công nhân",
            reviewNote?.takeIf { it.isNotBlank() } ?: "Vui lòng kiểm tra lại CCCD hoặc Mã nhân viên và gửi lại.",
            "Cập nhật lại"
        )
        VerificationStatus.REJECTED -> listOf(
            HrpError,
            "Yêu cầu xác thực công nhân bị từ chối",
            reviewNote?.takeIf { it.isNotBlank() } ?: "Thông tin không khớp với danh sách dự án. Vui lòng gửi lại hồ sơ chính xác.",
            "Gửi lại hồ sơ"
        )
        else -> listOf(
            HrpPrimary,
            "Xác thực Công nhân HRP để mở khóa Chấm công",
            "Dành cho NLĐ đã nhận việc: Liên kết CCCD & Mã nhân viên dự án để chấm công GPS và giới thiệu người làm.",
            "Xác thực ngay"
        )
    }

    val accentColor = badgeColor as Color

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText as String,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HrpNavyDark
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText as String,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HrpTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_worker_verification_button")
            ) {
                Text(buttonLabel as String, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun WorkerHeroCard(
    profile: WorkerProfile,
    verificationStatus: VerificationStatus,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onOpenWorkerVerification: () -> Unit
) {
    val isApprovedWorker = verificationStatus == VerificationStatus.APPROVED
    val badgeBg = if (isApprovedWorker) HrpSuccess.copy(alpha = 0.2f) else HrpWarning.copy(alpha = 0.2f)
    val badgeColor = if (isApprovedWorker) HrpSuccess else HrpWarning
    val badgeLabel = if (isApprovedWorker) "ĐÃ XÁC THỰC" else verificationStatus.label.uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Worker identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HrpPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.fullName.take(2).uppercase().ifBlank { "NL" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName.ifBlank { "Người Lao Động" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Mã NV: ${profile.employeeCode.ifBlank { "Chưa liên kết" }} • ${profile.position.ifBlank { "Người lao động" }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Factory badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HrpNavySurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = HrpPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = profile.factoryLocation.ifBlank { "Chưa phân công dự án làm việc" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check-in action bar (gated by Worker Verification approval)
            if (isApprovedWorker) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_check_in"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vào Ca (GPS)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCheckOut,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_check_out"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tan Ca (GPS)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenWorkerVerification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_require_verification"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = HrpPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cần xác thực công nhân HRP để chấm công GPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkerQuickStats(
    actualWorkDays: Double,
    otHours: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Bảng Công Tóm Tắt",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Công tích lũy", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary))
                    Text(
                        "$actualWorkDays công",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HrpSuccess)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Làm thêm OT", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary))
                    Text(
                        "${otHours}h",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HrpWarning)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerQuickActions(
    onAdvanceSalary: () -> Unit,
    onLeaveRequest: () -> Unit,
    onDispute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionTile(
            title = "Chế Độ & Lương",
            subtitle = "Hỏi đáp, phản ánh",
            icon = Icons.Default.MonetizationOn,
            color = HrpPrimary,
            modifier = Modifier.weight(1f),
            onClick = onAdvanceSalary
        )
        ActionTile(
            title = "Môi Trường Làm Việc",
            subtitle = "An toàn, ca kíp",
            icon = Icons.Default.DateRange,
            color = HrpInfo,
            modifier = Modifier.weight(1f),
            onClick = onLeaveRequest
        )
        ActionTile(
            title = "Khiếu Nại Công",
            subtitle = "Sai giờ, quên quẹt",
            icon = Icons.Default.Timer,
            color = HrpWarning,
            modifier = Modifier.weight(1f),
            onClick = onDispute
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = HrpSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 10.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentTicketsCard(
    tickets: List<TicketItem>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (tickets.isEmpty()) {
                Text(
                    text = "Chưa có góp ý hoặc yêu cầu nào",
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                )
            } else {
                tickets.forEachIndexed { index, ticket ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ticket.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ticket.code} • ${ticket.createdDate}",
                                style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp)
                            )
                        }
                        TicketStatusBadge(status = ticket.status)
                    }
                    if (index < tickets.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = HrpSurfaceBorder
                        )
                    }
                }
            }
        }
    }
}
