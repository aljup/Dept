package com.example.ui.screens.debt

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.DebtWithPayments
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.EditPaymentDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailsScreen(
    debtId: Long,
    debtViewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Debt) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    val debtFlow = remember(debtId) { debtViewModel.getDebtFlow(debtId) }
    val debt by debtFlow.collectAsState(initial = null)

    val debtWithPaymentsFlow = remember(debtId) { debtViewModel.getDebtWithPaymentsFlow(debtId) }
    val debtWithPayments by debtWithPaymentsFlow.collectAsState(initial = null)

    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<Payment?>(null) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل المعاملة", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("debt_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    debt?.let { currentDebt ->
                        IconButton(
                            onClick = {
                                val typeDesc = if (currentDebt.isCreditor) "مستحق لك بقيمة" else "مستحق عليك بقيمة"
                                val shareText = "تذكير بخصوص المعاملة المالية:\n" +
                                        "الطرف: ${currentDebt.name}\n" +
                                        "النوع: $typeDesc ${numberFormatter.format(currentDebt.amount)} $currency\n" +
                                        "التاريخ: ${dateFormatter.format(Date(currentDebt.date))}\n" +
                                        (currentDebt.dueDate?.let { "تاريخ الاستحقاق: ${dateFormatter.format(Date(it))}\n" } ?: "") +
                                        "الحالة: ${if (currentDebt.isPaid) "مسدد ✓" else "نشط"}\n" +
                                        if (currentDebt.notes.isNotBlank()) "ملاحظات: ${currentDebt.notes}" else ""

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفاصيل المعاملة"))
                            },
                            modifier = Modifier.testTag("share_debt_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة")
                        }

                        IconButton(
                            onClick = { onNavigateToEdit(currentDebt) },
                            modifier = Modifier.testTag("edit_debt_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل")
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_debt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (debt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "جاري تحميل تفاصيل المعاملة...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val currentDebt = debt!!
            val isPaid = currentDebt.isPaid
            val isCreditor = currentDebt.isCreditor

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Hero Amount Card
                val heroGradient = when {
                    isPaid -> Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF064E3B)))
                    isCreditor -> Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF134E4A)))
                    else -> Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroGradient)
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isPaid) "تم السداد بالكامل ✓"
                                    else if (isCreditor) "مستحق لك (أقرضته)"
                                    else "مستحق عليك (اقترضت منه)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Person / Entity Name
                            Text(
                                text = currentDebt.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Amount Display
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = numberFormatter.format(currentDebt.amount),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = currency,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Details Grid Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DetailItemRow(
                            icon = Icons.Default.CalendarMonth,
                            iconColor = MaterialTheme.colorScheme.primary,
                            title = "تاريخ المعاملة",
                            value = dateFormatter.format(Date(currentDebt.date))
                        )

                        currentDebt.dueDate?.let { due ->
                            val now = System.currentTimeMillis()
                            val diffDays = TimeUnit.MILLISECONDS.toDays(due - now)
                            val overdue = now > due && !isPaid

                            val statusText = when {
                                isPaid -> "تم سداده"
                                overdue -> "متأخر منذ ${-diffDays} يوم!"
                                diffDays == 0L -> "يستحق اليوم!"
                                else -> "متبقي $diffDays يوم"
                            }

                            DetailItemRow(
                                icon = Icons.Default.HourglassEmpty,
                                iconColor = if (overdue) DebtRed else MaterialTheme.colorScheme.tertiary,
                                title = "تاريخ الاستحقاق",
                                value = "${dateFormatter.format(Date(due))} ($statusText)"
                            )
                        }

                        DetailItemRow(
                            icon = Icons.Default.Category,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            title = "التصنيف",
                            value = currentDebt.category
                        )

                        if (currentDebt.notes.isNotBlank()) {
                            DetailItemRow(
                                icon = Icons.Default.Notes,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                title = "ملاحظات وتفاصيل",
                                value = currentDebt.notes
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Action Button: Toggle Paid / Active
                Button(
                    onClick = {
                        debtViewModel.toggleDebtStatus(currentDebt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("toggle_status_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaid) MaterialTheme.colorScheme.primary else PaidEmerald
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.Replay else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = if (isPaid) "إعادة فتح الدين (كنشط)" else "تحديد الدين كمسدد بالكامل",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("delete_debt_outlined_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Text(
                            text = "حذف المعاملة نهائياً",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Confirmation Dialog for Delete
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(text = "تأكيد الحذف", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("هل أنت متأكد من رغبتك في حذف معاملة \"${currentDebt.name}\" بمبلغ ${numberFormatter.format(currentDebt.amount)} $currency؟ لا يمكن التراجع عن هذا الإجراء.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                debtViewModel.deleteDebt(currentDebt) {
                                    onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("نعم، احذف", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
