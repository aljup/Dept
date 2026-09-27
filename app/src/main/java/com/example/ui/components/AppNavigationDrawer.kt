package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtFilter
import com.example.viewmodel.DebtStats

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    user: User?,
    stats: DebtStats,
    selectedFilter: DebtFilter,
    selectedCategory: String?,
    onFilterSelected: (DebtFilter) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    onCheckReminders: (() -> Unit)? = null,
    onLockApp: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("app_navigation_drawer_sheet"),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with User Profile Card
                    DrawerHeader(user = user, stats = stats)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Admin Section if Admin
                    if (user?.isAdmin == true && onOpenAdmin != null) {
                        DrawerSectionTitle(title = "صلاحيات المشرف")
                        DrawerActionItem(
                            title = "لوحة الإدارة الشاملة (تحكم كامل)",
                            icon = Icons.Default.AdminPanelSettings,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onOpenAdmin,
                            testTag = "drawer_admin_panel"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Section 1: الأقسام والمعاملات المالية
                    DrawerSectionTitle(title = "الأقسام والمعاملات")

                    DrawerFilterItem(
                        title = "لوحة التحكم (كافة المعاملات)",
                        subtitle = "${stats.activeCount + stats.paidCount} إجمالي",
                        icon = Icons.Default.Dashboard,
                        isSelected = selectedFilter == DebtFilter.ALL && selectedCategory == null,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onFilterSelected(DebtFilter.ALL)
                            onCategorySelected(null)
                        }
                    )

                    DrawerFilterItem(
                        title = "مستحقات لك (أموال أقرضتها)",
                        subtitle = "${stats.lentCount} نشطة",
                        icon = Icons.Default.ArrowDownward,
                        isSelected = selectedFilter == DebtFilter.CREDITOR,
                        tint = CreditGreen,
                        onClick = {
                            onFilterSelected(DebtFilter.CREDITOR)
                        }
                    )

                    DrawerFilterItem(
                        title = "التزامات عليك (أموال اقترضتها)",
                        subtitle = "${stats.borrowedCount} نشطة",
                        icon = Icons.Default.ArrowUpward,
                        isSelected = selectedFilter == DebtFilter.DEBTOR,
                        tint = DebtRed,
                        onClick = {
                            onFilterSelected(DebtFilter.DEBTOR)
                        }
                    )

                    DrawerFilterItem(
                        title = "المعاملات النشطة",
                        subtitle = "${stats.activeCount} مستمرة",
                        icon = Icons.Default.PendingActions,
                        isSelected = selectedFilter == DebtFilter.ACTIVE,
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            onFilterSelected(DebtFilter.ACTIVE)
                        }
                    )

                    DrawerFilterItem(
                        title = "المعاملات المسددة",
                        subtitle = "${stats.paidCount} مسددة بالكامل",
                        icon = Icons.Default.CheckCircle,
                        isSelected = selectedFilter == DebtFilter.PAID,
                        tint = PaidEmerald,
                        onClick = {
                            onFilterSelected(DebtFilter.PAID)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 2: تصنيفات المعاملات
                    DrawerSectionTitle(title = "تصنيفات المعاملات")

                    val categories = listOf("شخصي", "عمل", "عائلي", "قرض", "سلفة", "تسوق", "عام")
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                onCategorySelected(if (isSelected) null else cat)
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_cat_$cat"),
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 3: الحساب والأمان
                    DrawerSectionTitle(title = "الحساب والتنبيهات")

                    if (onCheckReminders != null) {
                        DrawerActionItem(
                            title = "فحص تذكيرات استحقاق الديون 🔔",
                            icon = Icons.Default.Notifications,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onCheckReminders,
                            testTag = "drawer_check_reminders"
                        )
                    }

                    if (user?.pinCode != null) {
                        DrawerActionItem(
                            title = "قفل التطبيق برمز PIN",
                            icon = Icons.Default.Lock,
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = onLockApp,
                            testTag = "drawer_lock_app"
                        )
                    }

                    DrawerActionItem(
                        title = "تسجيل الخروج",
                        icon = Icons.Default.ExitToApp,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onLogout,
                        testTag = "drawer_logout"
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader(
    user: User?,
    stats: DebtStats
) {
    val headerGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF0F766E),
            Color(0xFF042F2E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerGradient)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "المستخدم",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column {
                    Text(
                        text = user?.fullName ?: "مستخدم ديوني",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = user?.email ?: "account@duyuni.app",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Info Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "العملة: ${user?.currency ?: "ر.س"}",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "الرصيد: ${if (stats.netBalance >= 0) "+" else ""}${stats.netBalance.toInt()} ${user?.currency ?: "ر.س"}",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerFilterItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("drawer_filter_${title.hashCode()}"),
        shape = RoundedCornerShape(14.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun DrawerActionItem(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
