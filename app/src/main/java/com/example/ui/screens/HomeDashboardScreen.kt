package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.StaffProfileEntity
import com.example.data.local.TransactionEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SubtleOrange
import com.example.ui.theme.SyncedEmerald
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.ReportPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    language: AppLanguage,
    activeStaff: StaffProfileEntity,
    isDemoMode: Boolean,
    selectedPeriod: ReportPeriod,
    filteredTransactions: List<TransactionEntity>,
    visitorCount: Int,
    pendingSyncCount: Int,
    isCloudSignedIn: Boolean,
    lastSyncTimeMillis: Long?,
    onToggleDemoMode: () -> Unit,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onAddVisitor: () -> Unit,
    onCorrectVisitorMinusOne: (String) -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToFreezers: () -> Unit,
    onNavigateToAi: () -> Unit,
    onTriggerSync: () -> Unit
) {
    var showVisitorCorrectionDialog by remember { mutableStateOf(false) }
    var correctionReason by remember { mutableStateOf("") }

    val completedBills = filteredTransactions.filter { it.status == "COMPLETED" }
    val liveRevenue = completedBills.sumOf { it.grandTotal }
    val liveBillsCount = completedBills.size
    val liveBuyingCustomers = completedBills.count { !it.isRepeatCustomerBill }
    val liveProductLines = completedBills.sumOf { it.lineCount }

    // Never invent business data as real data: only show clearly marked demo numbers when Demo Mode is explicitly ON
    val displayRevenue = if (isDemoMode) 18450.0 else liveRevenue
    val displayVisitors = if (isDemoMode) 64 else visitorCount
    val displayBuyingCustomers = if (isDemoMode) 41 else liveBuyingCustomers
    val displayBillsCount = if (isDemoMode) 45 else liveBillsCount
    val displayProductLines = if (isDemoMode) 118 else liveProductLines

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Store Hero Card with Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_store_banner_1794392135774),
                        contentDescription = "New Farm Product Storefront",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        DeepNavyBlue.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = FreshTeal,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ASANSOL • KOLKATA SYNC",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${activeStaff.displayNameEn} (${activeStaff.roleCode})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "NEW FARM PRODUCT",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "50 Years of Trust. A Smarter Way Forward. • ৫০ বছরের বিশ্বাস",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 2. Four Prominent Quick Shortcuts (New Sale, +1 Visitor, Freezer Finder, NEW FARM AI)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (language == AppLanguage.EN_BN) "Quick Counter Shortcuts • দ্রুত শর্টকাট" else "Quick Counter Shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutActionCard(
                        title = "New Sale",
                        subtitle = "নতুন বিল / POS",
                        icon = Icons.Default.PointOfSale,
                        containerColor = DeepNavyBlue,
                        contentColor = Color.White,
                        testTag = "shortcut_new_sale",
                        onClick = onNavigateToBilling,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutActionCard(
                        title = "+1 Visitor",
                        subtitle = "কাস্টমার এন্ট্রি ($displayVisitors)",
                        icon = Icons.Default.PersonAdd,
                        containerColor = FreshTeal,
                        contentColor = Color.White,
                        testTag = "shortcut_add_visitor",
                        onClick = onAddVisitor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutActionCard(
                        title = "7-Freezer Finder",
                        subtitle = "ফ্রিজার ও সেকশন খুঁজুন",
                        icon = Icons.Default.AcUnit,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        testTag = "shortcut_freezer_finder",
                        onClick = onNavigateToFreezers,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutActionCard(
                        title = "NEW FARM AI",
                        subtitle = "ভয়েস অ্যাসিস্ট্যান্ট (Gemini 3.8)",
                        icon = Icons.Default.Mic,
                        containerColor = SubtleOrange,
                        contentColor = Color.White,
                        testTag = "shortcut_new_farm_ai",
                        onClick = onNavigateToAi,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Period Filter + Demo Mode Toggle
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDemoMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDemoMode) "⚠️ DEMO MODE ACTIVE (নমুনা তথ্য)" else "LIVE SHOP DATA (আসল দোকানের হিসাব)",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDemoMode) MaterialTheme.colorScheme.onTertiaryContainer else SyncedEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDemoMode) {
                                    "Showing clearly marked demonstration numbers. Real sales are never mixed with demo data."
                                } else {
                                    "Showing 100% genuine recorded bills and visitor counts."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { onToggleDemoMode() },
                            modifier = Modifier.testTag("demo_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { onSelectPeriod(period) },
                                label = { Text("${period.labelEn} • ${period.labelBn}") },
                                modifier = Modifier.testTag("period_chip_${period.name.lowercase(Locale.US)}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Five Core Metrics Grid (Sales, Visitors, Buying Customers, Completed Bills, Product Lines)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedPeriod.labelEn} Gross Sales • মোট বিক্রি",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", displayRevenue)}",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                modifier = Modifier.testTag("dashboard_revenue_text")
                            )
                            Text(
                                text = "Gross Revenue Only (Profit locked until purchase cost added)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Revenue",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        label = "Visitors (দোকানে আগমন)",
                        value = displayVisitors.toString(),
                        note = "Tap +1 Visitor on entry",
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_visitors_value"
                    )
                    MetricStatCard(
                        label = "Buying Customers (ক্রেতা)",
                        value = displayBuyingCustomers.toString(),
                        note = "Est. unique buyers",
                        icon = Icons.Default.Groups,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_buying_customers_value"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        label = "Completed Bills (মোট বিল)",
                        value = displayBillsCount.toString(),
                        note = "Active valid bills",
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_completed_bills_value"
                    )
                    MetricStatCard(
                        label = "Product Lines (পণ্য লাইন)",
                        value = displayProductLines.toString(),
                        note = "Items sold across bills",
                        icon = Icons.Default.ShoppingBag,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_product_lines_value"
                    )
                }
            }
        }

        // 5. Visitor Counter Quick Bar with Audited Correction
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visitor Counter • কাস্টমার কাউন্টার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val conversion = if (displayVisitors > 0) {
                            ((displayBuyingCustomers.toDouble() / displayVisitors.toDouble()) * 100.0).coerceAtMost(100.0)
                        } else 0.0
                        Text(
                            text = "Conversion: ${String.format(Locale.US, "%.1f", conversion)}% • Avg Bill: ₹${
                                String.format(
                                    Locale.US,
                                    "%.0f",
                                    if (displayBillsCount > 0) displayRevenue / displayBillsCount else 0.0
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showVisitorCorrectionDialog = true },
                            modifier = Modifier.testTag("correct_visitor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = "Correct Visitor",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("-1 Fix")
                        }
                        Button(
                            onClick = onAddVisitor,
                            colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                            modifier = Modifier.testTag("quick_add_visitor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Visitor",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1 Visitor")
                        }
                    }
                }
            }
        }

        // 6. Cloud Sync Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (pendingSyncCount == 0 && isCloudSignedIn) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync Status",
                            tint = if (pendingSyncCount == 0 && isCloudSignedIn) SyncedEmerald else SubtleOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when {
                                    !isCloudSignedIn -> "Offline Local SQLite Mode ($pendingSyncCount queued for Cloud)"
                                    pendingSyncCount == 0 -> "Kolkata Cloud Synchronized"
                                    else -> "$pendingSyncCount Local Record(s) Pending Sync"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val lastSyncStr = lastSyncTimeMillis?.let {
                                SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(it))
                            } ?: "Not synced in this session yet"
                            Text(
                                text = "Last cloud sync: $lastSyncStr",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onTriggerSync,
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Text("Sync Now")
                    }
                }
            }
        }

        // 7. Staff-Wise Sales Breakdown
        item {
            val staffSales = completedBills.groupBy { it.staffNameSnapshot }
                .mapValues { (_, list) -> list.sumOf { it.grandTotal } to list.size }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Staff-Wise Sales • কর্মীদের বিক্রির হিসাব",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (staffSales.isEmpty()) {
                        Text(
                            text = "No completed sales recorded in this period yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        staffSales.forEach { (staffName, pair) ->
                            val (totalAmt, billCnt) = pair
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$staffName ($billCnt bills)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", totalAmt)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavyBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Latest Transactions
        item {
            Text(
                text = "Latest Transactions • সাম্প্রতিক বিলসমূহ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No bills recorded for ${selectedPeriod.labelEn} yet.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'New Sale' above or use NEW FARM AI voice billing to record a transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions.take(8), key = { it.id }) { tx ->
                TransactionRowCard(tx = tx)
            }
        }
    }

    if (showVisitorCorrectionDialog) {
        AlertDialog(
            onDismissRequest = { showVisitorCorrectionDialog = false },
            title = { Text("Correct Visitor Count (-1)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a reason for the audit log (e.g., accidental double tap):")
                    OutlinedTextField(
                        value = correctionReason,
                        onValueChange = { correctionReason = it },
                        label = { Text("Correction Reason") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("visitor_correction_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (correctionReason.isNotBlank()) {
                            onCorrectVisitorMinusOne(correctionReason)
                            correctionReason = ""
                            showVisitorCorrectionDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_visitor_correction_button")
                ) {
                    Text("Save Correction")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVisitorCorrectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShortcutActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    label: String,
    value: String,
    note: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = FreshTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = note,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionEntity,
    onRequestCancel: ((TransactionEntity) -> Unit)? = null
) {
    val timeStr = remember(tx.createdAt) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.createdAt))
    }
    val isCancelled = tx.status == "CANCELLED"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.billNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$timeStr • ${tx.staffNameSnapshot} • ${tx.paymentMethod}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", tx.grandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isCancelled) AlertCrimson else DeepNavyBlue
                    )
                    Text(
                        text = "${tx.status} • ${tx.syncStatus}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCancelled) AlertCrimson else SyncedEmerald
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tx.itemsSummarySnapshot,
                style = MaterialTheme.typography.bodyMedium
            )
            if (isCancelled && !tx.cancellationReason.isNullOrBlank()) {
                Text(
                    text = "Cancelled by ${tx.cancelledByStaffName}: ${tx.cancellationReason}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AlertCrimson,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (!isCancelled && onRequestCancel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onRequestCancel(tx) },
                        modifier = Modifier.testTag("cancel_bill_${tx.id}")
                    ) {
                        Text("Cancel Bill (Audit)", color = AlertCrimson)
                    }
                }
            }
        }
    }
}
