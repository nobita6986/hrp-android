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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.CtvCandidate
import com.example.data.model.JobOpening
import com.example.data.model.UserRole
import com.example.data.repository.HrpFormatters
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpInfo
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpNavySurface
import com.example.ui.theme.HrpPrimary
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
fun JobBoardScreen(
    currentRole: UserRole,
    jobs: List<JobOpening>,
    candidates: List<CtvCandidate>,
    onApplyJob: (JobOpening) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf<String?>(null) }

    val filteredJobs = jobs.filter { job ->
        val matchesSearch = job.title.contains(searchQuery, ignoreCase = true) ||
            job.factoryCompany.contains(searchQuery, ignoreCase = true) ||
            job.industrialZone.contains(searchQuery, ignoreCase = true)

        val matchesZone = selectedZone == null || job.industrialZone.contains(selectedZone!!, ignoreCase = true)

        matchesSearch && matchesZone
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header Hero Banner
        item {
            JobBoardHeroBanner(currentRole = currentRole)
        }

        // CTV Referral Tool Card
        item {
            CtvReferralLinkCard()
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("job_search_input"),
                placeholder = { Text("Tìm theo vị trí, nhà máy (Samsung, Foxconn...)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = HrpTextSecondary)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HrpPrimary,
                    unfocusedBorderColor = HrpSurfaceBorder,
                    focusedContainerColor = HrpSurface,
                    unfocusedContainerColor = HrpSurface
                )
            )
        }

        // Zone Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val zones = listOf("Tất cả", "Bắc Ninh", "Thái Nguyên", "Bắc Giang", "Hải Phòng")
                items(zones) { zone ->
                    val isSelected = (zone == "Tất cả" && selectedZone == null) || (zone == selectedZone)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedZone = if (zone == "Tất cả") null else zone },
                        label = { Text(zone) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HrpPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Job Openings List
        item {
            HrpSectionHeader(
                title = "Vị Trí Đang Tuyển Dụng (${filteredJobs.size})",
                subtitle = "Chính sách phúc lợi tốt, bao ăn ở và đóng BHXH"
            )
        }

        items(filteredJobs, key = { it.id }) { job ->
            JobCardItem(
                job = job,
                currentRole = currentRole,
                onApply = { onApplyJob(job) }
            )
        }

        // CTV Candidate Tracking Section
        item {
            HrpSectionHeader(
                title = "Danh Sách Ứng Viên Đã Giới Thiệu (${candidates.size})",
                subtitle = "Theo dõi tiến độ phỏng vấn & chi trả hoa hồng CTV"
            )
        }

        item {
            CtvCandidateTrackingCard(candidates = candidates)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun JobBoardHeroBanner(currentRole: UserRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HỆ THỐNG CUNG ỨNG LAO ĐỘNG HRP",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HrpPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Chợ Việc Làm KCN & Kênh CTV Tuyển Dụng",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kết nối trực tiếp người lao động tới các nhà máy FDI hàng đầu Việt Nam. Hoa hồng trả thẳng vào ví CTV sau 15 ngày công.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CtvReferralLinkCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HrpPrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = HrpPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Link Giới Thiệu Của Bạn", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Mã CTV: HRP-CTV-088", color = HrpTextSecondary, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HrpSuccessContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("HOẠT ĐỘNG", color = HrpSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HrpSurfaceSubtle)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "https://hrpartner.vn/job?ref=HRP-088",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = HrpNavyDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Sao chép link",
                    tint = HrpPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun JobCardItem(
    job: JobOpening,
    currentRole: UserRole,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = job.factoryCompany,
                        style = MaterialTheme.typography.bodySmall.copy(color = HrpPrimary, fontWeight = FontWeight.Medium)
                    )
                }

                if (job.urgent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HrpError.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TUYỂN GẤP", color = HrpError, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = HrpTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.industrialZone, color = HrpTextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Salary & CTV reward banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HrpPrimarySoft)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mức thu nhập:", fontSize = 10.sp, color = HrpTextSecondary)
                    Text(job.salaryRange, fontWeight = FontWeight.Bold, color = HrpNavyDark, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Hoa hồng CTV:", fontSize = 10.sp, color = HrpTextSecondary)
                    Text(
                        HrpFormatters.formatVnd(job.commissionVnd),
                        fontWeight = FontWeight.Bold,
                        color = HrpPrimary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Benefits tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                job.benefits.take(2).forEach { benefit ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HrpSurfaceSubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(benefit, fontSize = 11.sp, color = HrpNavyDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_job_button_${job.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
            ) {
                Icon(imageVector = Icons.Default.Work, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentRole == UserRole.CTV) "Giới Thiệu Ứng Viên Ngay" else "Nộp Hồ Sơ Ứng Tuyển",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CtvCandidateTrackingCard(candidates: List<CtvCandidate>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            candidates.forEachIndexed { index, candidate ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (candidate.isPaid) HrpSuccessContainer else HrpWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (candidate.isPaid) Icons.Default.CheckCircle else Icons.Default.People,
                            contentDescription = null,
                            tint = if (candidate.isPaid) HrpSuccess else HrpWarning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.candidateName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(candidate.appliedJob, color = HrpTextSecondary, fontSize = 11.sp)
                        Text(
                            text = "Trạng thái: ${candidate.status}",
                            color = if (candidate.isPaid) HrpSuccess else HrpPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            HrpFormatters.formatVnd(candidate.rewardVnd),
                            fontWeight = FontWeight.Bold,
                            color = HrpNavyDark,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (candidate.isPaid) "ĐÃ TRẢ TIỀN" else "CHỜ DUYỆT CÔNG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (candidate.isPaid) HrpSuccess else HrpWarning
                        )
                    }
                }

                if (index < candidates.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HrpSurfaceBorder)
                }
            }
        }
    }
}
