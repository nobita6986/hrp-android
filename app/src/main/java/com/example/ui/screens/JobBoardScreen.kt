package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JobOpening
import com.example.ui.components.HrpSectionHeader
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimaryDark
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpSurfaceSubtle
import com.example.ui.theme.HrpTextSecondary

@Composable
fun JobBoardScreen(
    jobs: List<JobOpening>,
    onApplyJob: (JobOpening) -> Unit,
    selectedJobDetail: JobOpening? = null,
    isLoadingJobDetail: Boolean = false,
    onViewJobDetail: (JobOpening) -> Unit = {},
    onCloseJobDetail: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (selectedJobDetail != null) {
        JobDetailView(
            job = selectedJobDetail,
            isLoadingDetail = isLoadingJobDetail,
            onBack = onCloseJobDetail,
            onApply = { onApplyJob(selectedJobDetail) },
            modifier = modifier
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf<String?>(null) }

    val dynamicZones = remember(jobs) {
        val fromJobs = jobs.map { it.industrialZone.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (fromJobs.isNotEmpty()) {
            listOf("Tất cả") + fromJobs
        } else {
            listOf("Tất cả", "Bắc Ninh", "Thái Nguyên", "Bắc Giang", "Hải Phòng")
        }
    }

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
                placeholder = { Text("Tìm theo vị trí, nhà máy, khu công nghiệp...") },
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
                items(dynamicZones) { zone ->
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
                subtitle = "Chạm vào từng việc làm để xem chi tiết mô tả, phúc lợi và yêu cầu"
            )
        }

        if (filteredJobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = BorderStroke(1.dp, HrpSurfaceBorder)
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
                    onViewDetail = { onViewJobDetail(job) },
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
    onViewDetail: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .testTag("job_card_${job.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = BorderStroke(1.dp, HrpSurfaceBorder)
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
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = HrpNavyDark
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = job.factoryCompany,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HrpPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                if (job.urgent) {
                    Spacer(modifier = Modifier.width(8.dp))
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

            // Location & Shift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HrpTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.industrialZone, color = HrpTextSecondary, fontSize = 12.sp)
                }

                if (!job.shift.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = HrpTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(job.shift, color = HrpTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Salary & Slots banner
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
                if (job.slotsRemaining > 0) {
                    Text(
                        text = "Còn ${job.slotsRemaining} chỗ trống",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrpPrimaryDark
                    )
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

            // Action Buttons: Xem chi tiết + Ứng tuyển
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_job_detail_button_${job.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HrpSurfaceBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Xem chi tiết",
                        color = HrpNavyDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HrpNavyDark,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("apply_job_button_${job.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Ứng tuyển",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun JobDetailView(
    job: JobOpening,
    isLoadingDetail: Boolean,
    onBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Back button bar
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("back_to_job_list_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại danh sách việc làm",
                        tint = HrpPrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quay lại danh sách việc làm",
                        color = HrpPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Header Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (job.urgent) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(HrpError.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "HOT • TUYỂN GẤP",
                                            color = HrpError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(HrpPrimarySoft)
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = job.statusLabel.ifBlank { "Đang tuyển" },
                                        color = HrpPrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = HrpNavyDark
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Mã việc làm: ${job.slug}",
                            fontSize = 12.sp,
                            color = HrpTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Meta badges (Location, Shift, JobType)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetaBadgeChip(
                                icon = Icons.Default.LocationOn,
                                text = job.industrialZone
                            )
                            if (!job.shift.isNullOrBlank()) {
                                MetaBadgeChip(
                                    icon = Icons.Default.AccessTime,
                                    text = job.shift
                                )
                            }
                            MetaBadgeChip(
                                icon = Icons.Default.Work,
                                text = job.jobTypeLabel.ifBlank { "Toàn thời gian" }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = HrpSurfaceBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 2x2 Summary Grid matching <dl> on web
                        val slotsText = if (job.slotsRemaining > 0) {
                            "Còn ${job.slotsRemaining} chỗ trống"
                        } else {
                            "Đang cập nhật"
                        }
                        val quotaText = job.filledQuotaText.ifBlank {
                            if (job.slotsRemaining > 0) "0/${job.slotsRemaining}" else "Đang cập nhật"
                        }
                        val deadlineText = job.deadlineDisplay.ifBlank { "Đang tuyển liên tục" }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetricBox(
                                label = "Mức lương",
                                value = job.salaryRange,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricBox(
                                label = "Chỗ trống",
                                value = slotsText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetricBox(
                                label = "Chỉ tiêu đã tuyển",
                                value = quotaText,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricBox(
                                label = "Hạn nhận hồ sơ",
                                value = deadlineText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (isLoadingDetail) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = HrpSurface),
                        border = BorderStroke(1.dp, HrpSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = HrpPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Đang tải chi tiết công việc...",
                                fontSize = 13.sp,
                                color = HrpTextSecondary
                            )
                        }
                    }
                }
            }

            // Rich-text Section 1: Mô tả công việc
            item {
                val descText = job.description.ifBlank {
                    buildString {
                        append("• Vị trí tuyển dụng: ${job.position?.takeIf { it.isNotBlank() } ?: job.title}\n")
                        append("• Khu vực làm việc: ${job.industrialZone}\n")
                        if (!job.shift.isNullOrBlank()) {
                            append("• Ca làm việc: ${job.shift}\n")
                        }
                        append("• Mức thu nhập: ${job.salaryRange}")
                    }
                }
                DetailRichTextSectionCard(
                    title = "Mô tả công việc",
                    icon = Icons.Default.Description,
                    content = descText
                )
            }

            // Rich-text Section 2: Phúc lợi
            item {
                val benefitsText = job.benefitsDetail.ifBlank {
                    if (job.benefits.isNotEmpty()) {
                        job.benefits.joinToString("\n") { "• $it" }
                    } else {
                        "• Hưởng đầy đủ chế độ BHXH, BHYT và phụ cấp theo quy định nhà máy."
                    }
                }
                DetailRichTextSectionCard(
                    title = "Phúc lợi",
                    icon = Icons.Default.CheckCircle,
                    content = benefitsText
                )
            }

            // Rich-text Section 3: Yêu cầu ứng viên
            item {
                val reqText = job.requirementsDetail.ifBlank {
                    job.requirements.ifBlank { "Trong độ tuổi lao động, có sức khỏe tốt và giấy tờ tùy thân đầy đủ." }
                }
                DetailRichTextSectionCard(
                    title = "Yêu cầu ứng viên",
                    icon = Icons.Default.Info,
                    content = reqText
                )
            }

            // Rich-text Section 4: Hướng dẫn ứng tuyển
            item {
                val guideText = job.applicationGuide.ifBlank {
                    "👉 Bấm vào nút Ứng tuyển bên dưới hoặc liên hệ tổng đài hỗ trợ HRP: 0211.2216.999"
                }
                DetailRichTextSectionCard(
                    title = "Hướng dẫn ứng tuyển",
                    icon = Icons.Default.Work,
                    content = guideText
                )
            }

            // Section 5: Đơn vị tuyển dụng (Employer sidebar equivalent)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HrpSurface),
                    border = BorderStroke(1.dp, HrpSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                tint = HrpPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Đơn vị tuyển dụng",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HrpNavyDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = job.factoryCompany,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HrpNavyDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HrpTextSecondary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = job.employerAddress.ifBlank { job.industrialZone },
                                fontSize = 12.sp,
                                color = HrpTextSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Sticky Bottom CTA Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = HrpSurface,
            shadowElevation = 8.dp
        ) {
            Column {
                HorizontalDivider(color = HrpSurfaceBorder)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_apply_job_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nộp Hồ Sơ Ứng Tuyển",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBadgeChip(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HrpSurfaceSubtle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HrpTextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = HrpNavyDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryMetricBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) HrpPrimarySoft else HrpSurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = HrpTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) HrpPrimaryDark else HrpNavyDark
        )
    }
}

@Composable
private fun DetailRichTextSectionCard(
    title: String,
    icon: ImageVector,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HrpSurface),
        border = BorderStroke(1.dp, HrpSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HrpPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content,
                fontSize = 13.5.sp,
                color = HrpNavyDark,
                lineHeight = 20.sp
            )
        }
    }
}

