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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.local.AuditLogEntity
import com.example.data.local.ProductEntity
import com.example.data.local.RoleEntity
import com.example.data.local.StaffProfileEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SyncedEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsAndStaffScreen(
    staffProfiles: List<StaffProfileEntity>,
    activeStaff: StaffProfileEntity,
    activeRole: RoleEntity,
    products: List<ProductEntity>,
    auditLogs: List<AuditLogEntity>,
    googleUserEmail: String?,
    onSwitchStaffWithPin: (StaffProfileEntity, String, (Boolean, String) -> Unit) -> Unit,
    onUpdateProductRate: (ProductEntity, Double) -> Unit,
    onAddNewProduct: (String, String, String, String, Double) -> Unit,
    onGoogleAuthSuccess: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler(onBack = onNavigateBackHome)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var staffToAuthenticate by remember { mutableStateOf<StaffProfileEntity?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var productToEditRate by remember { mutableStateOf<ProductEntity?>(null) }
    var newRateText by remember { mutableStateOf("") }
    var showAddProductModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_staff_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Staff Management & Role-Based Login Switcher
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = DeepNavyBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Staff Login & Role Management • কর্মী ও রোল",
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepNavyBlue
                            )
                            Text(
                                text = "Active: ${activeStaff.displayNameEn} (${activeStaff.roleCode}) • Default PINs: Owner=1976, Manager=2222, Staff1=1111, Staff2=3333",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    staffProfiles.forEach { profile ->
                        val isCurrent = profile.id == activeStaff.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!isCurrent) {
                                        staffToAuthenticate = profile
                                        pinInput = ""
                                        pinError = null
                                    }
                                }
                                .testTag("staff_profile_card_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = profile.displayNameEn,
                                        tint = if (isCurrent) SyncedEmerald else DeepNavyBlue
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${profile.displayNameEn} • ${profile.displayNameBn}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = profile.badgeLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SyncedEmerald
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            staffToAuthenticate = profile
                                            pinInput = ""
                                            pinError = null
                                        },
                                        modifier = Modifier.testTag("switch_staff_btn_${profile.id}")
                                    ) {
                                        Text("Switch")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Cloud Account (Google Sign-In for Kolkata Sync)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cloud Sync Account (Google Sign-In)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (googleUserEmail != null) {
                        Text(
                            text = "Connected Google Account: $googleUserEmail",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SyncedEmerald
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                signOutFromGoogle(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = onGoogleSignOut
                                )
                            },
                            modifier = Modifier.testTag("settings_google_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out of Cloud Sync")
                        }
                    } else {
                        Text(
                            text = "Currently running in Local Offline Mode. Sign in with Google to synchronize with Kolkata Owner Cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoogleSignInButton(
                            onAuthSuccess = onGoogleAuthSuccess,
                            onAuthError = {}
                        )
                    }
                }
            }
        }

        // 3. Product Rate Management (Owner / Manager Permission)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Product Rates & Catalogue • পণ্যের দাম",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeRole.canEditRates) {
                                    "Tap any product to configure or update its selling rate. Past bills keep their original price snapshot."
                                } else {
                                    "Rate editing is locked for STAFF role. Switch to Owner or Manager to change rates."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activeRole.canEditRates) {
                            Button(
                                onClick = { showAddProductModal = true },
                                modifier = Modifier.testTag("add_new_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    products.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${product.nameEn} (${product.nameBn})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Unit: ${product.unitType} • Updated by ${product.updatedByStaffName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (product.isRateConfigured && product.currentRate > 0.0) {
                                        "₹${String.format(Locale.US, "%.2f", product.currentRate)}/${product.unitType}"
                                    } else {
                                        "Unset (₹0)"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (product.isRateConfigured) DeepNavyBlue else AlertCrimson
                                )
                                if (activeRole.canEditRates) {
                                    TextButton(
                                        onClick = {
                                            productToEditRate = product
                                            newRateText = if (product.currentRate > 0.0) product.currentRate.toString() else ""
                                        },
                                        modifier = Modifier.testTag("edit_rate_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Rate", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Set")
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        // 4. Immutable Audit Logs
        item {
            Text(
                text = "Security & Financial Audit Logs (${auditLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(auditLogs.take(25), key = { it.id }) { log ->
            val ts = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US).format(Date(log.timestamp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${log.actionType} • ${log.actorName} (${log.actorRole})",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepNavyBlue
                        )
                        Text(
                            text = ts,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.summary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Staff PIN Dialog
    staffToAuthenticate?.let { targetStaff ->
        AlertDialog(
            onDismissRequest = { staffToAuthenticate = null },
            title = { Text("Login as ${targetStaff.displayNameEn}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter 4-digit staff PIN (${targetStaff.badgeLabel}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_pin_input")
                    )
                    if (!pinError.isNullOrBlank()) {
                        Text(
                            text = pinError!!,
                            color = AlertCrimson,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSwitchStaffWithPin(targetStaff, pinInput) { ok, msg ->
                            if (ok) {
                                staffToAuthenticate = null
                            } else {
                                pinError = msg
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_staff_pin_button")
                ) {
                    Text("Verify & Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToAuthenticate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Product Rate Dialog
    productToEditRate?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToEditRate = null },
            title = { Text("Set Selling Rate: ${prod.nameEn}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unit: ${prod.unitType}. Historical bills will keep their original rate snapshot.")
                    OutlinedTextField(
                        value = newRateText,
                        onValueChange = { newRateText = it },
                        label = { Text("Selling Rate (₹ / ${prod.unitType})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_new_rate_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newRateText.toDoubleOrNull() ?: 0.0
                        if (parsed > 0.0) {
                            onUpdateProductRate(prod, parsed)
                            productToEditRate = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_rate_button")
                ) {
                    Text("Save Rate")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToEditRate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Product Modal
    if (showAddProductModal) {
        var nameEn by remember { mutableStateOf("") }
        var nameBn by remember { mutableStateOf("") }
        var unitType by remember { mutableStateOf("KG") }
        var category by remember { mutableStateOf("FROZEN_FISH") }
        var rateInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProductModal = false },
            title = { Text("Add New Product to Catalogue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("Product Name (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("Product Name (বাংলা - Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("KG", "GRAM", "PIECE", "PACKET").forEach { u ->
                            FilterChip(
                                selected = unitType == u,
                                onClick = { unitType = u },
                                label = { Text(u) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Selling Rate in ₹ (Leave blank if unset)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            onAddNewProduct(
                                nameEn,
                                nameBn,
                                category,
                                unitType,
                                rateInput.toDoubleOrNull() ?: 0.0
                            )
                            showAddProductModal = false
                        }
                    }
                ) {
                    Text("Save Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
