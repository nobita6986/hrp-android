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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.BodOverviewStats
import com.example.data.model.PayslipSummary
import com.example.data.model.TicketItem
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.data.model.WorkerProfile
import com.example.data.repository.HrpFormatters
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.HrpSectionHeader
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpInfo
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpNavySurface
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimaryDark
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSuccessContainer
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextPrimary
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning
import com.example.ui.viewmodel.NavigationTab

@Composable
fun HomeScreen(
    currentRole: UserRole,
    workerProfile: WorkerProfile,
    payslip: PayslipSummary,
    tickets: List<TicketItem>,
    bodMetrics: BodOverviewStats,
    onNavigateTab: (NavigationTab) -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onRequestAdvanceSalary: () -> Unit,
    onCreateTicket: (TicketType) -> Unit,
    onApproveAllAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Role-based main content
        when (currentRole) {
            UserRole.WORKER -> {
                item {
                    WorkerHeroCard(
                        profile = workerProfile,
                        onCheckIn = onCheckIn,
                        onCheckOut = onCheckOut
                    )
                }

                item {
                    WorkerQuickStats(
                        actualWorkDays = 24.5,
                        otHours = 28.0,
                        estimatedIncomeVnd = 12_400_000L,
                        onViewPayslip = { onNavigateTab(NavigationTab.PAYROLL) }
                    )
                }

                item {
                    HrpSectionHeader(
                        title = "Lối Tắt Hỗ Trợ Công Nhân",
                        subtitle = "Gửi yêu cầu trực tiếp đến HR & Kế toán"
                    )
                }

                item {
                    WorkerQuickActions(
                        onAdvanceSalary = onRequestAdvanceSalary,
                        onLeaveRequest = { onCreateTicket(TicketType.LEAVE_REQUEST) },
                        onDispute = { onCreateTicket(TicketType.TIMESHEET_DISPUTE) }
                    )
                }

                item {
                    HrpSectionHeader(
                        title = "Yêu Cầu Gần Đây",
                        actionText = "Xem tất cả (${tickets.size})",
                        onActionClick = { onNavigateTab(NavigationTab.TICKETS) }
                    )
                }

                item {
                    RecentTicketsCard(
                        tickets = tickets.take(2),
                        onViewAll = { onNavigateTab(NavigationTab.TICKETS) }
                    )
                }
            }

            UserRole.HR_STAFF -> {
                item {
                    HrStaffDashboardHero(
                        bodMetrics = bodMetrics,
                        onApproveAll = onApproveAllAttendance
                    )
                }

                item {
                    HrpSectionHeader(
                        title = "Nhiệm Vụ HR Cần Xử Lý Hôm Nay",
                        subtitle = "Phê duyệt công & giải quyết đơn từ"
                    )
                }

                item {
                    HrTasksList(
                        pendingTickets = tickets.filter { it.status == com.example.data.model.TicketStatus.PENDING },
                        onViewTickets = { onNavigateTab(NavigationTab.TICKETS) },
                        onViewAttendance = { onNavigateTab(NavigationTab.ATTENDANCE) }
                    )
                }

                item {
                    HrpSectionHeader(
                        title = "Tình Hình Nhân Sự Các Phân Xưởng",
                        subtitle = "Tỷ lệ chuyên cần ngày 18/10"
                    )
                }

                item {
                    FactoryWorkforceStatusCard()
                }
            }

            UserRole.BOD -> {
                item {
                    BodControlTowerHero(bodMetrics = bodMetrics)
                }

                item {
                    HrpSectionHeader(
                        title = "4 Thẻ Chỉ Số Sinh Tử (HRP Core Metrics)",
                        subtitle = "Báo cáo điều hành tổng thể toàn bộ hệ thống"
                    )
                }

                item {
                    BodVitalMetricsGrid(bodMetrics = bodMetrics)
                }

                item {
                    HrpSectionHeader(
                        title = "Phân Bổ Quỹ Lương & Dự Án Tháng 10",
                        subtitle = "Chi phí nhân sự theo các khách hàng lớn"
                    )
                }

                item {
                    BodProjectBreakdownCard()
                }
            }

            UserRole.CTV -> {
                item {
                    CtvWalletHeroCard()
                }

                item {
                    HrpSectionHeader(
                        title = "Công Cụ Tuyển Dụng Dành Cho CTV",
                        subtitle = "Chia sẻ link giới thiệu và nhận hoa hồng"
                    )
                }

                item {
                    CtvToolsCard(onExploreJobs = { onNavigateTab(NavigationTab.JOBS) })
                }

                item {
                    HrpSectionHeader(
                        title = "Việc Làm Hoa Hồng Cao Nhất Tuần",
                        actionText = "Xem chợ việc làm",
                        onActionClick = { onNavigateTab(NavigationTab.JOBS) }
                    )
                }

                item {
                    CtvHotJobsCard(onExploreJobs = { onNavigateTab(NavigationTab.JOBS) })
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// -------------------------------------------------------------
// WORKER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun WorkerHeroCard(
    profile: WorkerProfile,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
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
                        text = profile.fullName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Mã NV: ${profile.employeeCode} • ${profile.position}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpSuccess.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ĐANG TRONG CA",
                        color = HrpSuccess,
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
                    text = profile.factoryLocation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check-in action bar
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
                    Text("Tan Ca", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WorkerQuickStats(
    actualWorkDays: Double,
    otHours: Double,
    estimatedIncomeVnd: Long,
    onViewPayslip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewPayslip() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bảng Công & Thu Nhập T10/2026",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Chi tiết phiếu lương →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HrpPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Công tích lũy", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary))
                    Text(
                        "$actualWorkDays công",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HrpSuccess)
                    )
                }
                Column {
                    Text("Làm thêm OT", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary))
                    Text(
                        "${otHours}h",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HrpWarning)
                    )
                }
                Column {
                    Text("Lương ước tính", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary))
                    Text(
                        HrpFormatters.formatVndShort(estimatedIncomeVnd),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HrpPrimary)
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
            title = "Tạm Ứng Lương",
            subtitle = "Nhận trong 24h",
            icon = Icons.Default.MonetizationOn,
            color = HrpPrimary,
            modifier = Modifier.weight(1f),
            onClick = onAdvanceSalary
        )
        ActionTile(
            title = "Xin Nghỉ Phép",
            subtitle = "Phép năm, ốm",
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
                    com.example.ui.components.TicketStatusBadge(status = ticket.status)
                }
                if (index < tickets.size - 1) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = HrpSurfaceBorder
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HR STAFF COMPONENTS
// -------------------------------------------------------------
@Composable
private fun HrStaffDashboardHero(
    bodMetrics: BodOverviewStats,
    onApproveAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TRUNG TÂM ĐIỀU PHỐI NHÂN SỰ",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HrpPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Quản lý 2.450 Công nhân tại 4 Phân xưởng",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpNavySurface)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Điểm danh hôm nay", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("98.4%", color = HrpSuccess, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpNavySurface)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Đơn chờ HR duyệt", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("${bodMetrics.pendingTicketsCount} đơn", color = HrpWarning, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onApproveAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hr_approve_all_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
            ) {
                Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Duyệt Nhanh Toàn Bộ Chấm Công Ca Sáng", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HrTasksList(
    pendingTickets: List<TicketItem>,
    onViewTickets: () -> Unit,
    onViewAttendance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HrpWarning.copy(alpha = 0.1f))
                    .clickable { onViewTickets() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = HrpWarning)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Đơn tạm ứng lương cần HR ký duyệt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("2 công nhân đang chờ duyệt để chuyển kế toán", color = HrpTextSecondary, fontSize = 11.sp)
                }
                Text("Duyệt →", color = HrpWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HrpInfo.copy(alpha = 0.1f))
                    .clickable { onViewAttendance() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = HrpInfo)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Đối soát chấm công & Khiếu nại sai giờ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Xe đưa đón tuyến SEVT vào trễ ngày 13/10", color = HrpTextSecondary, fontSize = 11.sp)
                }
                Text("Xem →", color = HrpInfo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FactoryWorkforceStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FactoryRow("Samsung SEVT - Thái Nguyên", "1.120 / 1.150 người", 97.3)
            FactoryRow("Foxconn KCN Quang Châu - Bắc Giang", "680 / 700 người", 97.1)
            FactoryRow("Pegatron Deep C - Hải Phòng", "450 / 460 người", 97.8)
            FactoryRow("VinFast Cát Hải - Hải Phòng", "200 / 200 người", 100.0)
        }
    }
}

@Composable
private fun FactoryRow(name: String, count: String, rate: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Text(count, color = HrpTextSecondary, fontSize = 11.sp)
        }
        Text(
            "$rate%",
            color = if (rate >= 98.0) HrpSuccess else HrpPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// -------------------------------------------------------------
// BOD CONTROL TOWER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun BodControlTowerHero(bodMetrics: BodOverviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HrpSuccess)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REALTIME CONTROL TOWER",
                    color = HrpSuccess,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Báo Cáo Sức Khỏe Doanh Nghiệp HRP",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tổng lực lượng lao động: ${bodMetrics.activeWorkersCount} nhân sự (+${bodMetrics.workerGrowthPercent}% so với tháng trước)",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BodVitalMetricsGrid(bodMetrics: BodOverviewStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "1. Active Headcount",
                value = "${bodMetrics.activeWorkersCount} CN",
                subtitle = "+4.8% tăng trưởng",
                icon = Icons.Default.People,
                iconTint = HrpPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "2. Fill Rate Dự Án",
                value = "${bodMetrics.fillRatePercent}%",
                subtitle = "Chỉ tiêu: >95%",
                icon = Icons.Default.Speed,
                iconTint = HrpSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "3. Tổng Quỹ Lương T10",
                value = HrpFormatters.formatVndShort(bodMetrics.totalPayrollVnd),
                subtitle = "Bao gồm BHXH & OT",
                icon = Icons.Default.MonetizationOn,
                iconTint = HrpWarning,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "4. Tuyển Dụng SLA",
                value = "${bodMetrics.recruitmentSlaDays} Ngày",
                subtitle = "Tốc độ cung ứng lao động",
                icon = Icons.Default.TrendingUp,
                iconTint = HrpInfo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BodProjectBreakdownCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Chi phí dự án trọng điểm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ProjectCostRow("Tổ hợp Samsung Electronics", "10.4 Tỷ đ", "48% quỹ lương")
            ProjectCostRow("Đối tác Foxconn Industrial", "6.2 Tỷ đ", "28% quỹ lương")
            ProjectCostRow("Pegatron Technology", "3.6 Tỷ đ", "16% quỹ lương")
            ProjectCostRow("VinFast Auto", "1.65 Tỷ đ", "8% quỹ lương")
        }
    }
}

@Composable
private fun ProjectCostRow(project: String, cost: String, share: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(project, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Text(share, color = HrpTextSecondary, fontSize = 11.sp)
        }
        Text(cost, color = HrpNavyDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// -------------------------------------------------------------
// CTV AFFILIATE COMPONENTS
// -------------------------------------------------------------
@Composable
private fun CtvWalletHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VÍ HOA HỒNG CTV",
                    color = HrpPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Mã CTV: HRP-CTV-088",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "12.500.000 đ",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Text(
                text = "Số dư khả dụng (Đã đối soát & sẵn sàng rút)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { /* Rút tiền */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rút Tiền Ngay", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { /* Chia sẻ */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chia Sẻ Link", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CtvToolsCard(onExploreJobs: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HrpPrimarySoft)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = HrpPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mã QR Tuyển Dụng Độc Quyền", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("In poster hoặc dán ở bảng tin để ứng viên quét nộp hồ sơ", color = HrpTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CtvHotJobsCard(onExploreJobs: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExploreJobs() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("VinFast Cát Hải - Thợ máy CNC", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Hoa hồng: 1.500.000 đ / lao động đi làm", color = HrpPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HrpError.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("GẤP", color = HrpError, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}
