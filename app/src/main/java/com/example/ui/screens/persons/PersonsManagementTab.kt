package com.example.ui.screens.persons

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Person
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.EditPersonDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.PersonFilter
import com.example.viewmodel.PersonItemState
import com.example.viewmodel.PersonViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PersonsManagementTab(
    personViewModel: PersonViewModel,
    currency: String,
    onViewPersonStatement: (Person) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val persons by personViewModel.personsWithSummary.collectAsState()
    val searchQuery by personViewModel.searchQuery.collectAsState()
    val activeFilter by personViewModel.filter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var personToSettle by remember { mutableStateOf<PersonItemState?>(null) }

    val numFormatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = personViewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("persons_search_input"),
                placeholder = { Text("ابحث بالاسم أو رقم الهاتف أو الملاحظة...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { personViewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Filter Chips + Add Person Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PersonFilter.entries.forEach { f ->
                        FilterChip(
                            selected = activeFilter == f,
                            onClick = { personViewModel.setFilter(f) },
                            label = { Text(f.title, fontSize = 12.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_person_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("شخص جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Results count
            Text(
                text = "دليل المتعاملين: ${persons.size} جهة مسجلة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        if (persons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد نتائج مطابقة في دليل الأشخاص",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "أضف أشخاصاً وجهات تعامل لتنظيم ديونهم وسجل دفعاتهم بشكل منفصل.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة أول شخص الآن")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(persons, key = { it.person.id }) { item ->
                    PersonCard(
                        item = item,
                        currency = currency,
                        onViewStatement = { onViewPersonStatement(item.person) },
                        onEdit = { personToEdit = item.person },
                        onDelete = { personToDelete = item.person },
                        onSettle = { personToSettle = item },
                        onCallPhone = { phone ->
                            if (phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AddPersonDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, notes ->
                personViewModel.addPerson(name, phone, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Dialog
    personToEdit?.let { person ->
        EditPersonDialog(
            person = person,
            onDismiss = { personToEdit = null },
            onConfirm = { newName, newPhone, newNotes ->
                personViewModel.updatePerson(person, newName, newPhone, newNotes) {
                    personToEdit = null
                }
            }
        )
    }

    // Delete Confirmation
    personToDelete?.let { person ->
        var deleteDebtsToo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("حذف (${person.name})") },
            text = {
                Column {
                    Text("هل أنت متأكد من حذف هذا الشخص من دليلك؟")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteDebtsToo = !deleteDebtsToo }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = deleteDebtsToo,
                            onCheckedChange = { deleteDebtsToo = it }
                        )
                        Text(
                            text = "حذف كافة المعاملات والدفعات المرتبطة به أيضاً",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        personViewModel.deletePerson(person, deleteDebtsToo) {
                            personToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Settle Confirmation
    personToSettle?.let { item ->
        AlertDialog(
            onDismissRequest = { personToSettle = null },
            title = { Text("تسوية حساب (${item.person.name})") },
            text = { Text("سيتم تعيين حالة جميع المعاملات النشطة كمسددة بالكامل. هل ترغب بالاستمرار؟") },
            confirmButton = {
                Button(
                    onClick = {
                        personViewModel.settlePersonAccount(item.person.name)
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
}

@Composable
private fun PersonCard(
    item: PersonItemState,
    currency: String,
    onViewStatement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit,
    onCallPhone: (String) -> Unit
) {
    val person = item.person
    val net = item.netBalance
    val isPositive = net > 0.0
    val isNegative = net < 0.0
    val netColor = if (isPositive) CreditGreen else if (isNegative) DebtRed else PaidEmerald

    val numFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewStatement)
            .testTag("person_card_${person.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Avatar + Name + Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (person.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable { onCallPhone(person.phone) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = person.phone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Text(
                                text = "${item.activeDebtsCount} نشط • ${item.paymentsCount} دفعات مسجلة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Net Balance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (net > 0) "+" else ""}${numFormatter.format(net)} $currency",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor
                    )
                    Text(
                        text = if (isPositive) "صافي مستحق لك" else if (isNegative) "صافي دين عليك" else "الحساب متوازن ✓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = netColor
                    )
                }
            }

            // Quick Breakdown Pills
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CreditGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "لك عنده: ${numFormatter.format(item.totalLent)} $currency",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CreditGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DebtRed.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "له عندك: ${numFormatter.format(item.totalBorrowed)} $currency",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DebtRed
                    )
                }

                if (item.paymentsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaidEmerald.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${item.paymentsCount} دفعات ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PaidEmerald
                        )
                    }
                }
            }

            if (person.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = person.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View statement link
                TextButton(
                    onClick = onViewStatement,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("كشف الحساب والعمليات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.activeDebtsCount > 0) {
                        IconButton(onClick = onSettle, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "تسوية الحساب", tint = PaidEmerald, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (person.phone.isNotBlank()) {
                        IconButton(onClick = { onCallPhone(person.phone) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "اتصال", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
