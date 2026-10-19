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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpNavySurface
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning

@Composable
fun AttendanceScreen(
    attendanceList: List<AttendanceDay>,
    workerProfile: WorkerProfile? = null,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onDisputeAttendance: (AttendanceDay) -> Unit,
    onOpenWorkerVerification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<AttendanceStatus?>(null) }

    val filteredList = if (selectedFilter == null) {
        attendanceList
    } else {
        attendanceList.filter { it.status == selectedFilter }
    }

    val canCheckIn = workerProfile?.canCheckIn == true
    val isSiteGpsReady = workerProfile?.hasConfiguredSiteGps == true && !(workerProfile.siteGeofenceNotConfigured)
    val isActionEnabled = canCheckIn && isSiteGpsReady
    val siteSubtitle = workerProfile?.factoryLocation?.takeIf { it.isNotBlank() }
        ?: "Chưa có dự án được phân công (ACTIVE)"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Attendance summary banner
        item {
            AttendanceSummaryBanner(
                siteSubtitle = siteSubtitle,
                canCheckIn = canCheckIn,
                isSiteGpsReady = isSiteGpsReady,
                totalDays = attendanceList.size,
                approvedDays = attendanceList.count { it.status == AttendanceStatus.APPROVED },
                totalOtHours = attendanceList.sumOf { it.otHours },
                onCheckIn = onCheckIn,
                onCheckOut = onCheckOut
            )
        }

        // Section header
        item {
            HrpSectionHeader(
                title = "Lịch Sử Chấm Công Thực Tế",
                subtitle = "Dữ liệu đồng bộ trực tiếp từ hệ thống HRP"
            )
        }

        // Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Tất cả (${attendanceList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HrpPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AttendanceStatus.APPROVED,
                        onClick = { selectedFilter = AttendanceStatus.APPROVED },
                        label = { Text("Đã duyệt (${attendanceList.count { it.status == AttendanceStatus.APPROVED }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HrpSuccess,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AttendanceStatus.PENDING,
                        onClick = { selectedFilter = AttendanceStatus.PENDING },
                        label = { Text("Chờ duyệt (${attendanceList.count { it.status == AttendanceStatus.PENDING }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HrpWarning,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AttendanceStatus.DISPUTED,
                        onClick = { selectedFilter = AttendanceStatus.DISPUTED },
                        label = { Text("Ngoài vùng / Khiếu nại (${attendanceList.count { it.status == AttendanceStatus.DISPUTED }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HrpError,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Attendance records list or empty state
        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isActionEnabled -> "Chưa có bản ghi chấm công nào trong kỳ này."
                                canCheckIn && !isSiteGpsReady -> "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn nên chưa thể ghi nhận chấm công."
                                else -> "Tài khoản cần được Quản trị viên HRP liên kết hồ sơ công nhân và phân công dự án ACTIVE để bắt đầu chấm công GPS."
                            },
                            color = HrpTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { day ->
                AttendanceCardItem(
                    day = day,
                    onDispute = { onDisputeAttendance(day) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AttendanceSummaryBanner(
    siteSubtitle: String,
    canCheckIn: Boolean,
    isSiteGpsReady: Boolean,
    totalDays: Int,
    approvedDays: Int,
    totalOtHours: Double,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
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
        else -> "CHƯA MỞ QUYỀN"
    }

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
                        text = "CHẤM CÔNG ĐỊNH VỊ GPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = siteSubtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2
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
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HrpNavySurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Số ngày công", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("$approvedDays / $totalDays", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text("Giờ chuẩn", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("${approvedDays * 8.0}h", color = HrpSuccess, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text("Tăng ca (OT)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("${totalOtHours}h", color = HrpWarning, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isActionEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vào Ca (GPS)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCheckOut,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("Tan Ca (GPS)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val lockExplanation = if (canCheckIn && !isSiteGpsReady) {
                    "Tạm khóa Vào ca / Tan ca: Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn (SITE_GEOFENCE_NOT_CONFIGURED)."
                } else {
                    "Thao tác chấm công bị khóa (canCheckIn = false): Tài khoản cần được Admin liên kết hồ sơ công nhân và có phân công dự án đang hoạt động (ACTIVE)."
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HrpNavySurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            text = lockExplanation,
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
private fun AttendanceCardItem(
    day: AttendanceDay,
    onDispute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header date & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (day.dayOfWeek != day.dateString && day.dayOfWeek.isNotBlank()) {
                            "${day.dayOfWeek}, ${day.dateString}"
                        } else {
                            day.dateString
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                AttendanceStatusBadge(status = day.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.shiftName,
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpPrimary, fontWeight = FontWeight.Medium)
                )
                day.geofenceResult?.let { gf ->
                    Text(
                        text = if (gf.equals("INSIDE", ignoreCase = true)) "GPS: Trong vùng" else "GPS: $gf",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (gf.equals("INSIDE", ignoreCase = true)) HrpSuccess else HrpError,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time check in / check out block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HrpSurfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Giờ vào", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp))
                    Text(
                        text = day.checkInTime ?: "--:--",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = HrpNavyDark)
                    )
                }
                Column {
                    Text("Giờ ra", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp))
                    Text(
                        text = day.checkOutTime ?: "Đang làm",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (day.checkOutTime == null) HrpPrimary else HrpNavyDark
                        )
                    )
                }
                Column {
                    Text("Công chuẩn", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp))
                    Text(
                        text = "${day.standardHours}h",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = HrpSuccess)
                    )
                }
                Column {
                    Text("Tăng ca OT", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp))
                    Text(
                        text = "${day.otHours}h",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (day.otHours > 0) HrpWarning else HrpTextSecondary
                        )
                    )
                }
            }

            // Location & Notes
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = HrpTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = day.factorySite,
                    style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp),
                    modifier = Modifier.weight(1f)
                )
                if (day.status == AttendanceStatus.DISPUTED) {
                    Text(
                        text = "Cần kiểm tra",
                        style = MaterialTheme.typography.labelSmall.copy(color = HrpError, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = "Báo sai công",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onDispute() }
                    )
                }
            }

            if (day.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ghi chú: ${day.note}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (day.status == AttendanceStatus.DISPUTED) HrpError else HrpTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
