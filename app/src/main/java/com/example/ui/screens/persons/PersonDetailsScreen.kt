package com.example.ui.screens.persons

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Debt
import com.example.data.model.Payment
import com.example.data.model.Person
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.EditPaymentDialog
import com.example.ui.components.EditPersonDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtViewModel
import com.example.viewmodel.PersonViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsScreen(
    person: Person,
    personViewModel: PersonViewModel,
    currency: String,
    onNavigateBack: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onAddDebtForPerson: (String) -> Unit,
    debtViewModel: DebtViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(person) {
        personViewModel.selectPerson(person)
    }

    val statement by personViewModel.selectedPersonStatement.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("المعاملات المالية", "سجل الدفعات")

    var showEditDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<Payment?>(null) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }
    var debtItemToAddPaymentFor by remember { mutableStateOf<com.example.viewmodel.PersonDebtItem?>(null) }

    val numFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

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
                            text = person.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "كشف حساب وملف المتعامل",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("person_details_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Edit Person
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل بيانات الشخص", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Share Statement
                    IconButton(
                        onClick = {
                            val st = statement ?: return@IconButton
                            val report = buildString {
                                appendLine("📄 كشف حساب مالي: ${person.name}")
                                if (person.phone.isNotBlank()) appendLine("رقم الهاتف: ${person.phone}")
                                appendLine("التاريخ: ${dateFormatter.format(Date())}")
                                appendLine("----------------------------------------")
                                appendLine("• إجمالي المستحقات لك: ${numFormatter.format(st.totalLent)} $currency")
                                appendLine("• إجمالي الالتزامات عليك: ${numFormatter.format(st.totalBorrowed)} $currency")
                                appendLine("• صافي الرصيد: ${if (st.netBalance >= 0) "+" else ""}${numFormatter.format(st.netBalance)} $currency")
                                appendLine("• إجمالي الدفعات المسددة: ${numFormatter.format(st.totalPaidReceived)} $currency")
                                appendLine("----------------------------------------")
                                appendLine("المعاملات المالية (${st.debts.size}):")
                                st.debts.forEachIndexed { i, d ->
                                    val type = if (d.type == Debt.TYPE_CREDITOR) "لك" else "عليك"
                                    val status = if (d.status == Debt.STATUS_PAID) "مسدد ✓" else "نشط"
                                    appendLine("${i + 1}. [$type] ${numFormatter.format(d.amount)} $currency - $status (${dateFormatter.format(Date(d.date))})")
                                }
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, report)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة كشف الحساب"))
                        },
                        modifier = Modifier.testTag("share_person_statement")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة كشف الحساب", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile & Financial Summary Header Card
            statement?.let { st ->
                val net = st.netBalance
                val isPositive = net > 0.0
                val isNegative = net < 0.0
                val netColor = if (isPositive) CreditGreen else if (isNegative) DebtRed else PaidEmerald

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(netColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.name.firstOrNull()?.toString() ?: "👤",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = netColor
                                    )
                                }
                                Column {
                                    Text(
                                        text = person.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (person.phone.isNotBlank()) {
                                        Text(
                                            text = person.phone,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }

                            if (person.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("اتصال", fontSize = 11.sp)
                                }
                            }
                        }

                        if (person.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ملاحظات: ${person.notes}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("صافي الرصيد الحالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${if (net > 0) "+" else ""}${numFormatter.format(net)} $currency",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = netColor
                                )
                                Text(
                                    text = if (isPositive) "مستحق لك" else if (isNegative) "دين عليك" else "حساب مسدد بالكامل ✓",
                                    fontSize = 10.sp,
                                    color = netColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "لك عنده: ${numFormatter.format(st.totalLent)} $currency",
                                    fontSize = 12.sp,
                                    color = CreditGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "له عندك: ${numFormatter.format(st.totalBorrowed)} $currency",
                                    fontSize = 12.sp,
                                    color = DebtRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إجمالي السداد: ${numFormatter.format(st.totalPaidReceived)} $currency",
                                    fontSize = 11.sp,
                                    color = PaidEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast Add Debt for Person
                        Button(
                            onClick = { onAddDebtForPerson(person.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل دين أو معاملة جديدة لهذا الشخص", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = "$title (${if (index == 0) statement?.debts?.size ?: 0 else statement?.payments?.size ?: 0})",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Content List
            when (selectedTab) {
                0 -> {
                    // Debts List with Real-time Remaining Balances and Payments
                    val debtsList = statement?.debtsWithDetails ?: emptyList()
                    if (debtsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد معاملات مسجلة لهذا الشخص", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(debtsList, key = { it.debt.id }) { item ->
                                val debt = item.debt
                                val isCreditor = debt.type == Debt.TYPE_CREDITOR
                                val isPaid = debt.status == Debt.STATUS_PAID || item.remainingAmount <= 0.0
                                val typeColor = if (isCreditor) CreditGreen else DebtRed

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onDebtClick(debt.id) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(typeColor.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCreditor) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                        contentDescription = null,
                                                        tint = typeColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = if (isCreditor) "مستحق لك (إقراض)" else "التزام عليك (اقتراض)",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${dateFormatter.format(Date(debt.date))} • ${debt.category}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (debt.notes.isNotBlank()) {
                                                        Text(
                                                            text = debt.notes,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }

                                            // Amounts Column
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "المتبقي: ${numFormatter.format(item.remainingAmount)} $currency",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPaid) PaidEmerald else typeColor
                                                )
                                                Text(
                                                    text = if (isPaid) "مسدد بالكامل ✓" else "الأصلي: ${numFormatter.format(debt.amount)} $currency",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (item.totalPaid > 0.0 && !isPaid) {
                                                    Text(
                                                        text = "سُدد منه: ${numFormatter.format(item.totalPaid)} $currency",
                                                        fontSize = 10.sp,
                                                        color = PaidEmerald,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Progress Bar if partially paid or active
                                        if (item.totalPaid > 0.0 && !isPaid) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = { item.progress },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = PaidEmerald,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                Text(
                                                    text = "${(item.progress * 100).toInt()}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PaidEmerald
                                                )
                                            }
                                        }

                                        // Action button: + دفعة
                                        if (!isPaid) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                FilledTonalButton(
                                                    onClick = { debtItemToAddPaymentFor = item },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("تسجيل دفعة سداد (${numFormatter.format(item.remainingAmount)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Payments List
                    val paymentsList = statement?.payments ?: emptyList()
                    if (paymentsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد دفعات أو أقساط مسجلة لهذا الشخص حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(paymentsList, key = { it.id }) { payment ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(PaidEmerald.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payment,
                                                    contentDescription = null,
                                                    tint = PaidEmerald,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = "دفعة مالية (${payment.method})",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = dateFormatter.format(Date(payment.date)),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (payment.notes.isNotBlank()) {
                                                    Text(
                                                        text = payment.notes,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${numFormatter.format(payment.amount)} $currency",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaidEmerald
                                            )

                                            IconButton(
                                                onClick = { paymentToEdit = payment },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل الدفعة",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { paymentToDelete = payment },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الدفعة",
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
            }
        }
    }

    // Add Payment Dialog for selected debt
    debtItemToAddPaymentFor?.let { item ->
        val d = item.debt
        AddPaymentDialog(
            debtName = d.name,
            remainingAmount = item.remainingAmount,
            currency = currency,
            onDismiss = { debtItemToAddPaymentFor = null },
            onConfirm = { amount, method, notes ->
                debtViewModel.addPayment(
                    debtId = d.id,
                    personName = d.name,
                    amount = amount,
                    method = method,
                    notes = notes
                ) {
                    debtItemToAddPaymentFor = null
                }
            }
        )
    }

    // Edit Payment Dialog
    paymentToEdit?.let { p ->
        EditPaymentDialog(
            payment = p,
            currency = currency,
            onDismiss = { paymentToEdit = null },
            onConfirm = { newAmount, newMethod, newNotes ->
                debtViewModel.updatePayment(p, newAmount, newMethod, newNotes, p.date) {
                    paymentToEdit = null
                }
            }
        )
    }

    // Delete Payment Confirmation Dialog
    paymentToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("تأكيد حذف الدفعة") },
            text = { Text("هل أنت متأكد من حذف هذه الدفعة بقيمة ${numFormatter.format(p.amount)} $currency؟ سيتم تحديث الرصيد تلقائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        debtViewModel.deletePayment(p) {
                            paymentToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    if (showEditDialog) {
        EditPersonDialog(
            person = person,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName, newPhone, newNotes ->
                personViewModel.updatePerson(person, newName, newPhone, newNotes) {
                    showEditDialog = false
                }
            }
        )
    }
}
