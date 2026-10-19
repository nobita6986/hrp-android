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
    val isSiteGpsLocked = workerProfile.canCheckIn &&
        (!workerProfile.hasConfiguredSiteGps || workerProfile.siteGeofenceNotConfigured)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Worker hero card with Check-In / Check-Out gated by mobileAccess.canCheckIn & Site GPS confirmation
        item {
            WorkerHeroCard(
                profile = workerProfile,
                onCheckIn = onCheckIn,
                onCheckOut = onCheckOut
            )
        }

        // Status explanation card when check-in is locked (either canCheckIn=false or Site GPS not confirmed by Admin)
        if (!workerProfile.canCheckIn || isSiteGpsLocked) {
            item {
                MobileAccessStatusCard(
                    profile = workerProfile,
                    isSiteGpsLocked = isSiteGpsLocked
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
private fun MobileAccessStatusCard(
    profile: WorkerProfile,
    isSiteGpsLocked: Boolean
) {
    val accentColor = if (isSiteGpsLocked) HrpError else HrpWarning
    val titleText = if (isSiteGpsLocked) {
        "Tạm khóa chấm công: Site chưa được Admin xác nhận GPS"
    } else {
        "Tài khoản chưa mở quyền chấm công (canCheckIn = false)"
    }
    val subtitleText = if (isSiteGpsLocked) {
        "Dự án ${profile.projectName.ifBlank { profile.factoryLocation.ifBlank { "hiện tại" } }} chưa có địa điểm làm việc (Site) được Quản trị viên HRP xác nhận tọa độ GPS chuẩn (geoConfirmedAt). Thao tác Vào ca / Tan ca tạm khóa cho đến khi Admin xác nhận Site."
    } else {
        "Danh tính công nhân được mở quyền chấm công và giới thiệu khi tài khoản đã được Quản trị viên HRP liên kết hồ sơ Worker và có phân công dự án đang hoạt động (ACTIVE). Vui lòng liên hệ nhân sự hoặc tổng đài 0211.2216.999 nếu cần hỗ trợ."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_mobile_access_status_card"),
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
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HrpNavyDark
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HrpTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerHeroCard(
    profile: WorkerProfile,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val canCheckIn = profile.canCheckIn
    val isSiteGpsReady = profile.hasConfiguredSiteGps && !profile.siteGeofenceNotConfigured
    val isActionEnabled = canCheckIn && isSiteGpsReady

    val badgeBg = when {
        isActionEnabled -> HrpSuccess.copy(alpha = 0.2f)
        canCheckIn && !isSiteGpsReady -> HrpError.copy(alpha = 0.2f)
        else -> HrpWarning.copy(alpha = 0.2f)
    }
    val badgeColor = when {
        isActionEnabled -> HrpSuccess
        canCheckIn && !isSiteGpsReady -> HrpError
        else -> HrpWarning
    }
    val badgeLabel = when {
        isActionEnabled -> "ĐỦ ĐIỀU KIỆN"
        canCheckIn && !isSiteGpsReady -> "CHỜ XÁC NHẬN SITE"
        else -> "CHƯA PHÂN CÔNG"
    }

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

            // Factory / Assignment badge
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
                    text = profile.factoryLocation.ifBlank { "Chưa có phân công dự án đang hoạt động (ACTIVE)" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check-in action bar (strictly gated by mobileAccess.canCheckIn && confirmed Site GPS)
            if (isActionEnabled) {
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
                val lockReasonText = if (canCheckIn && !isSiteGpsReady) {
                    "Tạm khóa chấm công: Site dự án chưa được Admin xác nhận tọa độ GPS"
                } else {
                    "Chấm công bị khóa: Tài khoản cần được Admin liên kết và có phân công ACTIVE"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HrpNavySurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("check_in_locked_notice"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lockReasonText,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
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
