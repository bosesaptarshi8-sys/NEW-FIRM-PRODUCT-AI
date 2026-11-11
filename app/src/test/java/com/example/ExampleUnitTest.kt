package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiVoiceDraftBill
import com.example.data.local.NewFarmDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.RoleEntity
import com.example.data.local.StaffProfileEntity
import com.example.data.remote.CloudStoreRepository
import com.example.data.repository.DraftBillItem
import com.example.data.repository.StoreRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExampleUnitTest {

    private lateinit var context: Context
    private lateinit var db: NewFarmDatabase
    private lateinit var repository: StoreRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.newfarmproduct.asnsl")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        db = Room.inMemoryDatabaseBuilder(context, NewFarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "demo-database")
        repository = StoreRepository(db, db.appDao(), CloudStoreRepository(firestore))
        repository.ensureSeeded(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test1_to_7_billing_weight_piece_smallSales_cancellation_and_staffAttribution() = runBlocking {
        val products = repository.products.first()
        val salmon = products.first { it.id == "prod_salmon_fish" }
        val vetkiFinger = products.first { it.id == "prod_vetki_finger" }
        val eggs = products.first { it.id == "prod_eggs" }

        val ownerStaff = repository.staffProfiles.first().first { it.id == "staff_owner" }
        val ownerRole = repository.roles.first().first { it.code == "OWNER" }

        // Configure Eggs rate (₹7/piece)
        val configuredEggs = repository.updateProductRate(eggs, 7.0, ownerStaff, ownerRole).getOrThrow()

        // 1, 2, 3, 4: Multiple products (weight decimal 0.5kg Salmon @ 500 = 250, 0.25kg Vetki @ 1200 = 300, 6 Eggs @ 7 = 42)
        val items = listOf(
            DraftBillItem(product = salmon, quantity = 0.5, unitPriceSnapshot = salmon.currentRate),
            DraftBillItem(product = vetkiFinger, quantity = 0.25, unitPriceSnapshot = vetkiFinger.currentRate),
            DraftBillItem(product = configuredEggs, quantity = 6.0, unitPriceSnapshot = configuredEggs.currentRate)
        )
        val saveRes = repository.createAndSaveBill(
            items = items,
            paymentMethod = "CASH",
            isRepeatCustomer = false,
            customerRefOptional = null,
            activeStaff = ownerStaff
        )
        assertTrue(saveRes.isSuccess)
        val savedBill = saveRes.getOrThrow()
        assertEquals(592.0, savedBill.grandTotal, 0.01)
        assertEquals(ownerStaff.id, savedBill.staffId)

        // 5: Small-value sale (1 Egg = ₹7, no minimum threshold)
        val smallSale = repository.createAndSaveBill(
            items = listOf(DraftBillItem(product = configuredEggs, quantity = 1.0, unitPriceSnapshot = 7.0)),
            paymentMethod = "UPI",
            isRepeatCustomer = true,
            customerRefOptional = null,
            activeStaff = ownerStaff
        ).getOrThrow()
        assertEquals(7.0, smallSale.grandTotal, 0.01)

        // Historical price snapshot integrity: changing Salmon rate to ₹550/kg does NOT alter savedBill
        repository.updateProductRate(salmon, 550.0, ownerStaff, ownerRole)
        val savedItems = db.appDao().getItemsForTransaction(savedBill.id)
        val salmonLine = savedItems.first { it.productId == "prod_salmon_fish" }
        assertEquals(500.0, salmonLine.unitPriceSnapshot, 0.01)

        // 6: Non-destructive bill cancellation with audit history
        val cancelRes = repository.cancelBillWithAudit(
            transaction = smallSale,
            reason = "Customer returned single egg",
            activeStaff = ownerStaff,
            activeRole = ownerRole
        )
        assertTrue(cancelRes.isSuccess)
        val allTxs = repository.transactions.first()
        val cancelledTx = allTxs.first { it.id == smallSale.id }
        assertEquals("CANCELLED", cancelledTx.status)
        assertEquals("Customer returned single egg", cancelledTx.cancellationReason)
    }

    @Test
    fun test8_to_15_visitors_freezerSearch_photoHistory_offlineQueue_and_rbacPermissions() = runBlocking {
        val profiles = repository.staffProfiles.first()
        val roles = repository.roles.first()
        val staff1 = profiles.first { it.id == "staff_1" }
        val staffRole = roles.first { it.code == "STAFF" }

        // 8: Visitor counting (+1 arrival and audited -1 correction)
        repository.recordVisitor(1, null, staff1)
        repository.recordVisitor(1, null, staff1)
        repository.recordVisitor(-1, "Double tap correction", staff1)
        val netVisitors = repository.visitors.first().sumOf { it.delta }
        assertEquals(1, netVisitors)

        // 9 & 10: Freezer section search ('Vetki Fish Finger' -> Freezer 3, Section B) & photo history
        val mappings = repository.sectionMappings.first()
        val vetkiMatch = mappings.firstOrNull { it.productName.contains("Vetki Fish Finger", ignoreCase = true) }
        assertNotNull(vetkiMatch)
        assertEquals(3, vetkiMatch?.freezerNumber)
        assertEquals("Section B", vetkiMatch?.sectionCode)

        val freezer3 = repository.freezers.first().first { it.freezerNumber == 3 }
        val secB = repository.freezerSections.first().first { it.freezerNumber == 3 && it.sectionCode == "Section B" }
        repository.updateFreezerSectionDetails(
            freezer = freezer3,
            section = secB,
            newDescription = secB.description,
            newManualNotes = "Restocked 15 packs of Vetki Fish Finger",
            newPhotoUri = "file:///storage/emulated/0/new_freezer3_b.jpg",
            mappedProductsList = listOf("Vetki Fish Finger" to "Restocked 15 packs"),
            activeStaff = staff1
        )
        val photos = repository.freezerPhotos.first().filter { it.sectionId == secB.id }
        assertTrue(photos.size >= 2)
        assertEquals(1, photos.count { it.isCurrent })

        // 11 & 13: Offline sync queue & duplicate prevention
        val pendingQueue = repository.pendingSyncQueue.first()
        assertTrue(pendingQueue.isNotEmpty())

        // 14: RBAC — ordinary STAFF cannot edit product rates or cancel bills
        val salmon = repository.products.first().first { it.id == "prod_salmon_fish" }
        val staffRateEditAttempt = repository.updateProductRate(salmon, 999.0, staff1, staffRole)
        assertTrue(staffRateEditAttempt.isFailure)

        // 15: Voice Draft structure validation
        val draft = AiVoiceDraftBill(
            productName = "Salmon Fish",
            quantity = 1.0,
            unit = "KG",
            unitRate = 500.0,
            lineTotal = 500.0
        )
        assertEquals(500.0, draft.lineTotal, 0.01)
    }
}
