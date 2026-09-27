package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaidEmerald
import com.example.viewmodel.DebtStats
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebtCanvasChart(
    stats: DebtStats,
    currency: String,
    modifier: Modifier = Modifier
) {
    val total = stats.totalOverall
    val activeTotal = stats.totalLent + stats.totalBorrowed
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(stats) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 1
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_canvas_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "التوزيع المالي: مستحقات لك مقابل ديون عليك",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (total <= 0.0) {
                // Empty state placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد معاملات مسجلة حتى الآن لرسم المخطط المالي",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 1. Direct Visual Ratio Bar: Debts Receivable (Lent) vs Debts Owed (Borrowed)
                if (activeTotal > 0.0) {
                    val lentPercent = ((stats.totalLent / activeTotal) * 100).toInt()
                    val borrowedPercent = 100 - lentPercent

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CreditGreen)
                                )
                                Text(
                                    text = "مستحقات لك: $lentPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditGreen
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ديون عليك: $borrowedPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DebtRed
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(DebtRed)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Canvas Ratio Bar
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            val barWidth = size.width
                            val barHeight = size.height
                            val corner = CornerRadius(barHeight / 2, barHeight / 2)

                            // Background container
                            drawRoundRect(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                size = Size(barWidth, barHeight),
                                cornerRadius = corner
                            )

                            val animatedLentRatio = ((stats.totalLent / activeTotal) * progressAnim.value).toFloat()
                            val lentWidth = barWidth * animatedLentRatio

                            // Draw Lent portion (CreditGreen)
                            if (lentWidth > 0f) {
                                drawRoundRect(
                                    color = CreditGreen,
                                    size = Size(lentWidth, barHeight),
                                    cornerRadius = corner
                                )
                            }

                            // Draw Borrowed portion (DebtRed)
                            val borrowedWidth = (barWidth - lentWidth).coerceAtLeast(0f)
                            if (borrowedWidth > 0f && progressAnim.value > 0.05f) {
                                drawRoundRect(
                                    color = DebtRed,
                                    topLeft = Offset(lentWidth, 0f),
                                    size = Size(borrowedWidth, barHeight),
                                    cornerRadius = corner
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. Circular Donut Chart with Centered Net Financial Balance
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    Canvas(modifier = Modifier.size(160.dp)) {
                        val strokeWidthPx = 20.dp.toPx()
                        val arcSize = size.width - strokeWidthPx
                        val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                        // Base track
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidthPx)
                        )

                        val lentAngle = ((stats.totalLent / total) * 360f * progressAnim.value).toFloat()
                        val borrowedAngle = ((stats.totalBorrowed / total) * 360f * progressAnim.value).toFloat()
                        val paidAngle = ((stats.totalPaid / total) * 360f * progressAnim.value).toFloat()

                        var startAngle = -90f

                        // Lent slice (لك - مستحقات)
                        if (lentAngle > 0f) {
                            drawArc(
                                color = CreditGreen,
                                startAngle = startAngle,
                                sweepAngle = lentAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                            startAngle += lentAngle
                        }

                        // Borrowed slice (عليك - التزامات)
                        if (borrowedAngle > 0f) {
                            drawArc(
                                color = DebtRed,
                                startAngle = startAngle,
                                sweepAngle = borrowedAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                            startAngle += borrowedAngle
                        }

                        // Paid slice (مسدد)
                        if (paidAngle > 0f) {
                            drawArc(
                                color = PaidEmerald,
                                startAngle = startAngle,
                                sweepAngle = paidAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inside Label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (stats.netBalance >= 0) "+${numberFormatter.format(stats.netBalance)}" else numberFormatter.format(stats.netBalance),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (stats.netBalance >= 0) CreditGreen else DebtRed
                        )
                        Text(
                            text = "صافي الرصيد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "($currency)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Informative Legend Cards
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartLegendItem(
                        icon = Icons.Default.ArrowDownward,
                        color = CreditGreen,
                        label = "مستحق لك",
                        value = "${numberFormatter.format(stats.totalLent)} $currency",
                        badge = "${stats.lentCount} معاملات"
                    )
                    ChartLegendItem(
                        icon = Icons.Default.ArrowUpward,
                        color = DebtRed,
                        label = "التزام عليك",
                        value = "${numberFormatter.format(stats.totalBorrowed)} $currency",
                        badge = "${stats.borrowedCount} معاملات"
                    )
                    ChartLegendItem(
                        icon = Icons.Default.CheckCircle,
                        color = PaidEmerald,
                        label = "مسدد بالكامل",
                        value = "${numberFormatter.format(stats.totalPaid)} $currency",
                        badge = "${stats.paidCount} مسدد"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    value: String,
    badge: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "($badge)",
                        fontSize = 9.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
