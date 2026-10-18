package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceDay
import com.example.data.model.AttendanceStatus
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSuccess
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextPrimary
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning

@Composable
fun AttendanceScreen(
    currentRole: UserRole,
    attendanceList: List<AttendanceDay>,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onDisputeAttendance: (AttendanceDay) -> Unit,
    onApproveAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<AttendanceStatus?>(null) }

    val filteredList = if (selectedFilter == null) {
        attendanceList
    } else {
        attendanceList.filter { it.status == selectedFilter }
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
                totalDays = attendanceList.size,
                approvedDays = attendanceList.count { it.status == AttendanceStatus.APPROVED },
                totalOtHours = attendanceList.sumOf { it.otHours },
                onCheckIn = onCheckIn,
                onCheckOut = onCheckOut
            )
        }

        // HR batch approval bar
        if (currentRole == UserRole.HR_STAFF || currentRole == UserRole.BOD) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpPrimarySoft),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Chế độ HR Duyệt Bảng Công",
                                fontWeight = FontWeight.Bold,
                                color = HrpPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                "Kiểm tra GPS và giờ quẹt vân tay xưởng",
                                color = HrpTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onApproveAll,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                            modifier = Modifier.testTag("attendance_approve_all")
                        ) {
                            Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duyệt Hết", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Interactive mini calendar bar
        item {
            HrpSectionHeader(
                title = "Lịch Chấm Công Tháng 10/2026",
                subtitle = "Bấm vào ngày để xem chi tiết quẹt thẻ"
            )
        }

        item {
            AttendanceCalendarRow(attendanceList = attendanceList)
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
                        label = { Text("Khiếu nại (${attendanceList.count { it.status == AttendanceStatus.DISPUTED }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = com.example.ui.theme.HrpError,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Attendance records list
        items(filteredList, key = { it.id }) { day ->
            AttendanceCardItem(
                day = day,
                onDispute = { onDisputeAttendance(day) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AttendanceSummaryBanner(
    totalDays: Int,
    approvedDays: Int,
    totalOtHours: Double,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
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
                Column {
                    Text(
                        text = "CHẤM CÔNG 3 TẦNG (HRP V4)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Thái Nguyên • Hôm nay 18/10",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
                        text = "GPS HỢP LỆ",
                        color = HrpSuccess,
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
                    .background(com.example.ui.theme.HrpNavySurface)
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
                    Text("Vào Ca", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCheckOut,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text("Tan Ca", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AttendanceCalendarRow(attendanceList: List<AttendanceDay>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val days = listOf("13", "14", "15", "16", "17", "18")
        val weekdays = listOf("T3", "T4", "T5", "T6", "T7", "CN")

        items(days.indices.toList()) { index ->
            val dayNum = days[index]
            val weekday = weekdays[index]
            val isToday = dayNum == "18"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isToday) HrpNavyDark else HrpSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isToday) 2.dp else 1.dp,
                    color = if (isToday) HrpPrimary else HrpSurfaceBorder
                ),
                modifier = Modifier.width(52.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isToday) Color.White.copy(alpha = 0.7f) else HrpTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color.White else HrpNavyDark
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (dayNum) {
                                    "13" -> com.example.ui.theme.HrpError
                                    "18" -> HrpWarning
                                    else -> HrpSuccess
                                }
                            )
                    )
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
                        text = "${day.dayOfWeek}, ${day.dateString}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                AttendanceStatusBadge(status = day.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = day.shiftName,
                style = MaterialTheme.typography.bodySmall.copy(color = HrpPrimary, fontWeight = FontWeight.Medium)
            )

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
                        text = "Đang xử lý khiếu nại",
                        style = MaterialTheme.typography.labelSmall.copy(color = com.example.ui.theme.HrpError, fontWeight = FontWeight.Bold)
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
                        color = if (day.status == AttendanceStatus.DISPUTED) com.example.ui.theme.HrpError else HrpTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
