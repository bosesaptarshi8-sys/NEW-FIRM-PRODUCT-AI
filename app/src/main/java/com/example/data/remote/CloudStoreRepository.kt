package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class StoreTransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val billNumber: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val grandTotal: Double = 0.0,
    val lineCount: Long = 1L,
    val itemsSummary: String = "",
    val paymentMethod: String = "CASH",
    val status: String = "COMPLETED",
    val isRepeatCustomer: Boolean = false,
    val cancellationReason: String? = null,
    val saleEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreVisitorDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val delta: Long = 1L,
    val staffId: String = "",
    val staffName: String = "",
    val correctionReason: String? = null,
    val eventEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreFreezerSectionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val freezerNumber: Long = 1L,
    val sectionCode: String = "Section A",
    val mappedProducts: String = "",
    val manualNotes: String = "",
    val updatedByStaffName: String = "",
    val updatedEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreProductDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val unitType: String = "KG",
    val currentRate: Double = 0.0,
    val isRateConfigured: Boolean = false,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreAuditLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val summary: String = "",
    val eventEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

class CloudStoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Cloud Firestore.")
    }

    suspend fun syncTransaction(
        txId: String,
        billNumber: String,
        staffId: String,
        staffName: String,
        grandTotal: Double,
        lineCount: Int,
        itemsSummary: String,
        paymentMethod: String,
        status: String,
        isRepeatCustomer: Boolean,
        cancellationReason: String?,
        saleEpochMillis: Long
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_transactions").document(txId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "billNumber" to billNumber,
            "staffId" to staffId,
            "staffName" to staffName,
            "grandTotal" to grandTotal,
            "lineCount" to lineCount.toLong(),
            "itemsSummary" to itemsSummary.take(4000).ifBlank { "Item" },
            "paymentMethod" to paymentMethod,
            "status" to status,
            "isRepeatCustomer" to isRepeatCustomer,
            "saleEpochMillis" to saleEpochMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!cancellationReason.isNullOrBlank()) {
            payload["cancellationReason"] = cancellationReason.take(500)
        }
        return try {
            docRef.set(payload).await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelCloudTransaction(txId: String, reason: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_transactions").document(txId)
        val updates = mapOf(
            "status" to "CANCELLED",
            "cancellationReason" to reason.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTransactionById(ownerUid: String, txId: String): Result<StoreTransactionDoc?> {
        val docRef = db.collection("users").document(ownerUid)
            .collection("store_transactions").document(txId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    StoreTransactionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeTransactions(): Flow<List<StoreTransactionDoc>> = flow {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/store_transactions"
        emitAll(
            db.collection("users").document(uid).collection("store_transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StoreTransactionDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncVisitor(
        visitorId: String,
        dateKey: String,
        delta: Int,
        staffId: String,
        staffName: String,
        correctionReason: String?,
        eventEpochMillis: Long
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_visitors").document(visitorId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "dateKey" to dateKey,
            "delta" to delta.toLong(),
            "staffId" to staffId,
            "staffName" to staffName,
            "eventEpochMillis" to eventEpochMillis,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!correctionReason.isNullOrBlank()) {
            payload["correctionReason"] = correctionReason.take(500)
        }
        return try {
            docRef.set(payload).await()
            Result.success(visitorId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncFreezerSection(
        sectionId: String,
        freezerNumber: Int,
        sectionCode: String,
        mappedProducts: String,
        manualNotes: String,
        updatedByStaffName: String,
        updatedEpochMillis: Long
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_freezers").document(sectionId)
        val payload = mapOf(
            "userId" to uid,
            "freezerNumber" to freezerNumber.toLong(),
            "sectionCode" to sectionCode.take(40),
            "mappedProducts" to mappedProducts.take(2000),
            "manualNotes" to manualNotes.take(2000),
            "updatedByStaffName" to updatedByStaffName.take(100).ifBlank { "Staff" },
            "updatedEpochMillis" to updatedEpochMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(sectionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncProduct(
        productId: String,
        nameEn: String,
        nameBn: String,
        unitType: String,
        currentRate: Double,
        isRateConfigured: Boolean,
        isActive: Boolean
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_products").document(productId)
        val payload = mapOf(
            "userId" to uid,
            "nameEn" to nameEn.take(120),
            "nameBn" to nameBn.take(120),
            "unitType" to unitType,
            "currentRate" to currentRate,
            "isRateConfigured" to isRateConfigured,
            "isActive" to isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncAuditLog(
        logId: String,
        actorName: String,
        actorRole: String,
        actionType: String,
        summary: String,
        eventEpochMillis: Long
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("store_audit_logs").document(logId)
        val payload = mapOf(
            "userId" to uid,
            "actorName" to actorName.take(100),
            "actorRole" to actorRole.take(40),
            "actionType" to actionType.take(80),
            "summary" to summary.take(2000),
            "eventEpochMillis" to eventEpochMillis,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(logId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
