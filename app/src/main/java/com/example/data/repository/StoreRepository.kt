package com.example.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.example.data.local.AppDao
import com.example.data.local.AuditLogEntity
import com.example.data.local.FreezerEntity
import com.example.data.local.FreezerPhotoEntity
import com.example.data.local.FreezerSectionEntity
import com.example.data.local.NewFarmDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ProductVariantEntity
import com.example.data.local.RoleEntity
import com.example.data.local.SectionProductMappingEntity
import com.example.data.local.StaffProfileEntity
import com.example.data.local.SyncQueueEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemEntity
import com.example.data.local.VisitorEntity
import com.example.data.remote.CloudStoreRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DraftBillItem(
    val id: String = UUID.randomUUID().toString(),
    val product: ProductEntity,
    val variant: ProductVariantEntity? = null,
    val quantity: Double,
    val unitPriceSnapshot: Double
) {
    val lineTotal: Double get() = quantity * unitPriceSnapshot
    val displayNameEn: String
        get() = if (variant != null) "${product.nameEn} (${variant.variantNameEn})" else product.nameEn
    val displayNameBn: String
        get() = if (variant != null) "${product.nameBn} (${variant.variantNameBn})" else product.nameBn
}

class StoreRepository(
    private val db: NewFarmDatabase,
    private val dao: AppDao,
    private val cloudRepo: CloudStoreRepository
) {
    val roles: Flow<List<RoleEntity>> = dao.observeRoles()
    val staffProfiles: Flow<List<StaffProfileEntity>> = dao.observeStaffProfiles()
    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val productVariants: Flow<List<ProductVariantEntity>> = dao.observeProductVariants()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val transactionItems: Flow<List<TransactionItemEntity>> = dao.observeAllTransactionItems()
    val visitors: Flow<List<VisitorEntity>> = dao.observeVisitors()
    val freezers: Flow<List<FreezerEntity>> = dao.observeFreezers()
    val freezerSections: Flow<List<FreezerSectionEntity>> = dao.observeFreezerSections()
    val freezerPhotos: Flow<List<FreezerPhotoEntity>> = dao.observeFreezerPhotos()
    val sectionMappings: Flow<List<SectionProductMappingEntity>> = dao.observeSectionProductMappings()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val pendingSyncQueue: Flow<List<SyncQueueEntity>> = dao.observePendingSyncQueue()

    suspend fun ensureSeeded(context: Context) {
        withContext(Dispatchers.IO) {
            NewFarmDatabase.seedInitialCatalogIfNeeded(context, dao)
        }
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    suspend fun verifyStaffPin(staff: StaffProfileEntity, enteredPin: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = dao.getUserById(staff.userId) ?: return@withContext false
            val candidateHash = NewFarmDatabase.hashPin(enteredPin.trim())
            val matched = user.pinHash == candidateHash
            if (matched) {
                dao.touchStaffActivity(staff.id, System.currentTimeMillis())
            }
            matched
        }
    }

    suspend fun createAndSaveBill(
        items: List<DraftBillItem>,
        paymentMethod: String,
        isRepeatCustomer: Boolean,
        customerRefOptional: String?,
        activeStaff: StaffProfileEntity,
        viaVoiceAssistant: Boolean = false
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot save an empty bill."))
        }
        val unconfigured = items.firstOrNull { it.unitPriceSnapshot <= 0.0 }
        if (unconfigured != null) {
            return@withContext Result.failure(
                IllegalStateException("Rate for ${unconfigured.displayNameEn} is not configured yet. Please set its selling rate first.")
            )
        }

        val now = System.currentTimeMillis()
        val dateKey = todayDateKey(now)
        val compactDate = dateKey.replace("-", "")
        val countToday = dao.getTransactionCountForDate(dateKey) + 1
        val shortUuid = UUID.randomUUID().toString().take(4).uppercase(Locale.US)
        val txId = "tx_${compactDate}_${String.format(Locale.US, "%04d", countToday)}_$shortUuid"
        val billNumber = "NFP-$compactDate-${String.format(Locale.US, "%03d", countToday)}-$shortUuid"

        val grandTotal = items.sumOf { it.lineTotal }
        val summary = items.joinToString("; ") { item ->
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity)
            "${item.displayNameEn} $qtyStr ${item.product.unitType} @ ₹${String.format(Locale.US, "%.2f", item.unitPriceSnapshot)}"
        }

        val txEntity = TransactionEntity(
            id = txId,
            billNumber = billNumber,
            createdAt = now,
            dateKey = dateKey,
            staffId = activeStaff.id,
            staffNameSnapshot = activeStaff.displayNameEn,
            grandTotal = grandTotal,
            lineCount = items.size,
            itemsSummarySnapshot = summary,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            isRepeatCustomerBill = isRepeatCustomer,
            customerRefOptional = customerRefOptional?.takeIf { it.isNotBlank() },
            syncStatus = "PENDING"
        )

        val itemEntities = items.mapIndexed { index, draft ->
            TransactionItemEntity(
                id = "${txId}_item_${index + 1}",
                transactionId = txId,
                productId = draft.product.id,
                variantId = draft.variant?.id,
                productNameSnapshot = draft.displayNameEn,
                productNameBnSnapshot = draft.displayNameBn,
                quantity = draft.quantity,
                unitTypeSnapshot = draft.product.unitType,
                unitPriceSnapshot = draft.unitPriceSnapshot,
                lineTotal = draft.lineTotal
            )
        }

        val auditId = "audit_${UUID.randomUUID().toString().replace("-", "")}"
        val actionType = if (viaVoiceAssistant) "VOICE_BILL_CONFIRMED" else "BILL_CREATED"
        val auditEntity = AuditLogEntity(
            id = auditId,
            timestamp = now,
            actorStaffId = activeStaff.id,
            actorName = activeStaff.displayNameEn,
            actorRole = activeStaff.roleCode,
            actionType = actionType,
            entityType = "TRANSACTION",
            entityId = txId,
            summary = "Bill $billNumber saved (₹${String.format(Locale.US, "%.2f", grandTotal)}, ${items.size} items, $paymentMethod) — $summary"
        )

        val syncPayload = JSONObject().apply {
            put("txId", txId)
            put("billNumber", billNumber)
            put("staffId", activeStaff.id)
            put("staffName", activeStaff.displayNameEn)
            put("grandTotal", grandTotal)
            put("lineCount", items.size)
            put("itemsSummary", summary)
            put("paymentMethod", paymentMethod)
            put("status", "COMPLETED")
            put("isRepeatCustomer", isRepeatCustomer)
            put("saleEpochMillis", now)
        }.toString()

        db.withTransaction {
            dao.insertTransaction(txEntity)
            dao.insertTransactionItems(itemEntities)
            dao.insertAuditLog(auditEntity)
            dao.enqueueSyncItem(
                SyncQueueEntity(
                    id = "sync_$txId",
                    entityType = "TRANSACTION",
                    entityId = txId,
                    operation = "UPSERT",
                    payloadJson = syncPayload,
                    createdAt = now
                )
            )
        }

        // Attempt non-blocking cloud sync if signed in
        syncSingleTransactionWithCloud(txEntity)
        Result.success(txEntity)
    }

    suspend fun cancelBillWithAudit(
        transaction: TransactionEntity,
        reason: String,
        activeStaff: StaffProfileEntity,
        activeRole: RoleEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!activeRole.canCancelBills) {
            return@withContext Result.failure(
                SecurityException("Only Owner or Manager can authorize bill cancellation.")
            )
        }
        if (reason.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter a valid cancellation reason for the audit log.")
            )
        }
        val now = System.currentTimeMillis()
        val auditId = "audit_${UUID.randomUUID().toString().replace("-", "")}"
        val cleanReason = reason.trim()

        db.withTransaction {
            dao.markTransactionCancelled(
                txId = transaction.id,
                reason = cleanReason,
                staffName = activeStaff.displayNameEn,
                cancelledAt = now
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    id = auditId,
                    timestamp = now,
                    actorStaffId = activeStaff.id,
                    actorName = activeStaff.displayNameEn,
                    actorRole = activeStaff.roleCode,
                    actionType = "BILL_CANCELLED",
                    entityType = "TRANSACTION",
                    entityId = transaction.id,
                    summary = "Cancelled Bill ${transaction.billNumber} (₹${String.format(Locale.US, "%.2f", transaction.grandTotal)}). Reason: $cleanReason"
                )
            )
            dao.enqueueSyncItem(
                SyncQueueEntity(
                    id = "sync_cancel_${transaction.id}",
                    entityType = "TRANSACTION_CANCEL",
                    entityId = transaction.id,
                    operation = "CANCEL",
                    payloadJson = JSONObject().apply {
                        put("txId", transaction.id)
                        put("reason", cleanReason)
                    }.toString(),
                    createdAt = now
                )
            )
        }

        val cloudRes = cloudRepo.cancelCloudTransaction(transaction.id, cleanReason)
        if (cloudRes.isSuccess) {
            dao.updateTransactionSyncStatus(transaction.id, "SYNCED")
            dao.updateSyncQueueStatus("sync_cancel_${transaction.id}", "SYNCED", 0, now)
        }
        Result.success(Unit)
    }

    suspend fun recordVisitor(
        delta: Int,
        correctionReason: String?,
        activeStaff: StaffProfileEntity
    ): Result<VisitorEntity> = withContext(Dispatchers.IO) {
        if (delta == -1 && correctionReason.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("A reason is required when correcting visitor count.")
            )
        }
        val now = System.currentTimeMillis()
        val dateKey = todayDateKey(now)
        val visitorId = "vis_${UUID.randomUUID().toString().replace("-", "")}"
        val entity = VisitorEntity(
            id = visitorId,
            timestamp = now,
            dateKey = dateKey,
            delta = if (delta >= 0) 1 else -1,
            staffId = activeStaff.id,
            staffNameSnapshot = activeStaff.displayNameEn,
            correctionReason = correctionReason?.trim()?.takeIf { it.isNotEmpty() },
            syncStatus = "PENDING"
        )

        db.withTransaction {
            dao.insertVisitor(entity)
            if (entity.delta == -1) {
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "audit_${UUID.randomUUID().toString().replace("-", "")}",
                        timestamp = now,
                        actorStaffId = activeStaff.id,
                        actorName = activeStaff.displayNameEn,
                        actorRole = activeStaff.roleCode,
                        actionType = "VISITOR_CORRECTED",
                        entityType = "VISITOR",
                        entityId = visitorId,
                        summary = "Visitor count corrected (-1) by ${activeStaff.displayNameEn}: ${entity.correctionReason}"
                    )
                )
            }
            dao.enqueueSyncItem(
                SyncQueueEntity(
                    id = "sync_$visitorId",
                    entityType = "VISITOR",
                    entityId = visitorId,
                    operation = "UPSERT",
                    payloadJson = JSONObject().apply {
                        put("visitorId", visitorId)
                        put("dateKey", dateKey)
                        put("delta", entity.delta)
                        put("staffId", activeStaff.id)
                        put("staffName", activeStaff.displayNameEn)
                        put("correctionReason", entity.correctionReason ?: "")
                        put("eventEpochMillis", now)
                    }.toString(),
                    createdAt = now
                )
            )
        }

        val cloudRes = cloudRepo.syncVisitor(
            visitorId = visitorId,
            dateKey = dateKey,
            delta = entity.delta,
            staffId = activeStaff.id,
            staffName = activeStaff.displayNameEn,
            correctionReason = entity.correctionReason,
            eventEpochMillis = now
        )
        if (cloudRes.isSuccess) {
            dao.updateVisitorSyncStatus(visitorId, "SYNCED")
            dao.updateSyncQueueStatus("sync_$visitorId", "SYNCED", 0, now)
        }
        Result.success(entity)
    }

    suspend fun updateProductRate(
        product: ProductEntity,
        newRate: Double,
        activeStaff: StaffProfileEntity,
        activeRole: RoleEntity
    ): Result<ProductEntity> = withContext(Dispatchers.IO) {
        if (!activeRole.canEditRates) {
            return@withContext Result.failure(
                SecurityException("Only Owner or Manager can edit selling rates.")
            )
        }
        if (newRate <= 0.0) {
            return@withContext Result.failure(
                IllegalArgumentException("Selling rate must be greater than ₹0.")
            )
        }
        val now = System.currentTimeMillis()
        val updated = product.copy(
            currentRate = newRate,
            isRateConfigured = true,
            updatedAt = now,
            updatedByStaffName = activeStaff.displayNameEn
        )
        val auditId = "audit_${UUID.randomUUID().toString().replace("-", "")}"

        db.withTransaction {
            dao.updateProduct(updated)
            dao.insertAuditLog(
                AuditLogEntity(
                    id = auditId,
                    timestamp = now,
                    actorStaffId = activeStaff.id,
                    actorName = activeStaff.displayNameEn,
                    actorRole = activeStaff.roleCode,
                    actionType = "RATE_UPDATED",
                    entityType = "PRODUCT",
                    entityId = product.id,
                    summary = "Updated rate for ${product.nameEn} from ₹${product.currentRate} to ₹$newRate / ${product.unitType} (Historical bills unchanged)"
                )
            )
            dao.enqueueSyncItem(
                SyncQueueEntity(
                    id = "sync_${product.id}",
                    entityType = "PRODUCT",
                    entityId = product.id,
                    operation = "UPSERT",
                    payloadJson = JSONObject().apply {
                        put("productId", updated.id)
                        put("nameEn", updated.nameEn)
                        put("nameBn", updated.nameBn)
                        put("unitType", updated.unitType)
                        put("currentRate", updated.currentRate)
                        put("isRateConfigured", updated.isRateConfigured)
                        put("isActive", updated.isActive)
                    }.toString(),
                    createdAt = now
                )
            )
        }

        val cloudRes = cloudRepo.syncProduct(
            productId = updated.id,
            nameEn = updated.nameEn,
            nameBn = updated.nameBn,
            unitType = updated.unitType,
            currentRate = updated.currentRate,
            isRateConfigured = updated.isRateConfigured,
            isActive = updated.isActive
        )
        if (cloudRes.isSuccess) {
            dao.updateSyncQueueStatus("sync_${product.id}", "SYNCED", 0, now)
        }
        Result.success(updated)
    }

    suspend fun addNewProduct(
        nameEn: String,
        nameBn: String,
        category: String,
        unitType: String,
        rate: Double,
        activeStaff: StaffProfileEntity,
        activeRole: RoleEntity
    ): Result<ProductEntity> = withContext(Dispatchers.IO) {
        if (!activeRole.canEditRates) {
            return@withContext Result.failure(
                SecurityException("Only Owner or Manager can add products.")
            )
        }
        val cleanEn = nameEn.trim()
        if (cleanEn.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Product name is required."))
        }
        val now = System.currentTimeMillis()
        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val allowDecimal = unitType == "KG" || unitType == "GRAM"
        val entity = ProductEntity(
            id = prodId,
            sortOrder = 99,
            nameEn = cleanEn,
            nameBn = nameBn.trim().ifEmpty { cleanEn },
            nameHi = cleanEn,
            category = category,
            unitType = unitType,
            allowDecimalQty = allowDecimal,
            currentRate = if (rate > 0.0) rate else 0.0,
            isRateConfigured = rate > 0.0,
            isActive = true,
            updatedAt = now,
            updatedByStaffName = activeStaff.displayNameEn
        )
        db.withTransaction {
            dao.insertProduct(entity)
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "audit_${UUID.randomUUID().toString().replace("-", "")}",
                    timestamp = now,
                    actorStaffId = activeStaff.id,
                    actorName = activeStaff.displayNameEn,
                    actorRole = activeStaff.roleCode,
                    actionType = "PRODUCT_ADDED",
                    entityType = "PRODUCT",
                    entityId = prodId,
                    summary = "Added product ${entity.nameEn} (${entity.unitType}) @ ₹${entity.currentRate}"
                )
            )
        }
        Result.success(entity)
    }

    suspend fun updateFreezerSectionDetails(
        freezer: FreezerEntity,
        section: FreezerSectionEntity,
        newDescription: String,
        newManualNotes: String,
        newPhotoUri: String?,
        mappedProductsList: List<Pair<String, String>>, // productName to stockNote
        activeStaff: StaffProfileEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val finalPhotoUri = newPhotoUri ?: section.currentPhotoUri
        val updatedSection = section.copy(
            description = newDescription.trim().ifEmpty { section.description },
            manualNotes = newManualNotes.trim(),
            currentPhotoUri = finalPhotoUri,
            lastUpdatedAt = now,
            updatedByStaffId = activeStaff.id,
            updatedByStaffName = activeStaff.displayNameEn,
            syncStatus = "PENDING"
        )
        val updatedFreezer = freezer.copy(
            lastUpdatedAt = now,
            lastUpdatedByStaffName = activeStaff.displayNameEn
        )

        db.withTransaction {
            dao.updateFreezerSection(updatedSection)
            dao.updateFreezer(updatedFreezer)

            if (!newPhotoUri.isNullOrBlank() && newPhotoUri != section.currentPhotoUri) {
                dao.archivePreviousSectionPhotos(section.id)
                dao.insertFreezerPhoto(
                    FreezerPhotoEntity(
                        id = "photo_${UUID.randomUUID().toString().replace("-", "")}",
                        freezerId = freezer.id,
                        sectionId = section.id,
                        photoUri = newPhotoUri,
                        notesSnapshot = newManualNotes.trim().ifEmpty { updatedSection.description },
                        capturedAt = now,
                        uploadedByStaffId = activeStaff.id,
                        uploadedByStaffName = activeStaff.displayNameEn,
                        uploadStatus = "LOCAL_SAVED",
                        isCurrent = true
                    )
                )
            }

            mappedProductsList.forEach { (prodName, note) ->
                if (prodName.isNotBlank()) {
                    val mapId = "map_${section.id}_${prodName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_")}"
                    dao.insertSectionProductMapping(
                        SectionProductMappingEntity(
                            id = mapId,
                            freezerId = freezer.id,
                            freezerNumber = freezer.freezerNumber,
                            sectionId = section.id,
                            sectionCode = section.sectionCode,
                            productName = prodName.trim(),
                            stockNote = note.trim().ifEmpty { "Verified in ${section.sectionCode}" },
                            updatedAt = now,
                            updatedByStaffName = activeStaff.displayNameEn
                        )
                    )
                }
            }

            val mappedNamesSummary = mappedProductsList.joinToString(", ") { it.first }
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "audit_${UUID.randomUUID().toString().replace("-", "")}",
                    timestamp = now,
                    actorStaffId = activeStaff.id,
                    actorName = activeStaff.displayNameEn,
                    actorRole = activeStaff.roleCode,
                    actionType = "FREEZER_UPDATED",
                    entityType = "FREEZER_SECTION",
                    entityId = section.id,
                    summary = "Updated Freezer ${freezer.freezerNumber} ${section.sectionCode} by ${activeStaff.displayNameEn}. Notes: ${newManualNotes.take(120)}"
                )
            )
            dao.enqueueSyncItem(
                SyncQueueEntity(
                    id = "sync_${section.id}",
                    entityType = "FREEZER_SECTION",
                    entityId = section.id,
                    operation = "UPSERT",
                    payloadJson = JSONObject().apply {
                        put("sectionId", section.id)
                        put("freezerNumber", freezer.freezerNumber)
                        put("sectionCode", section.sectionCode)
                        put("mappedProducts", mappedNamesSummary)
                        put("manualNotes", newManualNotes.trim())
                        put("updatedByStaffName", activeStaff.displayNameEn)
                        put("updatedEpochMillis", now)
                    }.toString(),
                    createdAt = now
                )
            )
        }

        val cloudRes = cloudRepo.syncFreezerSection(
            sectionId = section.id,
            freezerNumber = freezer.freezerNumber,
            sectionCode = section.sectionCode,
            mappedProducts = mappedProductsList.joinToString(", ") { it.first },
            manualNotes = newManualNotes.trim(),
            updatedByStaffName = activeStaff.displayNameEn,
            updatedEpochMillis = now
        )
        if (cloudRes.isSuccess) {
            dao.updateSyncQueueStatus("sync_${section.id}", "SYNCED", 0, now)
        }
        Result.success(Unit)
    }

    suspend fun addCustomSectionToFreezer(
        freezer: FreezerEntity,
        sectionCode: String,
        description: String,
        activeStaff: StaffProfileEntity
    ): Result<FreezerSectionEntity> = withContext(Dispatchers.IO) {
        val cleanCode = sectionCode.trim().ifEmpty { "Section E" }
        val now = System.currentTimeMillis()
        val secId = "freezer_${freezer.freezerNumber}_sec_${cleanCode.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_")}"
        val section = FreezerSectionEntity(
            id = secId,
            freezerId = freezer.id,
            freezerNumber = freezer.freezerNumber,
            sectionCode = cleanCode,
            description = description.trim().ifEmpty { "$cleanCode in Freezer ${freezer.freezerNumber}" },
            manualNotes = "Newly added section by ${activeStaff.displayNameEn}",
            currentPhotoUri = null,
            lastUpdatedAt = now,
            updatedByStaffId = activeStaff.id,
            updatedByStaffName = activeStaff.displayNameEn
        )
        dao.insertFreezerSection(section)
        Result.success(section)
    }

    suspend fun deleteProductMapping(mappingId: String) = withContext(Dispatchers.IO) {
        dao.deleteSectionProductMapping(mappingId)
    }

    private suspend fun syncSingleTransactionWithCloud(tx: TransactionEntity): Boolean {
        val res = cloudRepo.syncTransaction(
            txId = tx.id,
            billNumber = tx.billNumber,
            staffId = tx.staffId,
            staffName = tx.staffNameSnapshot,
            grandTotal = tx.grandTotal,
            lineCount = tx.lineCount,
            itemsSummary = tx.itemsSummarySnapshot,
            paymentMethod = tx.paymentMethod,
            status = tx.status,
            isRepeatCustomer = tx.isRepeatCustomerBill,
            cancellationReason = tx.cancellationReason,
            saleEpochMillis = tx.createdAt
        )
        val now = System.currentTimeMillis()
        return if (res.isSuccess) {
            dao.updateTransactionSyncStatus(tx.id, "SYNCED")
            dao.updateSyncQueueStatus("sync_${tx.id}", "SYNCED", 0, now)
            true
        } else {
            false
        }
    }

    suspend fun syncAllPendingToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val pending = dao.getPendingSyncItems()
        var syncedCount = 0
        val now = System.currentTimeMillis()

        for (item in pending) {
            try {
                val json = JSONObject(item.payloadJson)
                val success = when (item.entityType) {
                    "TRANSACTION" -> {
                        val res = cloudRepo.syncTransaction(
                            txId = json.getString("txId"),
                            billNumber = json.getString("billNumber"),
                            staffId = json.getString("staffId"),
                            staffName = json.getString("staffName"),
                            grandTotal = json.getDouble("grandTotal"),
                            lineCount = json.getInt("lineCount"),
                            itemsSummary = json.getString("itemsSummary"),
                            paymentMethod = json.getString("paymentMethod"),
                            status = json.getString("status"),
                            isRepeatCustomer = json.getBoolean("isRepeatCustomer"),
                            cancellationReason = json.optString("cancellationReason").takeIf { it.isNotBlank() },
                            saleEpochMillis = json.getLong("saleEpochMillis")
                        )
                        if (res.isSuccess) {
                            dao.updateTransactionSyncStatus(json.getString("txId"), "SYNCED")
                        }
                        res.isSuccess
                    }
                    "TRANSACTION_CANCEL" -> {
                        val res = cloudRepo.cancelCloudTransaction(
                            txId = json.getString("txId"),
                            reason = json.getString("reason")
                        )
                        if (res.isSuccess) {
                            dao.updateTransactionSyncStatus(json.getString("txId"), "SYNCED")
                        }
                        res.isSuccess
                    }
                    "VISITOR" -> {
                        val res = cloudRepo.syncVisitor(
                            visitorId = json.getString("visitorId"),
                            dateKey = json.getString("dateKey"),
                            delta = json.getInt("delta"),
                            staffId = json.getString("staffId"),
                            staffName = json.getString("staffName"),
                            correctionReason = json.optString("correctionReason").takeIf { it.isNotBlank() },
                            eventEpochMillis = json.getLong("eventEpochMillis")
                        )
                        if (res.isSuccess) {
                            dao.updateVisitorSyncStatus(json.getString("visitorId"), "SYNCED")
                        }
                        res.isSuccess
                    }
                    "FREEZER_SECTION" -> {
                        val res = cloudRepo.syncFreezerSection(
                            sectionId = json.getString("sectionId"),
                            freezerNumber = json.getInt("freezerNumber"),
                            sectionCode = json.getString("sectionCode"),
                            mappedProducts = json.getString("mappedProducts"),
                            manualNotes = json.getString("manualNotes"),
                            updatedByStaffName = json.getString("updatedByStaffName"),
                            updatedEpochMillis = json.getLong("updatedEpochMillis")
                        )
                        res.isSuccess
                    }
                    "PRODUCT" -> {
                        val res = cloudRepo.syncProduct(
                            productId = json.getString("productId"),
                            nameEn = json.getString("nameEn"),
                            nameBn = json.getString("nameBn"),
                            unitType = json.getString("unitType"),
                            currentRate = json.getDouble("currentRate"),
                            isRateConfigured = json.getBoolean("isRateConfigured"),
                            isActive = json.getBoolean("isActive")
                        )
                        res.isSuccess
                    }
                    else -> false
                }

                if (success) {
                    syncedCount++
                    dao.updateSyncQueueStatus(item.id, "SYNCED", item.retryCount, now)
                } else {
                    dao.updateSyncQueueStatus(item.id, "FAILED", item.retryCount + 1, now)
                }
            } catch (e: Exception) {
                dao.updateSyncQueueStatus(item.id, "FAILED", item.retryCount + 1, now)
            }
        }
        Result.success(syncedCount)
    }

    suspend fun exportCsvReport(
        context: Context,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        visitorsCount: Int,
        buyingCustomersCount: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(exportDir, "new_farm_product_${periodLabel.lowercase(Locale.US)}_report.csv")
            val sb = StringBuilder()
            sb.appendLine("NEW FARM PRODUCT - ASANSOL (50 Years of Trust. A Smarter Way Forward.)")
            sb.appendLine("Report Period,$periodLabel")
            sb.appendLine("Total Visitors,$visitorsCount")
            sb.appendLine("Estimated Buying Customers,$buyingCustomersCount")
            sb.appendLine("Note,Gross Revenue Only - Profit disabled until purchase costs are configured")
            sb.appendLine()
            sb.appendLine("Bill Number,Date,Staff,Payment Method,Status,Line Items,Grand Total (INR),Items Summary")
            transactions.forEach { tx ->
                val safeSummary = tx.itemsSummarySnapshot.replace(",", " |")
                sb.appendLine("${tx.billNumber},${tx.dateKey},${tx.staffNameSnapshot},${tx.paymentMethod},${tx.status},${tx.lineCount},${String.format(Locale.US, "%.2f", tx.grandTotal)},\"$safeSummary\"")
            }
            file.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportPdfReport(
        context: Context,
        periodLabel: String,
        revenue: Double,
        visitorsCount: Int,
        buyingCustomersCount: Int,
        completedBills: Int,
        productLines: Int,
        transactions: List<TransactionEntity>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(exportDir, "new_farm_product_${periodLabel.lowercase(Locale.US)}_report.pdf")
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(13, 37, 63)
                textSize = 18f
                isFakeBoldText = true
            }
            val subPaint = Paint().apply {
                color = Color.rgb(0, 137, 123)
                textSize = 11f
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(16, 32, 48)
                textSize = 10f
            }

            var y = 45f
            canvas.drawText("NEW FARM PRODUCT — ASANSOL, WB", 40f, y, titlePaint)
            y += 18f
            canvas.drawText("50 Years of Trust. A Smarter Way Forward. | $periodLabel Report", 40f, y, subPaint)
            y += 24f
            canvas.drawText("Gross Revenue: INR ${String.format(Locale.US, "%.2f", revenue)} (Profit calculation disabled until cost data added)", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Visitors: $visitorsCount | Est. Buying Customers: $buyingCustomersCount | Completed Bills: $completedBills | Product Lines: $productLines", 40f, y, bodyPaint)
            y += 24f
            canvas.drawText("Recent Bills:", 40f, y, titlePaint.apply { textSize = 13f })
            y += 18f

            transactions.take(22).forEach { tx ->
                if (y < 790f) {
                    val line = "${tx.billNumber} | ${tx.dateKey} | ${tx.staffNameSnapshot} | ${tx.status} | INR ${String.format(Locale.US, "%.2f", tx.grandTotal)} | ${tx.itemsSummarySnapshot.take(48)}"
                    canvas.drawText(line, 40f, y, bodyPaint)
                    y += 15f
                }
            }

            pdf.finishPage(page)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
