package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val id: String,
    val code: String, // OWNER, MANAGER, STAFF
    val nameEn: String,
    val nameBn: String,
    val nameHi: String,
    val canEditRates: Boolean,
    val canCancelBills: Boolean,
    val canViewOwnerReports: Boolean,
    val canManageStaff: Boolean
)

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("roleId"), Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val roleId: String,
    val pinHash: String, // SHA-256 hash, never plain text
    val googleUid: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "staff_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"], unique = true)]
)
data class StaffProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val roleCode: String, // OWNER, MANAGER, STAFF
    val displayNameEn: String,
    val displayNameBn: String,
    val displayNameHi: String,
    val badgeLabel: String,
    val isActive: Boolean = true,
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index("nameEn"), Index("category")]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val nameEn: String,
    val nameBn: String,
    val nameHi: String,
    val category: String, // STAPLES, FROZEN_FISH, ICE_CREAM
    val unitType: String, // KG, GRAM, PIECE, PACKET
    val allowDecimalQty: Boolean,
    val currentRate: Double, // 0.0 when unconfigured
    val isRateConfigured: Boolean,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedByStaffName: String = "System"
)

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class ProductVariantEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val variantNameEn: String,
    val variantNameBn: String,
    val rateOverride: Double, // 0.0 if unset
    val isRateConfigured: Boolean,
    val isActive: Boolean = true
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["billNumber"], unique = true),
        Index("createdAt"),
        Index("dateKey"),
        Index("staffId"),
        Index("status")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String, // Stable UUID for idempotent sync
    val billNumber: String,
    val createdAt: Long,
    val dateKey: String, // YYYY-MM-DD
    val staffId: String,
    val staffNameSnapshot: String,
    val grandTotal: Double,
    val lineCount: Int,
    val itemsSummarySnapshot: String,
    val paymentMethod: String, // CASH, UPI, CARD
    val status: String, // COMPLETED, CANCELLED
    val isRepeatCustomerBill: Boolean = false,
    val customerRefOptional: String? = null,
    val cancellationReason: String? = null,
    val cancelledByStaffName: String? = null,
    val cancelledAt: Long? = null,
    val syncStatus: String = "PENDING" // PENDING, SYNCED, FAILED
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("transactionId"), Index("productId")]
)
data class TransactionItemEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val productId: String,
    val variantId: String? = null,
    val productNameSnapshot: String,
    val productNameBnSnapshot: String,
    val quantity: Double,
    val unitTypeSnapshot: String,
    val unitPriceSnapshot: Double, // Historical rate locked at sale time
    val lineTotal: Double
)

@Entity(
    tableName = "visitors",
    indices = [Index("timestamp"), Index("dateKey"), Index("staffId")]
)
data class VisitorEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val dateKey: String, // YYYY-MM-DD
    val delta: Int, // +1 arrival, -1 audited correction
    val staffId: String,
    val staffNameSnapshot: String,
    val correctionReason: String? = null,
    val syncStatus: String = "PENDING"
)

@Entity(
    tableName = "freezers",
    indices = [Index(value = ["freezerNumber"], unique = true)]
)
data class FreezerEntity(
    @PrimaryKey val id: String,
    val freezerNumber: Int, // 1..7
    val freezerNameEn: String,
    val freezerNameBn: String,
    val description: String,
    val lastUpdatedAt: Long,
    val lastUpdatedByStaffName: String
)

@Entity(
    tableName = "freezer_sections",
    foreignKeys = [
        ForeignKey(
            entity = FreezerEntity::class,
            parentColumns = ["id"],
            childColumns = ["freezerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("freezerId"), Index("sectionCode")]
)
data class FreezerSectionEntity(
    @PrimaryKey val id: String,
    val freezerId: String,
    val freezerNumber: Int,
    val sectionCode: String, // Section A, Section B, Section C, Section D...
    val description: String,
    val manualNotes: String,
    val currentPhotoUri: String? = null,
    val lastUpdatedAt: Long,
    val updatedByStaffId: String,
    val updatedByStaffName: String,
    val syncStatus: String = "PENDING"
)

@Entity(
    tableName = "freezer_photos",
    foreignKeys = [
        ForeignKey(
            entity = FreezerSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId"), Index("freezerId"), Index("capturedAt")]
)
data class FreezerPhotoEntity(
    @PrimaryKey val id: String,
    val freezerId: String,
    val sectionId: String,
    val photoUri: String,
    val notesSnapshot: String,
    val capturedAt: Long,
    val uploadedByStaffId: String,
    val uploadedByStaffName: String,
    val uploadStatus: String = "LOCAL_SAVED", // LOCAL_SAVED, QUEUED, SYNCED
    val isCurrent: Boolean = true
)

@Entity(
    tableName = "section_product_mappings",
    foreignKeys = [
        ForeignKey(
            entity = FreezerSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId"), Index("freezerId"), Index("productName")]
)
data class SectionProductMappingEntity(
    @PrimaryKey val id: String,
    val freezerId: String,
    val freezerNumber: Int,
    val sectionId: String,
    val sectionCode: String,
    val productName: String,
    val stockNote: String,
    val updatedAt: Long,
    val updatedByStaffName: String
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("timestamp"), Index("actionType")]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val actorStaffId: String,
    val actorName: String,
    val actorRole: String,
    val actionType: String, // BILL_CREATED, BILL_CANCELLED, RATE_UPDATED, FREEZER_UPDATED, VISITOR_CORRECTED, VOICE_BILL_CONFIRMED
    val entityType: String,
    val entityId: String,
    val summary: String,
    val syncStatus: String = "PENDING"
)

@Entity(
    tableName = "sync_queue",
    indices = [Index("status"), Index("createdAt"), Index(value = ["entityType", "entityId", "operation"], unique = true)]
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String, // TRANSACTION, TRANSACTION_CANCEL, VISITOR, FREEZER_SECTION, PRODUCT, AUDIT_LOG
    val entityId: String,
    val operation: String, // UPSERT, CANCEL
    val payloadJson: String,
    val retryCount: Int = 0,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val lastAttemptAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
