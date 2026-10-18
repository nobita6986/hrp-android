package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpTextSecondary
import com.example.ui.viewmodel.NavigationTab

@Composable
fun HrpBottomBar(
    currentTab: NavigationTab,
    pendingTicketsCount: Int,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("hrp_bottom_bar"),
        containerColor = HrpSurface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(NavigationTab.HOME, "Tổng quan", Icons.Default.Dashboard),
            Triple(NavigationTab.ATTENDANCE, "Chấm công", Icons.Default.CalendarMonth),
            Triple(NavigationTab.PAYROLL, "Phiếu lương", Icons.AutoMirrored.Filled.ReceiptLong),
            Triple(NavigationTab.TICKETS, "Yêu cầu", Icons.Default.Assignment),
            Triple(NavigationTab.JOBS, "Việc làm & CTV", Icons.Default.WorkOutline)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == NavigationTab.TICKETS && pendingTicketsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = HrpPrimary) {
                                    Text(
                                        text = "$pendingTicketsCount",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HrpPrimary,
                    selectedTextColor = HrpPrimary,
                    unselectedIconColor = HrpTextSecondary,
                    unselectedTextColor = HrpTextSecondary,
                    indicatorColor = HrpPrimarySoft
                ),
                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
            )
        }
    }
}
