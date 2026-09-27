package com.example.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Debt
import com.example.data.model.DebtWithPayments
import com.example.data.model.Person
import com.example.data.model.User
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.AppTopBar
import com.example.ui.components.DebtCanvasChart
import com.example.ui.components.DebtItemCard
import com.example.ui.components.DebtSummaryCards
import com.example.ui.screens.ledger.TransactionsLedgerTab
import com.example.ui.screens.payments.PaymentsLogTab
import com.example.ui.screens.persons.PersonsManagementTab
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtFilter
import com.example.viewmodel.DebtViewModel
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainDashboardTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("الرئيسية", Icons.Default.Dashboard, "nav_tab_home"),
    PERSONS("الأشخاص", Icons.Default.People, "nav_tab_persons"),
    DEBTS("الديون", Icons.Default.ReceiptLong, "nav_tab_debts"),
    LEDGER("السجلات", Icons.Default.Payments, "nav_tab_ledger")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User?,
    debtViewModel: DebtViewModel,
    personViewModel: PersonViewModel,
    onAddDebtClick: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onViewPersonStatement: (Person) -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    onLockApp: () -> Unit,
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainDashboardTab.HOME) }
    var ledgerSubTab by remember { mutableIntStateOf(0) } // 0: Payments, 1: Full Ledger

    val debts by debtViewModel.debts.collectAsState()
    val debtsWithPayments by debtViewModel.debtsWithPayments.collectAsState()
    val stats by debtViewModel.stats.collectAsState()
    val activeFilter by debtViewModel.filter.collectAsState()
    val searchQuery by debtViewModel.searchQuery.collectAsState()
    val selectedCategory by debtViewModel.selectedCategory.collectAsState()
    val allPayments by debtViewModel.allPayments.collectAsState()
    val personsWithSummary by personViewModel.personsWithSummary.collectAsState()

    val currency = user?.currency ?: debtViewModel.getCurrency()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog state for Recording a Payment directly
    var targetDebtForPayment by remember { mutableStateOf<DebtWithPayments?>(null) }
    // Dialog state for Adding a Person directly
    var showAddPersonDialog by remember { mutableStateOf(false) }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Handled gracefully */ }

        LaunchedEffect(Unit) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
            }
        }
    }

    // Sync current user ID with ViewModels
    LaunchedEffect(user?.id) {
        user?.id?.let {
            debtViewModel.refreshUser(it)
            personViewModel.refreshUser(it)
        }
    }

    val categories = listOf("الكل", "شخصي", "عمل", "عائلي", "قرض", "سلفة", "تسوق", "عام")

    val handleCheckReminders = {
        debtViewModel.triggerDueDebtsReminderCheck()
    }

    // Identify urgent upcoming / overdue debts for alert banner
    val urgentDebts = remember(debtsWithPayments) {
        val now = System.currentTimeMillis()
        val threeDaysLater = now + (3 * 24 * 60 * 60 * 1000L)
        debtsWithPayments.filter {
            !it.debt.isPaid && it.debt.dueDate != null && it.debt.dueDate <= threeDaysLater
        }.sortedBy { it.debt.dueDate }
    }

    AppNavigationDrawer(
        drawerState = drawerState,
        user = user,
        stats = stats,
        selectedFilter = activeFilter,
        selectedCategory = selectedCategory,
        onFilterSelected = { filter ->
            debtViewModel.setFilter(filter)
            currentTab = MainDashboardTab.DEBTS
            scope.launch { drawerState.close() }
        },
        onCategorySelected = { cat ->
            debtViewModel.selectCategory(cat)
            currentTab = MainDashboardTab.DEBTS
            scope.launch { drawerState.close() }
        },
        onOpenAdmin = onOpenAdmin,
        onCheckReminders = {
            scope.launch { drawerState.close() }
            handleCheckReminders()
        },
        onLockApp = {
            scope.launch { drawerState.close() }
            onLockApp()
        },
        onLogout = {
            scope.launch { drawerState.close() }
            onLogout()
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    user = user,
                    onOpenDrawer = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onOpenAdmin = onOpenAdmin,
                    onCheckReminders = handleCheckReminders,
                    onLockApp = onLockApp,
                    onLogout = onLogout
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    MainDashboardTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val badgeCount = when (tab) {
                            MainDashboardTab.PERSONS -> personsWithSummary.size
                            MainDashboardTab.DEBTS -> stats.activeCount
                            MainDashboardTab.LEDGER -> allPayments.size
                            else -> 0
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                if (badgeCount > 0 && tab != MainDashboardTab.HOME) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Text(text = "$badgeCount", fontSize = 10.sp)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            },
            floatingActionButton = {
                when (currentTab) {
                    MainDashboardTab.HOME, MainDashboardTab.DEBTS -> {
                        FloatingActionButton(
                            onClick = onAddDebtClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("add_debt_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة دين جديد",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    MainDashboardTab.PERSONS -> {
                        FloatingActionButton(
                            onClick = { showAddPersonDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("add_person_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "إضافة شخص جديد",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    MainDashboardTab.LEDGER -> {
                        // In ledger, can quickly record a debt or switch to add payment
                        FloatingActionButton(
                            onClick = onAddDebtClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("ledger_add_debt_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "تسجيل معاملة جديدة",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    // =========================================================
                    // 1. TAB: الرئيسية (Dashboard & Financial Overview)
                    // =========================================================
                    MainDashboardTab.HOME -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Admin Quick Access Banner if user is Admin
                            if (user?.isAdmin == true && onOpenAdmin != null) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = onOpenAdmin)
                                            .testTag("admin_banner_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FilterList,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "لوحة تحكم وإدارة المشرف",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "إدارة المعاملات، الأشخاص، التصنيفات والمستخدمين",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = onOpenAdmin,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("فتح اللوحة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Urgent Debts Due Date Alert Banner
                            if (urgentDebts.isNotEmpty()) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = DebtRed.copy(alpha = 0.10f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HourglassEmpty,
                                                    contentDescription = null,
                                                    tint = DebtRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "تنبيه استحقاق الديون (${urgentDebts.size})",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DebtRed
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val firstUrgent = urgentDebts.first()
                                            val isPast = System.currentTimeMillis() > (firstUrgent.debt.dueDate ?: 0L)
                                            Text(
                                                text = "${firstUrgent.debt.name} بمبلغ ${numberFormatter.format(firstUrgent.remainingAmount)} $currency ${if (isPast) "متأخر عن السداد!" else "يستحق قريباً"} (${dateFormatter.format(Date(firstUrgent.debt.dueDate!!))})",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { targetDebtForPayment = firstUrgent },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("تسجيل سداد الآن 💵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = { onDebtClick(firstUrgent.debt.id) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("تفاصيل الدين", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Summary Cards (Net Balance, Receivables, Payables, Paid)
                            item {
                                DebtSummaryCards(
                                    stats = stats,
                                    currency = currency
                                )
                            }

                            // Canvas Financial Distribution Chart
                            item {
                                DebtCanvasChart(
                                    stats = stats,
                                    currency = currency
                                )
                            }

                            // Quick Action Shortcuts Grid (دين جديد، شخص جديد، سجل الدفعات، سجل العمليات)
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "الإجراءات السريعة",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Shortcut: New Debt
                                            QuickActionItem(
                                                title = "دين جديد",
                                                icon = Icons.Default.Add,
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                contentColor = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f),
                                                onClick = onAddDebtClick
                                            )
                                            // Shortcut: New Person
                                            QuickActionItem(
                                                title = "شخص جديد",
                                                icon = Icons.Default.PersonAdd,
                                                containerColor = CreditGreen.copy(alpha = 0.15f),
                                                contentColor = CreditGreen,
                                                modifier = Modifier.weight(1f),
                                                onClick = { showAddPersonDialog = true }
                                            )
                                            // Shortcut: Payments Log
                                            QuickActionItem(
                                                title = "سجل الدفعات",
                                                icon = Icons.Default.Payments,
                                                containerColor = PaidEmerald.copy(alpha = 0.15f),
                                                contentColor = PaidEmerald,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    currentTab = MainDashboardTab.LEDGER
                                                    ledgerSubTab = 0
                                                }
                                            )
                                            // Shortcut: All Debts
                                            QuickActionItem(
                                                title = "إدارة الديون",
                                                icon = Icons.Default.ReceiptLong,
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                contentColor = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.weight(1f),
                                                onClick = { currentTab = MainDashboardTab.DEBTS }
                                            )
                                        }
                                    }
                                }
                            }

                            // Recent Debts Preview Section Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "آخر المعاملات المسجلة",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    TextButton(onClick = { currentTab = MainDashboardTab.DEBTS }) {
                                        Text("عرض كافة الديون (${debts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Recent 4 Debts with quick "+ دفعة" button
                            val recentDebts = debtsWithPayments.take(4)
                            if (recentDebts.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد معاملات مسجلة بعد. ابدأ بإضافة دين جديد!",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                items(recentDebts, key = { it.debt.id }) { itemWithPayments ->
                                    DebtItemCard(
                                        debt = itemWithPayments.debt,
                                        currency = currency,
                                        totalPaid = itemWithPayments.totalPaid,
                                        remainingAmount = itemWithPayments.remainingAmount,
                                        progress = itemWithPayments.progress,
                                        onClick = { onDebtClick(itemWithPayments.debt.id) },
                                        onToggleStatus = { debtViewModel.toggleDebtStatus(itemWithPayments.debt) },
                                        onAddPayment = { targetDebtForPayment = itemWithPayments }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(60.dp)) }
                        }
                    }

                    // =========================================================
                    // 2. TAB: إدارة الأشخاص (Persons Directory & Statements)
                    // =========================================================
                    MainDashboardTab.PERSONS -> {
                        PersonsManagementTab(
                            personViewModel = personViewModel,
                            currency = currency,
                            onViewPersonStatement = onViewPersonStatement
                        )
                    }

                    // =========================================================
                    // 3. TAB: إدارة الديون (Dedicated Debts Workspace)
                    // =========================================================
                    MainDashboardTab.DEBTS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Search and Filters Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Search Input
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = debtViewModel::setSearchQuery,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("debt_search_input"),
                                    placeholder = { Text("ابحث في الديون بالاسم أو الملاحظة أو التصنيف...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { debtViewModel.setSearchQuery("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "مسح البحث"
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Main Filter Tabs (الكل، لك، عليك، النشطة، المسددة)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DebtFilter.entries.forEach { filter ->
                                        val isSelected = activeFilter == filter
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { debtViewModel.setFilter(filter) }
                                                .testTag("filter_chip_${filter.name}"),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = filter.title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }

                                // Category Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val isCatSelected = (selectedCategory == null && cat == "الكل") || (selectedCategory == cat)
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable {
                                                    debtViewModel.selectCategory(if (cat == "الكل") null else cat)
                                                },
                                            color = if (isCatSelected) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCatSelected) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            // Debts List
                            if (debtsWithPayments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "لا توجد معاملات مطابقة",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "جرب تغيير معايير البحث أو الفلترة أو أضف معاملة جديدة",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(debtsWithPayments, key = { it.debt.id }) { itemWithPayments ->
                                        DebtItemCard(
                                            debt = itemWithPayments.debt,
                                            currency = currency,
                                            totalPaid = itemWithPayments.totalPaid,
                                            remainingAmount = itemWithPayments.remainingAmount,
                                            progress = itemWithPayments.progress,
                                            onClick = { onDebtClick(itemWithPayments.debt.id) },
                                            onToggleStatus = { debtViewModel.toggleDebtStatus(itemWithPayments.debt) },
                                            onAddPayment = { targetDebtForPayment = itemWithPayments }
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(60.dp)) }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // 4. TAB: السجلات والدفعات (Ledger & Payments Hub)
                    // =========================================================
                    MainDashboardTab.LEDGER -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sub-tabs for Payments Log vs Transactions Ledger
                            PrimaryTabRow(
                                selectedTabIndex = ledgerSubTab,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Tab(
                                    selected = ledgerSubTab == 0,
                                    onClick = { ledgerSubTab = 0 },
                                    text = {
                                        Text(
                                            text = "سجل الدفعات (${allPayments.size})",
                                            fontSize = 13.sp,
                                            fontWeight = if (ledgerSubTab == 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                Tab(
                                    selected = ledgerSubTab == 1,
                                    onClick = { ledgerSubTab = 1 },
                                    text = {
                                        Text(
                                            text = "سجل العمليات الشامل",
                                            fontSize = 13.sp,
                                            fontWeight = if (ledgerSubTab == 1) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }

                            when (ledgerSubTab) {
                                0 -> {
                                    PaymentsLogTab(
                                        debtViewModel = debtViewModel,
                                        currency = currency
                                    )
                                }

                                1 -> {
                                    TransactionsLedgerTab(
                                        debtViewModel = debtViewModel,
                                        currency = currency,
                                        onDebtClick = onDebtClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Payment Dialog (when clicking "+ دفعة" anywhere)
    targetDebtForPayment?.let { dwp ->
        AddPaymentDialog(
            debtName = dwp.debt.name,
            remainingAmount = dwp.remainingAmount,
            currency = currency,
            onDismiss = { targetDebtForPayment = null },
            onConfirm = { amount, method, notes ->
                debtViewModel.addPayment(
                    debtId = dwp.debt.id,
                    personName = dwp.debt.name,
                    amount = amount,
                    method = method,
                    notes = notes
                ) {
                    targetDebtForPayment = null
                }
            }
        )
    }

    // Add Person Dialog
    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { name, phone, notes ->
                personViewModel.addPerson(name, phone, notes) {
                    showAddPersonDialog = false
                }
            }
        )
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
