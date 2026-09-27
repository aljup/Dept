package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtItemCard(
    debt: Debt,
    currency: String,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit,
    totalPaid: Double = 0.0,
    remainingAmount: Double = debt.amount,
    progress: Float = 0f,
    onAddPayment: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val isPaid = debt.isPaid
    val isCreditor = debt.isCreditor
    val hasPayments = totalPaid > 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("debt_item_${debt.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPaid) 0.5.dp else 2.5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Top Row: Type Icon, Name & Category, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon representing Type
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPaid -> PaidEmerald.copy(alpha = 0.15f)
                                isCreditor -> CreditGreen.copy(alpha = 0.15f)
                                else -> DebtRed.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPaid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "مسدد",
                            tint = PaidEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isCreditor) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = if (isCreditor) "مستحق لك" else "مستحق عليك",
                            tint = if (isCreditor) CreditGreen else DebtRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Category
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = debt.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None
                        )

                        // Category Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = debt.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Date and Due Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateFormatter.format(Date(debt.date)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        debt.dueDate?.let { due ->
                            val isOverdue = System.currentTimeMillis() > due && !isPaid
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.HourglassEmpty else Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isOverdue) DebtRed else MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = (if (isOverdue) "متأخر: " else "استحقاق: ") + dateFormatter.format(Date(due)),
                                    fontSize = 10.sp,
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOverdue) DebtRed else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Primary Amount (Original or Remaining)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${numberFormatter.format(if (hasPayments && !isPaid) remainingAmount else debt.amount)} $currency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isPaid -> PaidEmerald
                            isCreditor -> CreditGreen
                            else -> DebtRed
                        },
                        textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = if (isPaid) "مسدد بالكامل" else if (hasPayments) "المتبقي للسداد" else if (isCreditor) "مستحق لك" else "التزام عليك",
                        fontSize = 10.sp,
                        color = when {
                            isPaid -> PaidEmerald
                            isCreditor -> CreditGreen
                            else -> DebtRed
                        }
                    )
                }
            }

            // Note (if exists)
            if (debt.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = debt.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Partial Payments Progress Bar
            if (hasPayments && !isPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تم سداد: ${numberFormatter.format(totalPaid)} $currency (${(progress * 100).toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PaidEmerald
                        )
                        Text(
                            text = "الإجمالي: ${numberFormatter.format(debt.amount)} $currency",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isCreditor) CreditGreen else PaidEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row: Add Payment, Status Toggle, Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Add Payment Button
                if (!isPaid && onAddPayment != null) {
                    FilledTonalButton(
                        onClick = onAddPayment,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("debt_add_payment_${debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCreditor) "تسجيل تحصيل دفعة" else "سداد قسط / دفعة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Status Toggle Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPaid) PaidEmerald.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClick = onToggleStatus)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPaid) "إعادة تنشيط ↺" else "تسوية بالكامل ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPaid) PaidEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Open Details Arrow
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "عرض التفاصيل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
