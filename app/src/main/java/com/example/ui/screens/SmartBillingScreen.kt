package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.ProductVariantEntity
import com.example.data.local.RoleEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.DraftBillItem
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SubtleOrange
import com.example.ui.theme.SyncedEmerald
import java.util.Locale

@Composable
fun SmartBillingScreen(
    products: List<ProductEntity>,
    variants: List<ProductVariantEntity>,
    draftCart: List<DraftBillItem>,
    recentTransactions: List<TransactionEntity>,
    selectedPaymentMethod: String,
    isRepeatCustomer: Boolean,
    activeRole: RoleEntity,
    onAddDraftItem: (ProductEntity, ProductVariantEntity?, Double, Double) -> Unit,
    onRemoveDraftItem: (String) -> Unit,
    onClearDraftCart: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onToggleRepeatCustomer: (Boolean) -> Unit,
    onSaveBill: () -> Unit,
    onCancelCompletedBill: (TransactionEntity, String) -> Unit,
    onUpdateProductRate: (ProductEntity, Double) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler(onBack = onNavigateBackHome)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedProductForEntry by remember { mutableStateOf<ProductEntity?>(null) }
    var showBillHistoryModal by remember { mutableStateOf(false) }
    var billToCancel by remember { mutableStateOf<TransactionEntity?>(null) }
    var cancelReasonInput by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesCat = selectedCategory == "ALL" || p.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                p.nameEn.contains(searchQuery, ignoreCase = true) ||
                p.nameBn.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery && p.isActive
        }
    }

    val grandTotal = draftCart.sumOf { it.lineTotal }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("billing_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Search & History Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product (e.g. Salmon, Vetki, Potato)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("billing_product_search_input")
                )
                OutlinedButton(
                    onClick = { showBillHistoryModal = true },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("billing_history_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "Bill History")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bills (${recentTransactions.size})")
                }
            }
        }

        // 2. Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val categories = listOf(
                    "ALL" to "All Items (সব পণ্য)",
                    "FROZEN_FISH" to "Fish & Frozen (মাছ ও ফ্রোজেন)",
                    "STAPLES" to "Daily Staples (চাল-ডাল-আলু)",
                    "ICE_CREAM" to "Ice Cream (আইসক্রিম)"
                )
                items(categories) { (code, label) ->
                    FilterChip(
                        selected = selectedCategory == code,
                        onClick = { selectedCategory = code },
                        label = { Text(label) },
                        modifier = Modifier.testTag("cat_chip_${code.lowercase(Locale.US)}")
                    )
                }
            }
        }

        // 3. Active Bill Cart (Always visible at top when items are added)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Bill Cart • বর্তমান বিল (${draftCart.size} lines)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyBlue
                        )
                        if (draftCart.isNotEmpty()) {
                            TextButton(
                                onClick = onClearDraftCart,
                                modifier = Modifier.testTag("cancel_draft_bill_button")
                            ) {
                                Text("Clear / Cancel Draft", color = AlertCrimson)
                            }
                        }
                    }

                    if (draftCart.isEmpty()) {
                        Text(
                            text = "Tap any product below to add quantity. Supports decimal weights (e.g. 0.25 kg, 1.5 kg) and small sales with no minimum threshold.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        draftCart.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayNameEn,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val qtyFormatted = if (item.quantity % 1.0 == 0.0) {
                                        item.quantity.toInt().toString()
                                    } else {
                                        String.format(Locale.US, "%.3f", item.quantity)
                                    }
                                    Text(
                                        text = "$qtyFormatted ${item.product.unitType} × ₹${String.format(Locale.US, "%.2f", item.unitPriceSnapshot)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", item.lineTotal)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavyBlue
                                    )
                                    IconButton(
                                        onClick = { onRemoveDraftItem(item.id) },
                                        modifier = Modifier.testTag("remove_cart_item_${item.product.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Item",
                                            tint = AlertCrimson
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grand Total & Payment Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GRAND TOTAL (সর্বমোট)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%,.2f", grandTotal)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SyncedEmerald,
                                modifier = Modifier.testTag("pos_grand_total_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CASH", "UPI", "CARD").forEach { mode ->
                                FilterChip(
                                    selected = selectedPaymentMethod == mode,
                                    onClick = { onSelectPaymentMethod(mode) },
                                    label = { Text(mode) },
                                    modifier = Modifier.testTag("payment_mode_$mode")
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleRepeatCustomer(!isRepeatCustomer) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isRepeatCustomer,
                                onCheckedChange = onToggleRepeatCustomer,
                                modifier = Modifier.testTag("repeat_customer_checkbox")
                            )
                            Text(
                                text = "Same Customer Split Bill (Do not increment unique Buying Customers count)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onSaveBill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("save_complete_bill_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SyncedEmerald)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Save Bill")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE & COMPLETE BILL • ₹${String.format(Locale.US, "%,.2f", grandTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Product Selection Grid
        item {
            Text(
                text = "Tap Product to Add to Bill • পণ্য নির্বাচন করুন (${filteredProducts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(filteredProducts, key = { it.id }) { product ->
            val hasRate = product.isRateConfigured && product.currentRate > 0.0
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedProductForEntry = product }
                    .testTag("pos_product_card_${product.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${product.nameBn} • Unit: ${product.unitType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = if (hasRate) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (hasRate) {
                                    "₹${String.format(Locale.US, "%.0f", product.currentRate)}/${product.unitType}"
                                } else {
                                    "Configure Rate"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hasRate) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add ${product.nameEn}",
                            tint = DeepNavyBlue
                        )
                    }
                }
            }
        }
    }

    // Quantity & Rate Entry Modal
    selectedProductForEntry?.let { product ->
        val prodVariants = variants.filter { it.productId == product.id }
        var chosenVariant by remember(product) { mutableStateOf(prodVariants.firstOrNull()) }
        var qtyInput by remember(product) { mutableStateOf("1") }
        var rateInput by remember(product) {
            mutableStateOf(
                if (product.isRateConfigured && product.currentRate > 0.0) {
                    product.currentRate.toString()
                } else ""
            )
        }
        var saveRatePermanently by remember(product) { mutableStateOf(!product.isRateConfigured) }

        val parsedQty = qtyInput.toDoubleOrNull() ?: 0.0
        val parsedRate = rateInput.toDoubleOrNull() ?: 0.0
        val previewLineTotal = parsedQty * parsedRate

        AlertDialog(
            onDismissRequest = { selectedProductForEntry = null },
            title = {
                Column {
                    Text(product.nameEn, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${product.nameBn} • Unit: ${product.unitType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (prodVariants.isNotEmpty()) {
                        Text("Select Variant:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(prodVariants) { v ->
                                FilterChip(
                                    selected = chosenVariant?.id == v.id,
                                    onClick = { chosenVariant = v },
                                    label = { Text(v.variantNameEn) }
                                )
                            }
                        }
                    }

                    Text("Quick Quantity Presets:", style = MaterialTheme.typography.labelLarge)
                    val presets = if (product.allowDecimalQty) {
                        listOf("0.25", "0.5", "1", "2", "5")
                    } else {
                        listOf("1", "2", "6", "10", "12")
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            OutlinedButton(
                                onClick = { qtyInput = preset },
                                modifier = Modifier.testTag("qty_preset_$preset")
                            ) {
                                Text("$preset ${product.unitType}")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text("Quantity (${product.unitType})") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (product.allowDecimalQty) KeyboardType.Decimal else KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_qty_input")
                    )

                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = {
                            if (activeRole.canEditRates || !product.isRateConfigured) {
                                rateInput = it
                            }
                        },
                        enabled = activeRole.canEditRates || !product.isRateConfigured,
                        label = {
                            Text(
                                if (product.isRateConfigured) "Selling Rate (₹ / ${product.unitType})"
                                else "Enter Selling Rate (₹ / ${product.unitType} - Not Configured)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_rate_input")
                    )

                    if (activeRole.canEditRates && parsedRate > 0.0 && parsedRate != product.currentRate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveRatePermanently,
                                onCheckedChange = { saveRatePermanently = it }
                            )
                            Text(
                                "Save ₹${String.format(Locale.US, "%.2f", parsedRate)} as default product rate (past bills stay unchanged)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Line Total:", fontWeight = FontWeight.SemiBold)
                            Text(
                                "₹${String.format(Locale.US, "%,.2f", previewLineTotal)}",
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyBlue
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsedQty > 0.0 && parsedRate > 0.0) {
                            if (saveRatePermanently && activeRole.canEditRates && parsedRate != product.currentRate) {
                                onUpdateProductRate(product, parsedRate)
                            }
                            onAddDraftItem(product, chosenVariant, parsedQty, parsedRate)
                            selectedProductForEntry = null
                        }
                    },
                    enabled = parsedQty > 0.0 && parsedRate > 0.0,
                    modifier = Modifier.testTag("confirm_add_to_bill_button")
                ) {
                    Text("Add to Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bill History & Audited Cancellation Modal
    if (showBillHistoryModal) {
        AlertDialog(
            onDismissRequest = { showBillHistoryModal = false },
            title = { Text("Bill History & Audited Cancellation") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (recentTransactions.isEmpty()) {
                        item { Text("No bills recorded yet.") }
                    } else {
                        items(recentTransactions, key = { it.id }) { tx ->
                            TransactionRowCard(
                                tx = tx,
                                onRequestCancel = if (activeRole.canCancelBills) {
                                    { targetTx -> billToCancel = targetTx }
                                } else null
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBillHistoryModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    billToCancel?.let { targetTx ->
        AlertDialog(
            onDismissRequest = { billToCancel = null },
            title = { Text("Cancel Bill ${targetTx.billNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Completed bills are never deleted. Cancelling marks this bill CANCELLED and records an immutable audit log entry.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason (Required)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_cancel_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReasonInput.isNotBlank()) {
                            onCancelCompletedBill(targetTx, cancelReasonInput)
                            cancelReasonInput = ""
                            billToCancel = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    modifier = Modifier.testTag("confirm_bill_cancel_button")
                ) {
                    Text("Confirm Audited Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToCancel = null }) {
                    Text("Back")
                }
            }
        )
    }
}
