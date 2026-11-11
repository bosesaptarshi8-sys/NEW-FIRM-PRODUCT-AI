package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Roles & Staff
    @Query("SELECT * FROM roles")
    fun observeRoles(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM staff_profiles WHERE isActive = 1 ORDER BY id ASC")
    fun observeStaffProfiles(): Flow<List<StaffProfileEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffProfiles(profiles: List<StaffProfileEntity>)

    @Query("UPDATE staff_profiles SET lastActiveAt = :timestamp WHERE id = :staffId")
    suspend fun touchStaffActivity(staffId: String, timestamp: Long)

    // Products & Variants
    @Query("SELECT * FROM products ORDER BY sortOrder ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product_variants WHERE isActive = 1")
    fun observeProductVariants(): Flow<List<ProductVariantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductVariants(variants: List<ProductVariantEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    // Transactions & Items
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_items ORDER BY id ASC")
    fun observeAllTransactionItems(): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    suspend fun getItemsForTransaction(txId: String): List<TransactionItemEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE dateKey = :dateKey")
    suspend fun getTransactionCountForDate(dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query(
        """
        UPDATE transactions 
        SET status = 'CANCELLED', 
            cancellationReason = :reason, 
            cancelledByStaffName = :staffName, 
            cancelledAt = :cancelledAt,
            syncStatus = 'PENDING'
        WHERE id = :txId
        """
    )
    suspend fun markTransactionCancelled(
        txId: String,
        reason: String,
        staffName: String,
        cancelledAt: Long
    )

    @Query("UPDATE transactions SET syncStatus = :syncStatus WHERE id = :txId")
    suspend fun updateTransactionSyncStatus(txId: String, syncStatus: String)

    // Visitors
    @Query("SELECT * FROM visitors ORDER BY timestamp DESC")
    fun observeVisitors(): Flow<List<VisitorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitor(visitor: VisitorEntity)

    @Query("UPDATE visitors SET syncStatus = :syncStatus WHERE id = :visitorId")
    suspend fun updateVisitorSyncStatus(visitorId: String, syncStatus: String)

    // Freezers, Sections, Photos, Mappings
    @Query("SELECT * FROM freezers ORDER BY freezerNumber ASC")
    fun observeFreezers(): Flow<List<FreezerEntity>>

    @Query("SELECT * FROM freezer_sections ORDER BY freezerNumber ASC, sectionCode ASC")
    fun observeFreezerSections(): Flow<List<FreezerSectionEntity>>

    @Query("SELECT * FROM freezer_photos ORDER BY capturedAt DESC")
    fun observeFreezerPhotos(): Flow<List<FreezerPhotoEntity>>

    @Query("SELECT * FROM section_product_mappings ORDER BY updatedAt DESC")
    fun observeSectionProductMappings(): Flow<List<SectionProductMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreezers(freezers: List<FreezerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreezerSections(sections: List<FreezerSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreezerSection(section: FreezerSectionEntity)

    @Update
    suspend fun updateFreezer(freezer: FreezerEntity)

    @Update
    suspend fun updateFreezerSection(section: FreezerSectionEntity)

    @Query("UPDATE freezer_photos SET isCurrent = 0 WHERE sectionId = :sectionId")
    suspend fun archivePreviousSectionPhotos(sectionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreezerPhoto(photo: FreezerPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionProductMappings(mappings: List<SectionProductMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionProductMapping(mapping: SectionProductMappingEntity)

    @Query("DELETE FROM section_product_mappings WHERE id = :mappingId")
    suspend fun deleteSectionProductMapping(mappingId: String)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Sync Queue
    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    fun observePendingSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingSyncItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastAttemptAt = :attemptAt WHERE id = :id")
    suspend fun updateSyncQueueStatus(id: String, status: String, retryCount: Int, attemptAt: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
