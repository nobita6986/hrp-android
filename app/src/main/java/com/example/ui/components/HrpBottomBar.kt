package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    // Calculate initial unified font size for the 5-tab row based on screen width,
    // and automatically step down if any label overflows a single line.
    val initialFontSizeSp = remember(screenWidthDp) {
        ((screenWidthDp / 5f) / 7.2f).coerceIn(8.5f, 11.5f)
    }
    var unifiedLabelFontSizeSp by remember(screenWidthDp) {
        mutableFloatStateOf(initialFontSizeSp)
    }

    NavigationBar(
        modifier = modifier.testTag("hrp_bottom_bar"),
        containerColor = HrpSurface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(NavigationTab.HOME, "Tổng quan", Icons.Default.Dashboard),
            Triple(NavigationTab.JOBS, "Việc làm", Icons.Default.WorkOutline),
            Triple(NavigationTab.ATTENDANCE, "Bảng công", Icons.Default.CalendarMonth),
            Triple(NavigationTab.FEEDBACK, "Góp ý", Icons.Default.Assignment),
            Triple(NavigationTab.REFERRAL, "Giới thiệu", Icons.Default.Share)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == NavigationTab.FEEDBACK && pendingTicketsCount > 0) {
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
                        fontSize = unifiedLabelFontSizeSp.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        onTextLayout = { textLayoutResult ->
                            if (textLayoutResult.didOverflowWidth && unifiedLabelFontSizeSp > 8.0f) {
                                unifiedLabelFontSizeSp = (unifiedLabelFontSizeSp - 0.5f).coerceAtLeast(8.0f)
                            }
                        }
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

