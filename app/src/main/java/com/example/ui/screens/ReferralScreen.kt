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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun ReferralScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 16.dp)
            .testTag("referral_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header Banner
        item {
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
                                text = "CHƯƠNG TRÌNH ĐỒNG ĐỘI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HrpPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Giới Thiệu Người Làm",
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
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CHƯA MỞ",
                                color = HrpWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Chính sách giới thiệu ứng viên đang trong quá trình hoàn thiện và phê duyệt bởi Quản trị viên HRP. Hệ thống sẽ kích hoạt khi chính sách chính thức được công bố.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Program Status & Notice Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HrpSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HrpPrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = HrpPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tiếp nhận giới thiệu tạm đóng",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HrpNavyDark
                            )
                            Text(
                                text = "Chưa tiếp nhận thông tin người được giới thiệu",
                                color = HrpTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HrpSurfaceBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quy định minh bạch:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HrpNavyDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.Policy,
                        title = "Chính sách hoa hồng",
                        description = "Mức thưởng và điều kiện chi trả sẽ được công bố chính thức theo văn bản của Owner/Admin. Ứng dụng không tự ý áp đặt con số."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Sự kiện tính thành công",
                        description = "Ứng viên được liên hệ, nhận việc và hoàn thành đủ số ngày công tối thiểu theo quy định đối soát của công ty."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReferralRuleItem(
                        icon = Icons.Default.People,
                        title = "Xác nhận đồng ý",
                        description = "Người giới thiệu chỉ gửi thông tin tối thiểu sau khi đã nhận được sự đồng thuận của ứng viên để HRP liên hệ. App không tự ứng tuyển thay."
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ReferralRuleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HrpSurfaceSubtle)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HrpPrimary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = HrpNavyDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = HrpTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
