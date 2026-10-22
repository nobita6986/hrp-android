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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.CheckInGateState
import com.example.data.model.RewardAttendancePolicy
import com.example.data.model.VerificationStatus
import com.example.data.model.WorkerProfile
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.HrpSectionHeader
import kotlinx.coroutines.delay
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
    lastCheckInEpochMs: Long = 0L,
    lastCheckOutEpochMs: Long = 0L,
    nowEpochMsOverride: Long? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var liveNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nowEpochMsOverride) {
        if (nowEpochMsOverride == null) {
            while (true) {
                delay(30_000L)
                liveNowMs = System.currentTimeMillis()
            }
        }
    }
    val effectiveNowMs = nowEpochMsOverride ?: liveNowMs
    val gateState = remember(attendanceList, lastCheckInEpochMs, lastCheckOutEpochMs, effectiveNowMs) {
        RewardAttendancePolicy.evaluateButtonGate(
            attendanceList = attendanceList,
            cachedCheckInMs = lastCheckInEpochMs,
            cachedCheckOutMs = lastCheckOutEpochMs,
            nowMs = effectiveNowMs
        )
    }

    val filteredList = if (selectedFilter == null) {
        attendanceList
    } else {
        attendanceList.filter { it.status == selectedFilter }
    }

    val canCheckIn = workerProfile?.canCheckIn == true
    val hasActiveAssignment = workerProfile?.hasActiveAssignment == true
    val isSiteGpsReady = workerProfile?.hasConfiguredSiteGps == true && !(workerProfile.siteGeofenceNotConfigured)
    val isActionEnabled = canCheckIn && isSiteGpsReady
    val siteSubtitle = workerProfile?.factoryLocation?.takeIf { it.isNotBlank() }
        ?: "Chưa có dự án được phân công (ACTIVE)"
    val totalStandardHours = remember(attendanceList) {
        val rawSum = attendanceList
            .filter { it.status == AttendanceStatus.APPROVED }
            .sumOf { it.standardHours }
        Math.round(rawSum * 10.0) / 10.0
    }

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
                hasActiveAssignment = hasActiveAssignment,
                isSiteGpsReady = isSiteGpsReady,
                gateState = gateState,
                totalDays = attendanceList.size,
                approvedDays = attendanceList.count { it.status == AttendanceStatus.APPROVED },
                totalStandardHours = totalStandardHours,
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
                                hasActiveAssignment && !isSiteGpsReady -> "Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn nên chưa thể ghi nhận chấm công."
                                hasActiveAssignment && !canCheckIn -> "Hồ sơ công nhân đã có phân công dự án nhưng đang chờ hoàn tất kích hoạt trạng thái chấm công."
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
    hasActiveAssignment: Boolean = false,
    isSiteGpsReady: Boolean,
    gateState: CheckInGateState,
    totalDays: Int,
    approvedDays: Int,
    totalStandardHours: Double,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val isActionEnabled = canCheckIn && isSiteGpsReady
    val isWaitingSiteGps = hasActiveAssignment && !isSiteGpsReady
    val badgeBg = when {
        isWaitingSiteGps -> HrpError.copy(alpha = 0.2f)
        hasActiveAssignment && !canCheckIn -> HrpWarning.copy(alpha = 0.2f)
        else -> HrpWarning.copy(alpha = 0.2f)
    }
    val badgeColor = when {
        isWaitingSiteGps -> HrpError
        hasActiveAssignment && !canCheckIn -> HrpWarning
        else -> HrpWarning
    }
    val badgeLabel: String? = when {
        isActionEnabled -> null
        isWaitingSiteGps -> "CHỜ XÁC NHẬN SITE"
        hasActiveAssignment && !canCheckIn -> "ĐÃ PHÂN CÔNG"
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
                        text = "BẢNG CÔNG CÁ NHÂN",
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

                if (badgeLabel != null) {
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2 stats row (Days & Standard Hours; OT removed as app does not manage OT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HrpNavySurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Số ngày công", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("$approvedDays / $totalDays", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Giờ ghi nhận", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("${totalStandardHours}h", color = HrpSuccess, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isActionEnabled) {
                val hintText = gateState.statusHint
                    ?: "Thực hiện Chấm công Vào ca / Tan ca tại nút chính giữa màn hình Tổng quan."
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HrpNavySurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("attendance_cycle_hint")
                ) {
                    Text(
                        text = hintText,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            } else {
                val lockExplanation = when {
                    isWaitingSiteGps -> "Tạm khóa Vào ca / Tan ca: Địa điểm làm việc (Site) của dự án chưa được Admin xác nhận tọa độ GPS chuẩn (SITE_GEOFENCE_NOT_CONFIGURED)."
                    hasActiveAssignment && !canCheckIn -> "Tạm khóa chấm công: Hồ sơ đang chờ hoàn tất kích hoạt chấm công."
                    else -> "Thao tác chấm công bị khóa: Tài khoản cần được Admin liên kết hồ sơ công nhân và có phân công dự án đang hoạt động (ACTIVE)."
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
                        text = day.checkOutTime ?: if (day.status == AttendanceStatus.ABSENT) "--:--" else "Đang làm",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                day.checkOutTime != null -> HrpNavyDark
                                day.status == AttendanceStatus.ABSENT -> HrpError
                                else -> HrpPrimary
                            }
                        )
                    )
                }
                Column {
                    Text("Công chuẩn", style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary, fontSize = 11.sp))
                    Text(
                        text = "${day.standardHours}h",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (day.status == AttendanceStatus.APPROVED && day.standardHours > 0.0) HrpSuccess else HrpTextSecondary
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
