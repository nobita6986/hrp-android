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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
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
import com.example.data.model.PayslipSummary
import com.example.data.model.UserRole
import com.example.data.model.WorkerProfile
import com.example.data.repository.HrpFormatters
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
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

@Composable
fun PayrollScreen(
    currentRole: UserRole,
    workerProfile: WorkerProfile,
    payslip: PayslipSummary,
    onRequestAdvanceSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Hero Net Salary Card
        item {
            NetSalaryHeroCard(
                payslip = payslip,
                workerProfile = workerProfile,
                onRequestAdvanceSalary = onRequestAdvanceSalary
            )
        }

        // Pre-computed snapshot badge (high concurrency badge from architecture)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HrpSurfaceSubtle),
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = HrpSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bản Ghi Lương Hợp Nhất (Pre-computed JSON)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = HrpNavyDark
                        )
                        Text(
                            text = "Đã khóa số liệu kỳ ${payslip.monthPeriod} • Khớp lệnh với ngân hàng",
                            color = HrpTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Gross Earnings Breakdown
        item {
            HrpSectionHeader(
                title = "1. Các Khoản Thu Nhập (Gross Income)",
                subtitle = "Tổng thu nhập trước khi trừ bảo hiểm và thuế"
            )
        }

        item {
            EarningsBreakdownCard(payslip = payslip)
        }

        // Mandatory Deductions Breakdown
        item {
            HrpSectionHeader(
                title = "2. Các Khoản Giảm Trừ (Deductions)",
                subtitle = "Theo quy định Luật Lao Động & BHXH Việt Nam"
            )
        }

        item {
            DeductionsBreakdownCard(payslip = payslip)
        }

        // Bank Account & Payment Destination
        item {
            HrpSectionHeader(
                title = "3. Tài Khoản Nhận Lương",
                subtitle = "Giải ngân tự động vào ngày ${payslip.payDate}"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HrpSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HrpNavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workerProfile.bankAccount,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HrpNavyDark
                        )
                        Text(
                            text = "Chủ TK: ${workerProfile.fullName.uppercase()}",
                            color = HrpTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HrpSuccessContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ĐÃ XÁC THỰC", color = HrpSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun NetSalaryHeroCard(
    payslip: PayslipSummary,
    workerProfile: WorkerProfile,
    onRequestAdvanceSalary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PHIẾU LƯƠNG ĐIỆN TỬ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = payslip.monthPeriod,
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
                        text = "ĐÃ THANH TOÁN",
                        color = HrpSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "THỰC LĨNH CHUYỂN KHOẢN (NET)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = HrpFormatters.formatVnd(payslip.netSalaryVnd),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Work days summary line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HrpNavySurface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Công thực tế: ${payslip.actualWorkDays} / ${payslip.standardWorkDays} ngày",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Tăng ca OT: ${payslip.otHours}h",
                    color = HrpWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advance Salary Button
            Button(
                onClick = onRequestAdvanceSalary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payroll_request_advance_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yêu Cầu Tạm Ứng Lương Kỳ Này",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EarningsBreakdownCard(payslip: PayslipSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SalaryRow("Lương cơ bản theo ngày công", HrpFormatters.formatVnd(payslip.baseSalaryVnd), "${payslip.actualWorkDays} ngày")
            SalaryRow("Tiền làm thêm ca OT (150% - 200%)", HrpFormatters.formatVnd(payslip.otPayVnd), "${payslip.otHours} giờ OT")
            SalaryRow("Phụ cấp chuyên cần", HrpFormatters.formatVnd(payslip.attendanceBonusVnd), "Đạt 100% công")
            SalaryRow("Phụ cấp ăn ca trưa & tăng ca", HrpFormatters.formatVnd(payslip.mealAllowanceVnd), "2 bữa / ngày")
            SalaryRow("Hỗ trợ nhà trọ & đi lại", HrpFormatters.formatVnd(payslip.transportAllowanceVnd), "Cố định tháng")

            HorizontalDivider(color = HrpSurfaceBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TỔNG THU NHẬP (GROSS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HrpNavyDark
                )
                Text(
                    text = HrpFormatters.formatVnd(payslip.grossSalaryVnd),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HrpSuccess
                )
            }
        }
    }
}

@Composable
private fun DeductionsBreakdownCard(payslip: PayslipSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SalaryRow("Bảo hiểm xã hội (BHXH 8%)", "-${HrpFormatters.formatVnd(payslip.socialInsuranceVnd)}", "Trừ theo lương đóng BH")
            SalaryRow("Bảo hiểm y tế (BHYT 1.5%)", "-${HrpFormatters.formatVnd(payslip.healthInsuranceVnd)}", "Khám chữa bệnh nhà máy")
            SalaryRow("Bảo hiểm thất nghiệp (BHTN 1%)", "-${HrpFormatters.formatVnd(payslip.unemploymentInsVnd)}", "Theo quy định nhà nước")
            SalaryRow("Thuế TNCN (PIT)", "0 đ", "Đã trừ gia cảnh bản thân")
            SalaryRow("Tạm ứng lương đã nhận trong tháng", "-${HrpFormatters.formatVnd(payslip.advancePaidVnd)}", "Đã chuyển đợt 1", isNegative = true)

            HorizontalDivider(color = HrpSurfaceBorder)

            val totalDeductions = payslip.socialInsuranceVnd + payslip.healthInsuranceVnd +
                payslip.unemploymentInsVnd + payslip.advancePaidVnd

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TỔNG KHOẢN GIẢM TRỪ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HrpNavyDark
                )
                Text(
                    text = "-${HrpFormatters.formatVnd(totalDeductions)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HrpError
                )
            }
        }
    }
}

@Composable
private fun SalaryRow(
    title: String,
    amount: String,
    note: String,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = HrpNavyDark)
            Text(text = note, fontSize = 10.sp, color = HrpTextSecondary)
        }
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) HrpError else HrpNavyDark
        )
    }
}
