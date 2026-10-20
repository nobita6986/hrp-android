package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TicketItem
import com.example.data.model.TicketStatus
import com.example.data.model.TicketType
import com.example.data.repository.HrpFormatters
import com.example.ui.components.TicketStatusBadge
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.theme.HrpWarning

@Composable
fun TicketScreen(
    tickets: List<TicketItem>,
    phoneVerified: Boolean = true,
    onCreateTicketClick: () -> Unit,
    onOpenPhoneOtp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTypeFilter by remember { mutableStateOf<TicketType?>(null) }

    val filteredTickets = if (selectedTypeFilter == null) {
        tickets
    } else {
        tickets.filter { it.type == selectedTypeFilter }
    }

    Box(modifier = modifier.fillMaxSize().background(HrpBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Header summary banner
            item {
                TicketHeaderBanner(
                    total = tickets.size,
                    pending = tickets.count { it.status == TicketStatus.PENDING || it.status == TicketStatus.HR_APPROVED },
                    onCreateTicket = onCreateTicketClick
                )
            }

            // OTP Phone Verification Required Notice when phoneVerified=false
            if (!phoneVerified) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_otp_required_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HrpSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HrpWarning.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Yêu cầu xác thực số điện thoại qua OTP (phoneVerified = false)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HrpNavyDark
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tài khoản của bạn chưa xác thực số điện thoại bằng mã OTP nên chưa thể gửi góp ý mới lên máy chủ. Hệ thống không giả lập gửi thành công khi chưa xác thực OTP.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HrpTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onOpenPhoneOtp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feedback_open_phone_otp_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Xác minh số điện thoại qua OTP",
                                    fontWeight = FontWeight.Bold,
                                    color = HrpPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("Tất cả (${tickets.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HrpPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    TicketType.values().forEach { type ->
                        val count = tickets.count { it.type == type }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = type },
                                label = { Text("${type.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HrpNavyDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Tickets List
            if (filteredTickets.isEmpty()) {
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
                                text = "Chưa có góp ý hoặc yêu cầu nào",
                                color = HrpTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredTickets, key = { it.id }) { ticket ->
                    TicketCard(ticket = ticket)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Padding for FAB
        }

        // Floating Action Button to create Ticket
        FloatingActionButton(
            onClick = onCreateTicketClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_create_ticket"),
            containerColor = HrpPrimary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tạo đơn mới")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gửi Góp Ý / Yêu Cầu", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TicketHeaderBanner(
    total: Int,
    pending: Int,
    onCreateTicket: () -> Unit
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
                        text = "SỔ TIẾP NHẬN YÊU CẦU & GÓP Ý",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Góp Ý & Đơn Từ Của Bạn",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpWarning.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pending ĐANG XỬ LÝ",
                        color = HrpWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tổng cộng $total yêu cầu đã gửi. Bạn có thể gửi phản hồi, xin nghỉ phép, tạm ứng lương hoặc khiếu nại công tại đây.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TicketCard(
    ticket: TicketItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Type & Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (ticket.type) {
                                    TicketType.LEAVE_REQUEST -> com.example.ui.theme.HrpInfo.copy(alpha = 0.15f)
                                    TicketType.ADVANCE_SALARY -> HrpPrimarySoft
                                    TicketType.TIMESHEET_DISPUTE -> HrpWarning.copy(alpha = 0.15f)
                                    TicketType.OTHER -> HrpSurfaceSubtle
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (ticket.type) {
                                TicketType.LEAVE_REQUEST -> Icons.Default.DateRange
                                TicketType.ADVANCE_SALARY -> Icons.Default.MonetizationOn
                                TicketType.TIMESHEET_DISPUTE -> Icons.Default.Timer
                                else -> Icons.Default.Assignment
                            },
                            contentDescription = null,
                            tint = when (ticket.type) {
                                TicketType.LEAVE_REQUEST -> com.example.ui.theme.HrpInfo
                                TicketType.ADVANCE_SALARY -> HrpPrimary
                                TicketType.TIMESHEET_DISPUTE -> HrpWarning
                                else -> HrpNavyDark
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = ticket.type.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HrpNavyDark
                        )
                        Text(
                            text = "${ticket.code} • ${ticket.createdDate}",
                            fontSize = 11.sp,
                            color = HrpTextSecondary
                        )
                    }
                }

                TicketStatusBadge(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Description
            Text(
                text = ticket.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = HrpNavyDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                fontSize = 12.sp,
                color = HrpTextSecondary,
                lineHeight = 16.sp
            )

            // Extra fields (Amount for advance, days for leave)
            if (ticket.amountVnd != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpPrimarySoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Số tiền xin tạm ứng:", fontSize = 11.sp, color = HrpTextSecondary)
                    Text(
                        HrpFormatters.formatVnd(ticket.amountVnd),
                        fontWeight = FontWeight.Bold,
                        color = HrpPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            if (ticket.requestedDays != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpSurfaceSubtle)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thời gian xin nghỉ:", fontSize = 11.sp, color = HrpTextSecondary)
                    Text(
                        ticket.requestedDays,
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark,
                        fontSize = 12.sp
                    )
                }
            }

            // HR response note
            if (ticket.hrNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HrpSurfaceSubtle)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Phản hồi: ${ticket.hrNote}",
                        fontSize = 11.sp,
                        color = HrpNavyDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
