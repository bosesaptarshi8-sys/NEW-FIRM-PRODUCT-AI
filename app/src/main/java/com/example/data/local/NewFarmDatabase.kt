package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.security.MessageDigest

@Database(
    entities = [
        RoleEntity::class,
        UserEntity::class,
        StaffProfileEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        VisitorEntity::class,
        FreezerEntity::class,
        FreezerSectionEntity::class,
        FreezerPhotoEntity::class,
        SectionProductMappingEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NewFarmDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: NewFarmDatabase? = null

        fun getInstance(context: Context): NewFarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NewFarmDatabase::class.java,
                    "new_farm_product.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("nfp_salt_1976_$pin".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        suspend fun seedInitialCatalogIfNeeded(context: Context, dao: AppDao) {
            if (dao.getProductCount() > 0) return

            val now = System.currentTimeMillis()
            val pkg = context.packageName
            val sampleFreezerPhotoUri =
                "android.resource://$pkg/drawable/img_freezer_section_sample_1794392168908"

            // 1. Roles
            val roles = listOf(
                RoleEntity(
                    id = "role_owner",
                    code = "OWNER",
                    nameEn = "Owner (Kolkata / Asansol)",
                    nameBn = "মালিক (Owner)",
                    nameHi = "मालिक (Owner)",
                    canEditRates = true,
                    canCancelBills = true,
                    canViewOwnerReports = true,
                    canManageStaff = true
                ),
                RoleEntity(
                    id = "role_manager",
                    code = "MANAGER",
                    nameEn = "Family Member / Manager",
                    nameBn = "ম্যানেজার / পরিবার",
                    nameHi = "प्रबंधक / परिवार",
                    canEditRates = true,
                    canCancelBills = true,
                    canViewOwnerReports = true,
                    canManageStaff = false
                ),
                RoleEntity(
                    id = "role_staff",
                    code = "STAFF",
                    nameEn = "Shop Staff",
                    nameBn = "দোকান কর্মী",
                    nameHi = "दुकान कर्मचारी",
                    canEditRates = false,
                    canCancelBills = false,
                    canViewOwnerReports = false,
                    canManageStaff = false
                )
            )
            dao.insertRoles(roles)

            // 2. Users (SHA-256 salted PIN hashes — never plain text)
            val users = listOf(
                UserEntity(
                    id = "user_owner",
                    username = "owner",
                    roleId = "role_owner",
                    pinHash = hashPin("1976"),
                    createdAt = now
                ),
                UserEntity(
                    id = "user_manager",
                    username = "manager",
                    roleId = "role_manager",
                    pinHash = hashPin("2222"),
                    createdAt = now
                ),
                UserEntity(
                    id = "user_staff_1",
                    username = "staff1",
                    roleId = "role_staff",
                    pinHash = hashPin("1111"),
                    createdAt = now
                ),
                UserEntity(
                    id = "user_staff_2",
                    username = "staff2",
                    roleId = "role_staff",
                    pinHash = hashPin("3333"),
                    createdAt = now
                )
            )
            dao.insertUsers(users)

            // 3. Staff Profiles
            val profiles = listOf(
                StaffProfileEntity(
                    id = "staff_owner",
                    userId = "user_owner",
                    roleCode = "OWNER",
                    displayNameEn = "Owner (Saptarshi)",
                    displayNameBn = "মালিক (সপ্তর্ষি)",
                    displayNameHi = "मालिक (सप्तर्षि)",
                    badgeLabel = "OWNER • PIN 1976"
                ),
                StaffProfileEntity(
                    id = "staff_manager",
                    userId = "user_manager",
                    roleCode = "MANAGER",
                    displayNameEn = "Family / Shop Manager",
                    displayNameBn = "শপ ম্যানেজার (পরিবার)",
                    displayNameHi = "शॉप मैनेजर (परिवार)",
                    badgeLabel = "MANAGER • PIN 2222"
                ),
                StaffProfileEntity(
                    id = "staff_1",
                    userId = "user_staff_1",
                    roleCode = "STAFF",
                    displayNameEn = "Staff Member 1 (Counter A)",
                    displayNameBn = "কর্মী ১ (কাউন্টার A)",
                    displayNameHi = "कर्मचारी 1 (काउंटर A)",
                    badgeLabel = "STAFF 1 • PIN 1111"
                ),
                StaffProfileEntity(
                    id = "staff_2",
                    userId = "user_staff_2",
                    roleCode = "STAFF",
                    displayNameEn = "Staff Member 2 (Counter B)",
                    displayNameBn = "কর্মী ২ (কাউন্টার B)",
                    displayNameHi = "कर्मचारी 2 (काउंटर B)",
                    badgeLabel = "STAFF 2 • PIN 3333"
                )
            )
            dao.insertStaffProfiles(profiles)

            // 4. Initial 12 Selected Products (Never invent unspecified prices!)
            val products = listOf(
                ProductEntity(
                    id = "prod_salmon_fish",
                    sortOrder = 1,
                    nameEn = "Salmon Fish",
                    nameBn = "স্যামন মাছ (Salmon Fish)",
                    nameHi = "सैल्मन मछली (Salmon Fish)",
                    category = "FROZEN_FISH",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 500.0,
                    isRateConfigured = true,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_vetki_fry",
                    sortOrder = 2,
                    nameEn = "Vetki Fish Fry",
                    nameBn = "ভেটকি ফিশ ফ্রাই",
                    nameHi = "भेटकी फिश फ्राई",
                    category = "FROZEN_FISH",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 1200.0,
                    isRateConfigured = true,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_vetki_finger",
                    sortOrder = 3,
                    nameEn = "Vetki Fish Finger",
                    nameBn = "ভেটকি ফিশ ফিঙ্গার",
                    nameHi = "भेटकी फिश फिंगर",
                    category = "FROZEN_FISH",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 1200.0,
                    isRateConfigured = true,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_fish_fry",
                    sortOrder = 4,
                    nameEn = "Fish Fry (Regular)",
                    nameBn = "ফিশ ফ্রাই (রেগুলার)",
                    nameHi = "फिश फ्राई (रेगुलर)",
                    category = "FROZEN_FISH",
                    unitType = "PIECE",
                    allowDecimalQty = false,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_fish_finger",
                    sortOrder = 5,
                    nameEn = "Fish Finger (Regular)",
                    nameBn = "ফিশ ফিঙ্গার (রেগুলার)",
                    nameHi = "फिश फिंगर (रेगुलर)",
                    category = "FROZEN_FISH",
                    unitType = "PACKET",
                    allowDecimalQty = false,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_potato",
                    sortOrder = 6,
                    nameEn = "Potato",
                    nameBn = "আলু (Potato)",
                    nameHi = "आलू (Potato)",
                    category = "STAPLES",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_onion",
                    sortOrder = 7,
                    nameEn = "Onion",
                    nameBn = "পেঁয়াজ (Onion)",
                    nameHi = "प्याज (Onion)",
                    category = "STAPLES",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_garlic",
                    sortOrder = 8,
                    nameEn = "Garlic",
                    nameBn = "রসুন (Garlic)",
                    nameHi = "लहसुन (Garlic)",
                    category = "STAPLES",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_eggs",
                    sortOrder = 9,
                    nameEn = "Eggs",
                    nameBn = "ডিম (Eggs)",
                    nameHi = "अंडे (Eggs)",
                    category = "STAPLES",
                    unitType = "PIECE",
                    allowDecimalQty = false,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_rice",
                    sortOrder = 10,
                    nameEn = "Rice",
                    nameBn = "চাল (Rice)",
                    nameHi = "चावल (Rice)",
                    category = "STAPLES",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_dal",
                    sortOrder = 11,
                    nameEn = "Dal",
                    nameBn = "ডাল (Dal)",
                    nameHi = "दाल (Dal)",
                    category = "STAPLES",
                    unitType = "KG",
                    allowDecimalQty = true,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                ),
                ProductEntity(
                    id = "prod_ice_cream",
                    sortOrder = 12,
                    nameEn = "Ice Cream (Multiple Variants)",
                    nameBn = "আইসক্রিম (বিভিন্ন ভ্যারিয়েন্ট)",
                    nameHi = "आइसक्रीम (विभिन्न वेरिएंट)",
                    category = "ICE_CREAM",
                    unitType = "PACKET",
                    allowDecimalQty = false,
                    currentRate = 0.0,
                    isRateConfigured = false,
                    updatedAt = now,
                    updatedByStaffName = "Owner"
                )
            )
            dao.insertProducts(products)

            // 5. Ice Cream Variants
            val variants = listOf(
                ProductVariantEntity(
                    id = "var_ic_vanilla",
                    productId = "prod_ice_cream",
                    variantNameEn = "Vanilla Cup / Pack",
                    variantNameBn = "ভ্যানিলা কাপ / প্যাক",
                    rateOverride = 0.0,
                    isRateConfigured = false
                ),
                ProductVariantEntity(
                    id = "var_ic_butterscotch",
                    productId = "prod_ice_cream",
                    variantNameEn = "Butterscotch Cone / Family Pack",
                    variantNameBn = "বাটারস্কচ কোন / ফ্যামিলি প্যাক",
                    rateOverride = 0.0,
                    isRateConfigured = false
                ),
                ProductVariantEntity(
                    id = "var_ic_chocolate",
                    productId = "prod_ice_cream",
                    variantNameEn = "Chocolate Bar / Tub",
                    variantNameBn = "চকলেট বার / টাব",
                    rateOverride = 0.0,
                    isRateConfigured = false
                ),
                ProductVariantEntity(
                    id = "var_ic_kulfi",
                    productId = "prod_ice_cream",
                    variantNameEn = "Malai Kulfi Stick",
                    variantNameBn = "মালাই কুলফি স্টিক",
                    rateOverride = 0.0,
                    isRateConfigured = false
                )
            )
            dao.insertProductVariants(variants)

            // 6. Seven Deep Freezers (Freezer 1 .. Freezer 7)
            val freezerDescriptions = mapOf(
                1 to ("Ice Cream & Frozen Desserts" to "আইসক্রিম ও ডেজার্ট"),
                2 to ("Regular Fish Fry & Snacks" to "রেগুলার ফিশ ফ্রাই ও স্ন্যাকস"),
                3 to ("Premium Vetki & Salmon Fillets" to "প্রিমিয়াম ভেটকি ও স্যামন মাছ"),
                4 to ("Prawns & Sea Fish Cuts" to "চিংড়ি ও সামুদ্রিক মাছ"),
                5 to ("Frozen Green Peas, Corn & Veg" to "ফ্রোজেন কড়াইশুঁটি ও সুইট কর্ন"),
                6 to ("Bulk Party Packs & Ready Snacks" to "বাল্ক পার্টি প্যাক ও রেডি স্ন্যাকস"),
                7 to ("Backup Cold Storage & New Arrivals" to "ব্যাকআপ কোল্ড স্টোরেজ")
            )

            val freezers = (1..7).map { num ->
                val (descEn, _) = freezerDescriptions[num] ?: ("Deep Freezer $num" to "ডিপ ফ্রিজার $num")
                FreezerEntity(
                    id = "freezer_$num",
                    freezerNumber = num,
                    freezerNameEn = "Freezer $num",
                    freezerNameBn = "ফ্রিজার $num",
                    description = descEn,
                    lastUpdatedAt = now,
                    lastUpdatedByStaffName = "Owner (Saptarshi)"
                )
            }
            dao.insertFreezers(freezers)

            // 7. Sections A, B, C, D for each of the 7 Freezers
            val sections = mutableListOf<FreezerSectionEntity>()
            for (num in 1..7) {
                for (code in listOf("Section A", "Section B", "Section C", "Section D")) {
                    val secId = "freezer_${num}_sec_${code.last().lowercaseChar()}"
                    val isFreezer3SecB = (num == 3 && code == "Section B")
                    val isFreezer3SecA = (num == 3 && code == "Section A")
                    val isFreezer1SecA = (num == 1 && code == "Section A")
                    val isFreezer2SecA = (num == 2 && code == "Section A")

                    val desc = when {
                        isFreezer3SecB -> "Top middle basket — Vetki Fish Finger & Vetki Fish Fry"
                        isFreezer3SecA -> "Left basket — Vacuum-sealed Salmon Fish (₹500/kg)"
                        isFreezer1SecA -> "Front glass tray — Ice Cream Cups, Cones & Kulfi"
                        isFreezer2SecA -> "Upper basket — Regular Fish Fry & Fish Finger packets"
                        else -> "$code compartment in Freezer $num"
                    }
                    val notes = when {
                        isFreezer3SecB -> "Vetki Fish Finger (₹1,200/kg) on left divider; Vetki Fish Fry (₹1,200/kg) in center basket."
                        isFreezer3SecA -> "Salmon Fish 1kg and 500g packs stacked on top rack."
                        isFreezer1SecA -> "Vanilla, Butterscotch, Chocolate & Kulfi variants sorted by tray."
                        isFreezer2SecA -> "Fast-moving evening snacks tray."
                        else -> "Tap 'Update Section' to add product placement notes or capture photo."
                    }

                    sections.add(
                        FreezerSectionEntity(
                            id = secId,
                            freezerId = "freezer_$num",
                            freezerNumber = num,
                            sectionCode = code,
                            description = desc,
                            manualNotes = notes,
                            currentPhotoUri = if (isFreezer3SecB || isFreezer3SecA) sampleFreezerPhotoUri else null,
                            lastUpdatedAt = now,
                            updatedByStaffId = "staff_owner",
                            updatedByStaffName = "Owner (Saptarshi)"
                        )
                    )
                }
            }
            dao.insertFreezerSections(sections)

            // Seed initial photo history record for Freezer 3 Section B
            dao.insertFreezerPhoto(
                FreezerPhotoEntity(
                    id = "photo_init_f3_sec_b",
                    freezerId = "freezer_3",
                    sectionId = "freezer_3_sec_b",
                    photoUri = sampleFreezerPhotoUri,
                    notesSnapshot = "Initial verified top-basket photo: Vetki Fish Finger & Vetki Fish Fry",
                    capturedAt = now,
                    uploadedByStaffId = "staff_owner",
                    uploadedByStaffName = "Owner (Saptarshi)",
                    uploadStatus = "LOCAL_SAVED",
                    isCurrent = true
                )
            )

            // 8. Initial Section Product Mappings (so searching 'Vetki Fish Finger' immediately finds Freezer 3 -> Section B)
            val initialMappings = listOf(
                SectionProductMappingEntity(
                    id = "map_vetki_finger",
                    freezerId = "freezer_3",
                    freezerNumber = 3,
                    sectionId = "freezer_3_sec_b",
                    sectionCode = "Section B",
                    productName = "Vetki Fish Finger",
                    stockNote = "Left basket top layer • ₹1,200/kg",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                ),
                SectionProductMappingEntity(
                    id = "map_vetki_fry",
                    freezerId = "freezer_3",
                    freezerNumber = 3,
                    sectionId = "freezer_3_sec_b",
                    sectionCode = "Section B",
                    productName = "Vetki Fish Fry",
                    stockNote = "Center basket top layer • ₹1,200/kg",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                ),
                SectionProductMappingEntity(
                    id = "map_salmon_fish",
                    freezerId = "freezer_3",
                    freezerNumber = 3,
                    sectionId = "freezer_3_sec_a",
                    sectionCode = "Section A",
                    productName = "Salmon Fish",
                    stockNote = "Left compartment vacuum packs • ₹500/kg",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                ),
                SectionProductMappingEntity(
                    id = "map_ice_cream",
                    freezerId = "freezer_1",
                    freezerNumber = 1,
                    sectionId = "freezer_1_sec_a",
                    sectionCode = "Section A",
                    productName = "Ice Cream",
                    stockNote = "Front display basket • All variants",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                ),
                SectionProductMappingEntity(
                    id = "map_fish_fry_reg",
                    freezerId = "freezer_2",
                    freezerNumber = 2,
                    sectionId = "freezer_2_sec_a",
                    sectionCode = "Section A",
                    productName = "Fish Fry",
                    stockNote = "Upper snack basket",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                ),
                SectionProductMappingEntity(
                    id = "map_fish_finger_reg",
                    freezerId = "freezer_2",
                    freezerNumber = 2,
                    sectionId = "freezer_2_sec_a",
                    sectionCode = "Section A",
                    productName = "Fish Finger",
                    stockNote = "Upper snack basket right side",
                    updatedAt = now,
                    updatedByStaffName = "Owner (Saptarshi)"
                )
            )
            dao.insertSectionProductMappings(initialMappings)
        }
    }
}
