package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.FreezerEntity
import com.example.data.local.FreezerPhotoEntity
import com.example.data.local.FreezerSectionEntity
import com.example.data.local.SectionProductMappingEntity
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SubtleOrange
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun saveBitmapToInternalFile(context: Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "freezer_photos").apply { mkdirs() }
    val file = File(dir, "freezer_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return Uri.fromFile(file).toString()
}

@Composable
fun FreezerFinderScreen(
    freezers: List<FreezerEntity>,
    sections: List<FreezerSectionEntity>,
    photosHistory: List<FreezerPhotoEntity>,
    mappings: List<SectionProductMappingEntity>,
    searchQuery: String,
    selectedFreezerNumber: Int,
    onSearchQueryChange: (String) -> Unit,
    onSelectFreezerNumber: (Int) -> Unit,
    onUpdateSection: (FreezerEntity, FreezerSectionEntity, String, String, String?, List<Pair<String, String>>) -> Unit,
    onAddCustomSection: (FreezerEntity, String, String) -> Unit,
    onDeleteMapping: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler(onBack = onNavigateBackHome)

    val context = LocalContext.current
    val activeFreezer = freezers.find { it.freezerNumber == selectedFreezerNumber }
        ?: freezers.firstOrNull()
    val freezerSections = remember(sections, selectedFreezerNumber) {
        sections.filter { it.freezerNumber == selectedFreezerNumber }
    }
    var selectedSectionId by remember(selectedFreezerNumber, freezerSections) {
        mutableStateOf(
            freezerSections.find { it.sectionCode == "Section B" && selectedFreezerNumber == 3 }?.id
                ?: freezerSections.firstOrNull()?.id
                ?: ""
        )
    }
    val activeSection = freezerSections.find { it.id == selectedSectionId } ?: freezerSections.firstOrNull()

    var showEditSectionModal by remember { mutableStateOf(false) }
    var showAddSectionModal by remember { mutableStateOf(false) }
    var showPhotoHistoryModal by remember { mutableStateOf(false) }

    // Search matching across product mappings & manual notes
    val matchingMappings = remember(searchQuery, mappings) {
        if (searchQuery.isBlank()) emptyList()
        else mappings.filter {
            it.productName.contains(searchQuery, ignoreCase = true) ||
                it.stockNote.contains(searchQuery, ignoreCase = true) ||
                it.sectionCode.contains(searchQuery, ignoreCase = true)
        }
    }

    val matchingSectionsByNotes = remember(searchQuery, sections) {
        if (searchQuery.isBlank()) emptyList()
        else sections.filter {
            it.manualNotes.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Camera and Photo Picker launchers for activeSection
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && activeFreezer != null && activeSection != null) {
            val savedUri = saveBitmapToInternalFile(context, bitmap)
            onUpdateSection(
                activeFreezer,
                activeSection,
                activeSection.description,
                activeSection.manualNotes,
                savedUri,
                emptyList()
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && activeFreezer != null && activeSection != null) {
            onUpdateSection(
                activeFreezer,
                activeSection,
                activeSection.description,
                activeSection.manualNotes,
                uri.toString(),
                emptyList()
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("freezer_finder_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Search Bar
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "7-Freezer Product & Section Finder • ফ্রিজার খুঁজুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search e.g. 'Vetki Fish Finger', 'Salmon', 'Ice Cream'...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Freezer") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("freezer_search_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Vetki Fish Finger", "Salmon Fish", "Vetki Fish Fry", "Ice Cream")) { quickTerm ->
                            FilterChip(
                                selected = searchQuery.equals(quickTerm, ignoreCase = true),
                                onClick = { onSearchQueryChange(quickTerm) },
                                label = { Text(quickTerm) },
                                modifier = Modifier.testTag("quick_search_${quickTerm.replace(" ", "_").lowercase(Locale.US)}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Search Results (if query is active)
        if (searchQuery.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("freezer_search_results_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Search Results for \"$searchQuery\" (${matchingMappings.size + matchingSectionsByNotes.size} matches)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (matchingMappings.isEmpty() && matchingSectionsByNotes.isEmpty()) {
                            Text(
                                text = "No section mapped for \"$searchQuery\" yet. Select a Freezer & Section below and tap 'Edit / Map Product' to record its location.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            matchingMappings.forEach { match ->
                                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(match.updatedAt))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onSelectFreezerNumber(match.freezerNumber)
                                            selectedSectionId = match.sectionId
                                        }
                                        .testTag("search_result_freezer_${match.freezerNumber}_${match.sectionCode}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${match.productName} → Freezer ${match.freezerNumber} • ${match.sectionCode}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavyBlue
                                            )
                                            Text(
                                                text = "Note: ${match.stockNote}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Last updated: $timeStr by ${match.updatedByStaffName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onSelectFreezerNumber(match.freezerNumber)
                                                selectedSectionId = match.sectionId
                                            }
                                        ) {
                                            Text("View Photo")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Seven Deep Freezers Selector (Freezer 1 .. Freezer 7)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Deep Freezer (Freezer 1 – Freezer 7):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(freezers, key = { it.id }) { freezer ->
                        val isSelected = freezer.freezerNumber == selectedFreezerNumber
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DeepNavyBlue else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { onSelectFreezerNumber(freezer.freezerNumber) }
                                .testTag("freezer_card_${freezer.freezerNumber}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AcUnit,
                                        contentDescription = freezer.freezerNameEn,
                                        tint = if (isSelected) Color.White else FreshTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = freezer.freezerNameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else DeepNavyBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = freezer.description,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Section Selector & Active Section Profile
        if (activeFreezer != null && activeSection != null) {
            item {
                val sectionMappingsForCurrent = mappings.filter { it.sectionId == activeSection.id }
                val sectionPhotosForCurrent = photosHistory.filter { it.sectionId == activeSection.id }
                val updatedTimeStr = remember(activeSection.lastUpdatedAt) {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(activeSection.lastUpdatedAt))
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${activeFreezer.freezerNameEn} (${activeFreezer.freezerNameBn})",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DeepNavyBlue
                                )
                                Text(
                                    text = activeFreezer.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showAddSectionModal = true },
                                modifier = Modifier.testTag("add_custom_section_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Section", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Section")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Section Chips (Section A, Section B, Section C, Section D...)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(freezerSections, key = { it.id }) { sec ->
                                FilterChip(
                                    selected = sec.id == activeSection.id,
                                    onClick = { selectedSectionId = sec.id },
                                    label = { Text(sec.sectionCode) },
                                    modifier = Modifier.testTag("section_chip_${sec.sectionCode.replace(" ", "_").lowercase(Locale.US)}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Section Photo Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!activeSection.currentPhotoUri.isNullOrBlank()) {
                                if (activeSection.currentPhotoUri.contains("img_freezer_section_sample")) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_freezer_section_sample_1794392168908),
                                        contentDescription = "${activeFreezer.freezerNameEn} ${activeSection.sectionCode} Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = Uri.parse(activeSection.currentPhotoUri),
                                        contentDescription = "${activeFreezer.freezerNameEn} ${activeSection.sectionCode} Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "No Photo Yet",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No photograph captured for ${activeSection.sectionCode} yet.",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Use Camera or Photo Picker below to attach a top-layer section photo.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Honesty Notice about stacked items
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Verification Notice",
                                    tint = SubtleOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Photographs show top-layer arrangement only. Products stacked beneath other packets cannot be auto-detected from photos — always check timestamp ($updatedTimeStr by ${activeSection.updatedByStaffName}).",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Camera, Gallery, Photo History & Edit Section Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("freezer_capture_camera_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshTeal)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera")
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("freezer_pick_gallery_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery")
                            }
                            OutlinedButton(
                                onClick = { showPhotoHistoryModal = true },
                                modifier = Modifier.testTag("freezer_photo_history_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(${sectionPhotosForCurrent.size})")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Section Details & Mapped Products
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeFreezer.freezerNameEn} • ${activeSection.sectionCode}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DeepNavyBlue
                                )
                                Text(
                                    text = "Last updated: $updatedTimeStr by ${activeSection.updatedByStaffName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showEditSectionModal = true },
                                modifier = Modifier.testTag("edit_section_notes_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Section", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit / Map")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Description: ${activeSection.description}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Manual Notes: ${activeSection.manualNotes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Mapped Products in ${activeSection.sectionCode} (${sectionMappingsForCurrent.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (sectionMappingsForCurrent.isEmpty()) {
                            Text(
                                text = "No specific products mapped to this section yet. Tap 'Edit / Map' to map items gradually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            sectionMappingsForCurrent.forEach { mapItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "• ${mapItem.productName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepNavyBlue
                                        )
                                        Text(
                                            text = "  ${mapItem.stockNote}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteMapping(mapItem.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Mapping"
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

    // Edit Section & Map Product Modal
    if (showEditSectionModal && activeFreezer != null && activeSection != null) {
        var descInput by remember(activeSection) { mutableStateOf(activeSection.description) }
        var notesInput by remember(activeSection) { mutableStateOf(activeSection.manualNotes) }
        var newMappedProdName by remember { mutableStateOf("") }
        var newMappedProdNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditSectionModal = false },
            title = { Text("Update ${activeFreezer.freezerNameEn} • ${activeSection.sectionCode}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Section Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Manual Placement Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("section_manual_notes_input")
                    )
                    HorizontalDivider()
                    Text(
                        "Add / Correct Product Location Mapping (Optional):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = newMappedProdName,
                        onValueChange = { newMappedProdName = it },
                        label = { Text("Product Name (e.g. Vetki Fish Finger)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("section_map_product_name_input")
                    )
                    OutlinedTextField(
                        value = newMappedProdNote,
                        onValueChange = { newMappedProdNote = it },
                        label = { Text("Placement Note (e.g. Top left basket)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("section_map_product_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val extraMappings = if (newMappedProdName.isNotBlank()) {
                            listOf(newMappedProdName.trim() to newMappedProdNote.trim())
                        } else emptyList()
                        onUpdateSection(
                            activeFreezer,
                            activeSection,
                            descInput,
                            notesInput,
                            null,
                            extraMappings
                        )
                        showEditSectionModal = false
                    },
                    modifier = Modifier.testTag("save_section_update_button")
                ) {
                    Text("Save Section Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSectionModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Section Modal
    if (showAddSectionModal && activeFreezer != null) {
        var newSecCode by remember { mutableStateOf("Section E") }
        var newSecDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSectionModal = false },
            title = { Text("Add Section to ${activeFreezer.freezerNameEn}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSecCode,
                        onValueChange = { newSecCode = it },
                        label = { Text("Section Name (e.g. Section E)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSecDesc,
                        onValueChange = { newSecDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomSection(activeFreezer, newSecCode, newSecDesc)
                        showAddSectionModal = false
                    }
                ) {
                    Text("Add Section")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Photo History Modal
    if (showPhotoHistoryModal && activeSection != null) {
        val historyList = photosHistory.filter { it.sectionId == activeSection.id }
        AlertDialog(
            onDismissRequest = { showPhotoHistoryModal = false },
            title = { Text("Photo History • ${activeSection.sectionCode}") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (historyList.isEmpty()) {
                        item { Text("No photographs archived for this section yet.") }
                    } else {
                        items(historyList, key = { it.id }) { ph ->
                            val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(ph.capturedAt))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (ph.isCurrent) "CURRENT PHOTO • $ts" else "PREVIOUS PHOTO • $ts",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (ph.isCurrent) FreshTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Updated by: ${ph.uploadedByStaffName} (${ph.uploadStatus})",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = ph.notesSnapshot,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoHistoryModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}
