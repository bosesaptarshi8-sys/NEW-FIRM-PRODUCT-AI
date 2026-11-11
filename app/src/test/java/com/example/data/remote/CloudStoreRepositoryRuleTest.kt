package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudStoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncTransaction_and_observeTransactions_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(OWNER_EMAIL)
        val repository = CloudStoreRepository(firestore)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "")}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncTransaction(
                txId = txId,
                billNumber = "NFP-20261111-0001",
                staffId = "staff_owner",
                staffName = "Shop Owner",
                grandTotal = 1700.0,
                lineCount = 2,
                itemsSummary = "Salmon Fish 1kg @ 500, Vetki Fish Finger 1kg @ 1200",
                paymentMethod = "CASH",
                status = "COMPLETED",
                isRepeatCustomer = false,
                cancellationReason = null,
                saleEpochMillis = System.currentTimeMillis()
            )
        }
        assertTrue("Expected syncTransaction to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == txId } }
        }
        assertTrue(emitted.any { it.id == txId && it.billNumber == "NFP-20261111-0001" })

        val cancelResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.cancelCloudTransaction(txId, "Customer requested correction")
        }
        assertTrue(cancelResult.isSuccess)
    }

    @Test
    fun getTransactionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL)
        val ownerRepo = CloudStoreRepository(firestore)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "")}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.syncTransaction(
                txId = txId,
                billNumber = "NFP-20261111-0002",
                staffId = "staff_1",
                staffName = "Staff 1",
                grandTotal = 500.0,
                lineCount = 1,
                itemsSummary = "Salmon Fish 1kg @ 500",
                paymentMethod = "UPI",
                status = "COMPLETED",
                isRepeatCustomer = false,
                cancellationReason = null,
                saleEpochMillis = System.currentTimeMillis()
            )
        }
        assertTrue(createResult.isSuccess)

        signInTestUser(INTRUDER_EMAIL)
        val intruderRepo = CloudStoreRepository(firestore)
        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            intruderRepo.getTransactionById(ownerUid, txId)
        }
        assertTrue(readResult.isFailure)
        val fsEx = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudStoreRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions().first()
            }
            fail("Expected unauthenticated observeTransactions to throw PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val OWNER_EMAIL = "owner@newfarmproduct.com"
        const val INTRUDER_EMAIL = "intruder@example.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
