package com.example.ui.screens.payments

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DebtWithPayments
import com.example.data.model.Payment
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.EditPaymentDialog
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentsLogTab(
    debtViewModel: DebtViewModel,
    currency: String,
    modifier: Modifier = Modifier
) {
    val payments by debtViewModel.filteredPayments.collectAsState()
    val allPayments by debtViewModel.allPayments.collectAsState()
    val debtsWithPayments by debtViewModel.debtsWithPayments.collectAsState()
    val searchQuery by debtViewModel.paymentsSearchQuery.collectAsState()
    val selectedMethod by debtViewModel.paymentMethodFilter.collectAsState()

    var paymentToEdit by remember { mutableStateOf<Payment?>(null) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }
    var showDebtSelectorDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtWithPayments?>(null) }

    val numFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val totalPaidSum = allPayments.sumOf { it.amount }

    Column(modifier = modifier.fillMaxSize()) {
        // Summary Header Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PaidEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = PaidEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إجمالي الدفعات المسددة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${numFormatter.format(totalPaidSum)} $currency",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaidEmerald
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${allPayments.size} دفعة مسجلة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Button(
                        onClick = { showDebtSelectorDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_payment_from_log_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسجيل دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search & Method Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = debtViewModel::setPaymentsSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payments_search_input"),
                placeholder = { Text("ابحث في الدفعات بالاسم أو الملاحظة أو المبلغ...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { debtViewModel.setPaymentsSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Method Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedMethod == null,
                    onClick = { debtViewModel.setPaymentMethodFilter(null) },
                    label = { Text("كافة الطرق (${allPayments.size})", fontSize = 12.sp) },
                    shape = RoundedCornerShape(20.dp)
                )

                Payment.ALL_METHODS.forEach { method ->
                    val count = allPayments.count { it.method == method }
                    FilterChip(
                        selected = selectedMethod == method,
                        onClick = {
                            debtViewModel.setPaymentMethodFilter(if (selectedMethod == method) null else method)
                        },
                        label = { Text("$method ($count)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد دفعات مطابقة في السجل",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يمكنك تسجيل دفعات مالية وأقساط من خلال صفحة تفاصيل أي دين.",
                        fontSize = 12.sp,
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
                items(payments, key = { it.id }) { payment ->
                    PaymentLogCard(
                        payment = payment,
                        currency = currency,
                        onEdit = { paymentToEdit = payment },
                        onDelete = { paymentToDelete = payment }
                    )
                }
            }
        }
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
            text = { Text("هل أنت متأكد من حذف هذه الدفعة بقيمة ${numFormatter.format(p.amount)} $currency؟ سيتم إعادة حساب الرصيد المتبقي للدين تلقائياً.") },
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

    // Debt Selector Dialog for Adding a Payment
    if (showDebtSelectorDialog) {
        val activeDebts = debtsWithPayments.filter { !it.debt.isPaid }
        AlertDialog(
            onDismissRequest = { showDebtSelectorDialog = false },
            title = {
                Text(
                    text = "اختر المعاملة لتسجيل الدفعة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (activeDebts.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد ديون نشطة حالياً لتسجيل دفعات عليها.", fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeDebts, key = { it.debt.id }) { dwp ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDebtForPayment = dwp
                                        showDebtSelectorDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dwp.debt.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dwp.debt.category} • ${if (dwp.debt.isCreditor) "مستحق لك" else "التزام عليك"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${numFormatter.format(dwp.remainingAmount)} $currency",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "المتبقي",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDebtSelectorDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Add Payment Dialog for selected debt
    selectedDebtForPayment?.let { dwp ->
        AddPaymentDialog(
            debtName = dwp.debt.name,
            remainingAmount = dwp.remainingAmount,
            currency = currency,
            onDismiss = { selectedDebtForPayment = null },
            onConfirm = { amount, method, notes ->
                debtViewModel.addPayment(
                    debtId = dwp.debt.id,
                    personName = dwp.debt.name,
                    amount = amount,
                    method = method,
                    notes = notes
                ) {
                    selectedDebtForPayment = null
                }
            }
        )
    }
}

@Composable
fun PaymentLogCard(
    payment: Payment,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val numFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
    }
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_log_card_${payment.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaidEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = PaidEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = payment.personName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dateFormatter.format(Date(payment.date))} • ${payment.method}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${numFormatter.format(payment.amount)} $currency",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PaidEmerald
                )
            }

            if (payment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظة: ${payment.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل الدفعة", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
