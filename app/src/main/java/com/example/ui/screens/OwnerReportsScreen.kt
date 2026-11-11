package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.RoleEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemEntity
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SubtleOrange
import com.example.ui.viewmodel.ReportPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OwnerReportsScreen(
    activeRole: RoleEntity,
    selectedPeriod: ReportPeriod,
    filteredTransactions: List<TransactionEntity>,
    allTransactionItems: List<TransactionItemEntity>,
    visitorCount: Int,
    pendingSyncCount: Int,
    lastSyncTimeMillis: Long?,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onTriggerSync: () -> Unit,
    onExportReport: (Boolean, String, Double, Int, Int, Int, Int, List<TransactionEntity>) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler(onBack = onNavigateBackHome)

    val context = LocalContext.current
    val completedBills = filteredTransactions.filter { it.status == "COMPLETED" }
    val cancelledBills = filteredTransactions.filter { it.status == "CANCELLED" }
    val grossRevenue = completedBills.sumOf { it.grandTotal }
    val completedCount = completedBills.size
    val buyingCustomersCount = completedBills.count { !it.isRepeatCustomerBill }
    val productLinesCount = completedBills.sumOf { it.lineCount }
    val avgBillValue = if (completedCount > 0) grossRevenue / completedCount else 0.0
    val conversionRate = if (visitorCount > 0) {
        ((buyingCustomersCount.toDouble() / visitorCount.toDouble()) * 100.0).coerceAtMost(100.0)
    } else 0.0

    val completedTxIds = completedBills.map { it.id }.toSet()
    val periodItems = allTransactionItems.filter { it.transactionId in completedTxIds }
    val productPerformance = periodItems.groupBy { it.productNameSnapshot }
        .mapValues { (_, items) ->
            val totalQty = items.sumOf { it.quantity }
            val unit = items.firstOrNull()?.unitTypeSnapshot ?: ""
            val totalRev = items.sumOf { it.lineTotal }
            Triple(totalQty, unit, totalRev)
        }
        .entries
        .sortedByDescending { it.value.third }

    val staffPerformance = completedBills.groupBy { it.staffNameSnapshot }
        .mapValues { (_, bills) -> bills.size to bills.sumOf { it.grandTotal } }
        .entries
        .sortedByDescending { it.value.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("owner_reports_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Period Selector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Remote Owner Dashboard & Reports • রিপোর্ট",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavyBlue
                    )
                    Text(
                        text = "Real-time Asansol shop monitoring for Kolkata oversight",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { onSelectPeriod(period) },
                                label = { Text(period.labelEn) },
                                modifier = Modifier.testTag("report_tab_${period.name.lowercase(Locale.US)}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Role Permission Check for Confidential Owner Reports
        if (!activeRole.canViewOwnerReports) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confidential Owner & Manager Reports",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Current active role is STAFF. Financial revenue summaries and exports are restricted to OWNER and MANAGER accounts. Switch role in Settings (PIN: 1976 for Owner).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else {
            // 3. Gross Revenue vs Profit Guardrail Notice
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Revenue Notice",
                            tint = SubtleOrange
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GROSS REVENUE ONLY: Values below represent gross sales revenue (মোট বিক্রি), NOT profit. Profit calculations remain disabled until purchase cost and operating expenses are configured.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // 4. Five Separate Executive Metrics + Conversion & Average Bill Value
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "${selectedPeriod.labelEn} Gross Revenue (${selectedPeriod.labelBn})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", grossRevenue)}",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            modifier = Modifier.testTag("reports_gross_revenue_value")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1. Visitors: $visitorCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "2. Buying Customers (Est.): $buyingCustomersCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "3. Completed Bills: $completedCount (Cancelled: ${cancelledBills.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "4. Product Lines: $productLinesCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Visitor-to-Buyer Conversion: ${String.format(Locale.US, "%.1f", conversionRate)}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Avg Bill Value: ₹${String.format(Locale.US, "%.2f", avgBillValue)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "*Note: Buying customer counts in V1 are estimates derived from non-repeat bills since walk-in purchases are anonymous.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // 5. Export CSV / PDF & Cloud Sync Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Export & Cloud Synchronization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val syncText = lastSyncTimeMillis?.let {
                            "Last successful cloud sync: ${SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US).format(Date(it))}"
                        } ?: "Pending sync queue items: $pendingSyncCount"
                        Text(
                            text = syncText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onExportReport(
                                        false,
                                        selectedPeriod.labelEn,
                                        grossRevenue,
                                        visitorCount,
                                        buyingCustomersCount,
                                        completedCount,
                                        productLinesCount,
                                        filteredTransactions
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                            Button(
                                onClick = {
                                    onExportReport(
                                        true,
                                        selectedPeriod.labelEn,
                                        grossRevenue,
                                        visitorCount,
                                        buyingCustomersCount,
                                        completedCount,
                                        productLinesCount,
                                        filteredTransactions
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF")
                            }
                            OutlinedButton(
                                onClick = onTriggerSync,
                                modifier = Modifier.testTag("reports_sync_button")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 6. Product-Wise Performance
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Product-Wise Performance • পণ্যভিত্তিক বিক্রি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (productPerformance.isEmpty()) {
                            Text(
                                text = "No product lines sold in this period yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            productPerformance.forEach { (prodName, triple) ->
                                val (qty, unit, rev) = triple
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$prodName (${String.format(Locale.US, "%.2f", qty)} $unit)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", rev)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepNavyBlue
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // 7. Staff Performance
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Staff Performance • কর্মীদের পারফরম্যান্স",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (staffPerformance.isEmpty()) {
                            Text(
                                text = "No staff sales recorded in this period yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            staffPerformance.forEach { (staffName, pair) ->
                                val (billsCnt, rev) = pair
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$staffName ($billsCnt completed bills)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", rev)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FreshTeal
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
