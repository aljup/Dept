package com.example.ui.screens.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.User
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.PersonSummary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    currentUser: User?,
    onNavigateBack: () -> Unit,
    onEditDebt: (Debt) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("المعاملات المالية", "الأشخاص والمتعاملين", "إدارة التصنيفات", "إدارة المستخدمين")

    // Collect snackbars
    LaunchedEffect(Unit) {
        adminViewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة الإدارة والتحكم الشاملة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إدارة المعاملات، التصنيفات، الأشخاص والمستخدمين",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val report = adminViewModel.generateComprehensiveReport()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("التقرير المالي - ديوني", report)
                            clipboard.setPrimaryClip(clip)
                            scope.launch {
                                snackbarHostState.showSnackbar("تم نسخ التقرير المالي الشامل إلى الحافظة بنجاح 📋")
                            }
                        },
                        modifier = Modifier.testTag("copy_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ التقرير المالي الشامل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            HorizontalDivider()

            when (selectedTabIndex) {
                0 -> AdminDebtsTab(
                    adminViewModel = adminViewModel,
                    currency = currentUser?.currency ?: "ر.س",
                    onEditDebt = onEditDebt
                )
                1 -> AdminPersonsTab(
                    adminViewModel = adminViewModel,
                    currency = currentUser?.currency ?: "ر.س"
                )
                2 -> AdminCategoriesTab(adminViewModel = adminViewModel)
                3 -> AdminUsersTab(
                    adminViewModel = adminViewModel,
                    currentUserId = currentUser?.id ?: -1L
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ALL DEBTS & TRANSACTIONS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminDebtsTab(
    adminViewModel: AdminViewModel,
    currency: String,
    onEditDebt: (Debt) -> Unit
) {
    val debts by adminViewModel.filteredDebts.collectAsState()
    val searchQuery by adminViewModel.debtsSearchQuery.collectAsState()
    val statusFilter by adminViewModel.selectedStatusFilter.collectAsState()
    val categories by adminViewModel.categories.collectAsState()
    val selectedCategory by adminViewModel.selectedCategoryFilter.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showBulkSettleDialog by remember { mutableStateOf(false) }

    val numFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = adminViewModel::setDebtsSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_debts_search_input"),
                placeholder = { Text("ابحث في كافة المعاملات بالاسم أو الملاحظة أو المبلغ...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { adminViewModel.setDebtsSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterBadge(
                    title = "الكل",
                    isSelected = statusFilter == null,
                    onClick = { adminViewModel.setStatusFilter(null) }
                )
                FilterBadge(
                    title = "النشطة فقط",
                    isSelected = statusFilter == Debt.STATUS_ACTIVE,
                    onClick = { adminViewModel.setStatusFilter(Debt.STATUS_ACTIVE) }
                )
                FilterBadge(
                    title = "المسددة فقط",
                    isSelected = statusFilter == Debt.STATUS_PAID,
                    onClick = { adminViewModel.setStatusFilter(Debt.STATUS_PAID) }
                )

                // Category chips
                categories.forEach { cat ->
                    FilterBadge(
                        title = cat.name,
                        isSelected = selectedCategory == cat.name,
                        onClick = {
                            adminViewModel.setCategoryFilter(if (selectedCategory == cat.name) null else cat.name)
                        }
                    )
                }
            }

            // Results count & Bulk action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض ${debts.size} معاملة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (debts.isNotEmpty()) {
                    TextButton(
                        onClick = { showBulkSettleDialog = true },
                        modifier = Modifier.testTag("bulk_settle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تسوية المعروض كمسدد ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider()

        if (debts.isEmpty()) {
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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد أي معاملات مطابقة لخيارات البحث",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debts, key = { it.id }) { debt ->
                    val isCreditor = debt.type == Debt.TYPE_CREDITOR
                    val isPaid = debt.status == Debt.STATUS_PAID
                    val typeColor = if (isCreditor) CreditGreen else DebtRed

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_debt_card_${debt.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(typeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCreditor) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = debt.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${if (isCreditor) "لك (مستحق)" else "عليك (التزام)"} • ${debt.category} • ${dateFormatter.format(Date(debt.date))}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${numFormatter.format(debt.amount)} $currency",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPaid) PaidEmerald else typeColor
                                )
                            }

                            if (debt.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = debt.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle status
                                TextButton(onClick = { adminViewModel.toggleDebtStatus(debt) }) {
                                    Icon(
                                        imageVector = if (isPaid) Icons.Default.Clear else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else PaidEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPaid) "إعادة كـ نشط" else "اعتماد السداد",
                                        fontSize = 11.sp,
                                        color = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else PaidEmerald
                                    )
                                }

                                // Edit
                                IconButton(
                                    onClick = { onEditDebt(debt) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { showDeleteConfirmDialog = debt },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete single confirmation dialog
    showDeleteConfirmDialog?.let { debtToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("تأكيد حذف المعاملة") },
            text = { Text("هل أنت متأكد من حذف معاملة (${debtToDelete.name}) بقيمة ${debtToDelete.amount}؟ لن يمكن التراجع.") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteDebt(debtToDelete)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Bulk settle confirmation dialog
    if (showBulkSettleDialog) {
        AlertDialog(
            onDismissRequest = { showBulkSettleDialog = false },
            title = { Text("تأكيد التسوية الجماعية") },
            text = { Text("سيتم تعيين حالة جميع المعاملات المعروضة حالياً (${debts.size} معاملة) كـ 'مسددة بالكامل'. هل ترغب بالاستمرار؟") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.bulkSettleDisplayed()
                        showBulkSettleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaidEmerald)
                ) {
                    Text("اعتماد التسوية للكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkSettleDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 2: PERSONS & CONTACTS DIRECTORY MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminPersonsTab(
    adminViewModel: AdminViewModel,
    currency: String
) {
    val persons by adminViewModel.persons.collectAsState()
    val searchQuery by adminViewModel.personSearchQuery.collectAsState()

    var personToRename by remember { mutableStateOf<PersonSummary?>(null) }
    var renameNewInput by remember { mutableStateOf("") }

    var personToSettle by remember { mutableStateOf<PersonSummary?>(null) }
    var personToDelete by remember { mutableStateOf<PersonSummary?>(null) }

    val numFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = adminViewModel::setPersonSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("admin_persons_search_input"),
            placeholder = { Text("ابحث عن شخص أو جهة متعاملة...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { adminViewModel.setPersonSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (persons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا يوجد أي أشخاص أو جهات مسجلة بعد",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(persons, key = { it.name }) { person ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_person_card_${person.name}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = person.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${person.totalCount} معاملات (${person.activeCount} نشط / ${person.paidCount} مسدد)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Net Balance
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (person.netBalance >= 0) "+" else ""}${numFormatter.format(person.netBalance)} $currency",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (person.netBalance >= 0) CreditGreen else DebtRed
                                    )
                                    Text(
                                        text = if (person.netBalance >= 0) "صافي لك" else "صافي عليك",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Breakdown pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CreditGreen.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "لك عنده: ${numFormatter.format(person.totalLent)} $currency",
                                        fontSize = 11.sp,
                                        color = CreditGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DebtRed.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "له عندك: ${numFormatter.format(person.totalBorrowed)} $currency",
                                        fontSize = 11.sp,
                                        color = DebtRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { personToSettle = person },
                                    enabled = person.activeCount > 0
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PaidEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسوية الحساب بالكامل", fontSize = 11.sp, color = PaidEmerald, fontWeight = FontWeight.Bold)
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            personToRename = person
                                            renameNewInput = person.name
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { personToDelete = person },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الكل", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename dialog
    personToRename?.let { p ->
        AlertDialog(
            onDismissRequest = { personToRename = null },
            title = { Text("تعديل اسم الشخص في كافة المعاملات") },
            text = {
                Column {
                    Text("الاسم الحالي: ${p.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameNewInput,
                        onValueChange = { renameNewInput = it },
                        label = { Text("الاسم الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewInput.isNotBlank()) {
                            adminViewModel.renamePerson(p.name, renameNewInput)
                            personToRename = null
                        }
                    }
                ) {
                    Text("حفظ الاسم الجديد")
                }
            },
            dismissButton = {
                TextButton(onClick = { personToRename = null }) { Text("إلغاء") }
            }
        )
    }

    // Settle confirmation
    personToSettle?.let { p ->
        AlertDialog(
            onDismissRequest = { personToSettle = null },
            title = { Text("تسوية كامل المعاملات") },
            text = { Text("هل أنت متأكد من تسوية واعتماد سداد جميع المعاملات النشطة للشخص (${p.name})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.settleAllDebtsForPerson(p.name)
                        personToSettle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaidEmerald)
                ) {
                    Text("تأكيد التسوية")
                }
            },
            dismissButton = {
                TextButton(onClick = { personToSettle = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete confirmation
    personToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("حذف كافة معاملات الشخص") },
            text = { Text("تحذير: سيتم حذف جميع المعاملات المالية (${p.totalCount} معاملة) المرتبطة بـ (${p.name}). هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteAllDebtsForPerson(p.name)
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: CATEGORIES MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminCategoriesTab(adminViewModel: AdminViewModel) {
    val categories by adminViewModel.categories.collectAsState()
    val allDebts by adminViewModel.allDebts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var editCategoryInput by remember { mutableStateOf("") }

    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Add button
        Button(
            onClick = {
                newCategoryInput = ""
                showAddDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_add_category_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("إضافة تصنيف مالي جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "التصنيفات المتاحة (${categories.size}):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                val countInUse = allDebts.count { it.category == cat.name }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_category_item_${cat.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = cat.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$countInUse معاملات مرتبطة بهذا التصنيف",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    categoryToEdit = cat
                                    editCategoryInput = cat.name
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { categoryToDelete = cat },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة تصنيف جديد") },
            text = {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    placeholder = { Text("مثال: استثمار، إيجار، فواتير...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryInput.isNotBlank()) {
                            adminViewModel.addCategory(newCategoryInput)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit dialog
    categoryToEdit?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("تعديل اسم التصنيف") },
            text = {
                Column {
                    Text("سيتم تحديث كافة المعاملات المسجلة تحت هذا التصنيف تلقائياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCategoryInput,
                        onValueChange = { editCategoryInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryInput.isNotBlank()) {
                            adminViewModel.renameCategory(cat, editCategoryInput)
                            categoryToEdit = null
                        }
                    }
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف التصنيف") },
            text = { Text("هل أنت متأكد من حذف تصنيف (${cat.name})؟ سيتم تلقائياً تحويل المعاملات التابعة له إلى تصنيف 'عام'.") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 4: USERS & ACCOUNTS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminUsersTab(
    adminViewModel: AdminViewModel,
    currentUserId: Long
) {
    val users by adminViewModel.allUsers.collectAsState()
    val allDebts by adminViewModel.allDebts.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userToResetPin by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "المستخدمون المسجلون بالنظام (${users.size}):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users, key = { it.id }) { user ->
                val debtsCount = allDebts.count { it.userId == user.id }
                val isCurrentLoggedIn = user.id == currentUserId

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_card_${user.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (user.isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.fullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (user.isAdmin) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "[مشرف]",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (isCurrentLoggedIn) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "(حسابك الحالي)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = user.email,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "العملة: ${user.currency}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تاريخ التسجيل: ${dateFormatter.format(Date(user.createdAt))} • عدد المعاملات: $debtsCount",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!user.pinCode.isNullOrEmpty()) {
                                TextButton(onClick = { userToResetPin = user }) {
                                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلغاء رمز PIN", fontSize = 11.sp)
                                }
                            }

                            if (!isCurrentLoggedIn) {
                                TextButton(
                                    onClick = { userToDelete = user },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف المستخدم", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset PIN dialog
    userToResetPin?.let { u ->
        AlertDialog(
            onDismissRequest = { userToResetPin = null },
            title = { Text("إعادة تعيين رمز PIN") },
            text = { Text("هل تريد إزالة رمز الـ PIN لحساب (${u.fullName})؟ سيتمكن من تسجيل الدخول بكلمة المرور دون مطالبته برمز PIN.") },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.resetUserPin(u.id)
                    userToResetPin = null
                }) {
                    Text("إزالة الرمز")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToResetPin = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete user dialog
    userToDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("حذف المستخدم نهائياً") },
            text = { Text("تحذير: سيتم حذف المستخدم (${u.fullName}) وكافة المعاملات المالية التابعة له نهائياً. هل أنت متأكد؟") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteUser(u, currentUserId)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun FilterBadge(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
