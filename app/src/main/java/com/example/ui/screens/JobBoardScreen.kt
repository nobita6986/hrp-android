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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.JobOpening
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary

@Composable
fun JobBoardScreen(
    jobs: List<JobOpening>,
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
            JobBoardHeroBanner()
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

        if (filteredJobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Không tìm thấy vị trí tuyển dụng phù hợp",
                            color = HrpTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredJobs, key = { it.id }) { job ->
                JobCardItem(
                    job = job,
                    onApply = { onApplyJob(job) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun JobBoardHeroBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HrpNavyDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HỆ THỐNG VIỆC LÀM HRP",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HrpPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Cơ Hội Việc Làm Tại Các Nhà Máy FDI",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kết nối trực tiếp người lao động tới các doanh nghiệp sản xuất uy tín. Hỗ trợ thủ tục nhanh gọn, phỏng vấn trực tiếp tại xưởng.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun JobCardItem(
    job: JobOpening,
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

            // Salary banner
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Benefits tags
            if (job.benefits.isNotEmpty()) {
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
            }

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
                    text = "Nộp Hồ Sơ Ứng Tuyển",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
