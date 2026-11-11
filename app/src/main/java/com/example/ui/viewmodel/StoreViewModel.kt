package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAssistantResponse
import com.example.data.ai.AiVoiceDraftBill
import com.example.data.ai.NewFarmAiAssistant
import com.example.data.local.AuditLogEntity
import com.example.data.local.FreezerEntity
import com.example.data.local.FreezerPhotoEntity
import com.example.data.local.FreezerSectionEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ProductVariantEntity
import com.example.data.local.RoleEntity
import com.example.data.local.SectionProductMappingEntity
import com.example.data.local.StaffProfileEntity
import com.example.data.local.SyncQueueEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemEntity
import com.example.data.local.VisitorEntity
import com.example.data.repository.DraftBillItem
import com.example.data.repository.StoreRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppLanguage(val label: String) {
    EN_BN("EN + বাংলা"),
    EN_HI("EN + हिन्दी"),
    EN_ONLY("English")
}

enum class ReportPeriod(val labelEn: String, val labelBn: String) {
    DAILY("Daily", "আজকের রিপোর্ট"),
    WEEKLY("Weekly (7 Days)", "সাপ্তাহিক রিপোর্ট"),
    MONTHLY("Monthly (30 Days)", "মাসিক রিপোর্ট")
}

data class ChatTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val responseMeta: AiAssistantResponse? = null
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Thinking : AiUiState
    data class Ready(val lastResponse: AiAssistantResponse) : AiUiState
    data class Error(val message: String) : AiUiState
}

class StoreViewModel(
    private val repository: StoreRepository,
    private val aiAssistant: NewFarmAiAssistant = NewFarmAiAssistant()
) : ViewModel() {

    val roles: StateFlow<List<RoleEntity>> = repository.roles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffProfiles: StateFlow<List<StaffProfileEntity>> = repository.staffProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productVariants: StateFlow<List<ProductVariantEntity>> = repository.productVariants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionItems: StateFlow<List<TransactionItemEntity>> = repository.transactionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visitors: StateFlow<List<VisitorEntity>> = repository.visitors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val freezers: StateFlow<List<FreezerEntity>> = repository.freezers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val freezerSections: StateFlow<List<FreezerSectionEntity>> = repository.freezerSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val freezerPhotos: StateFlow<List<FreezerPhotoEntity>> = repository.freezerPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sectionMappings: StateFlow<List<SectionProductMappingEntity>> = repository.sectionMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncQueue: StateFlow<List<SyncQueueEntity>> = repository.pendingSyncQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLanguage = MutableStateFlow(AppLanguage.EN_BN)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _activeStaffId = MutableStateFlow("staff_owner")
    val activeStaffId: StateFlow<String> = _activeStaffId.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    private val _lastCloudSyncTimeMillis = MutableStateFlow<Long?>(null)
    val lastCloudSyncTimeMillis: StateFlow<Long?> = _lastCloudSyncTimeMillis.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // POS Billing Draft Cart
    private val _draftCart = MutableStateFlow<List<DraftBillItem>>(emptyList())
    val draftCart: StateFlow<List<DraftBillItem>> = _draftCart.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("CASH")
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    private val _isRepeatCustomerBill = MutableStateFlow(false)
    val isRepeatCustomerBill: StateFlow<Boolean> = _isRepeatCustomerBill.asStateFlow()

    private val _customerReference = MutableStateFlow("")
    val customerReference: StateFlow<String> = _customerReference.asStateFlow()

    // Freezer Finder State
    private val _freezerSearchQuery = MutableStateFlow("")
    val freezerSearchQuery: StateFlow<String> = _freezerSearchQuery.asStateFlow()

    private val _selectedFreezerNumber = MutableStateFlow(3) // Default highlight Freezer 3
    val selectedFreezerNumber: StateFlow<Int> = _selectedFreezerNumber.asStateFlow()

    // NEW FARM AI Personal Assistant State
    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    private val _pendingVoiceDraftBill = MutableStateFlow<AiVoiceDraftBill?>(null)
    val pendingVoiceDraftBill: StateFlow<AiVoiceDraftBill?> = _pendingVoiceDraftBill.asStateFlow()

    private val _isWakeWordListeningEnabled = MutableStateFlow(false)
    val isWakeWordListeningEnabled: StateFlow<Boolean> = _isWakeWordListeningEnabled.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatTurn>>(
        listOf(
            ChatTurn(
                isUser = false,
                text = "নমস্কার! আমি NEW FARM AI (Gemini 3.8 Flash)। বলুন “হাই নিউ ফর্ম প্রোডাক্ট” বা নিচের যেকোনো প্রশ্ন ট্যাপ করুন — যেমন: “আজ কত বিক্রি হয়েছে?”, “Vetki Fish Finger কোথায় আছে?”, বা “এক কেজি Salmon Fish বিক্রি হয়েছে”।"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatTurn>> = _chatHistory.asStateFlow()

    fun initializeCatalog(context: Context) {
        viewModelScope.launch {
            repository.ensureSeeded(context)
        }
    }

    fun cycleLanguage() {
        _selectedLanguage.value = when (_selectedLanguage.value) {
            AppLanguage.EN_BN -> AppLanguage.EN_HI
            AppLanguage.EN_HI -> AppLanguage.EN_ONLY
            AppLanguage.EN_ONLY -> AppLanguage.EN_BN
        }
    }

    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
        val modeLabel = if (_isDemoMode.value) {
            "Demo Mode enabled — displaying clearly labeled sample metrics without modifying real shop records."
        } else {
            "Switched back to Live Shop Data."
        }
        _bannerMessage.value = modeLabel
    }

    fun setReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun getActiveStaff(): StaffProfileEntity {
        val list = staffProfiles.value
        return list.find { it.id == _activeStaffId.value }
            ?: list.firstOrNull()
            ?: StaffProfileEntity(
                id = "staff_owner",
                userId = "user_owner",
                roleCode = "OWNER",
                displayNameEn = "Owner (Saptarshi)",
                displayNameBn = "মালিক (সপ্তর্ষি)",
                displayNameHi = "मालिक (सप्तर्षि)",
                badgeLabel = "OWNER"
            )
    }

    fun getActiveRole(): RoleEntity {
        val staff = getActiveStaff()
        return roles.value.find { it.code == staff.roleCode }
            ?: RoleEntity(
                id = "role_owner",
                code = staff.roleCode,
                nameEn = staff.roleCode,
                nameBn = staff.displayNameBn,
                nameHi = staff.displayNameHi,
                canEditRates = staff.roleCode != "STAFF",
                canCancelBills = staff.roleCode != "STAFF",
                canViewOwnerReports = staff.roleCode != "STAFF",
                canManageStaff = staff.roleCode == "OWNER"
            )
    }

    fun switchStaffWithPin(
        targetStaff: StaffProfileEntity,
        enteredPin: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val verified = repository.verifyStaffPin(targetStaff, enteredPin)
            if (verified) {
                _activeStaffId.value = targetStaff.id
                val msg = "Switched operator to ${targetStaff.displayNameEn} (${targetStaff.roleCode})"
                _bannerMessage.value = msg
                onResult(true, msg)
            } else {
                onResult(false, "Incorrect PIN for ${targetStaff.displayNameEn}. Default PINs: Owner=1976, Manager=2222, Staff1=1111, Staff2=3333")
            }
        }
    }

    // POS Billing Actions
    fun addOrUpdateDraftItem(
        product: ProductEntity,
        variant: ProductVariantEntity?,
        quantity: Double,
        unitRate: Double
    ) {
        if (quantity <= 0.0 || unitRate <= 0.0) return
        val current = _draftCart.value.toMutableList()
        val existingIdx = current.indexOfFirst {
            it.product.id == product.id && it.variant?.id == variant?.id && it.unitPriceSnapshot == unitRate
        }
        if (existingIdx >= 0) {
            val old = current[existingIdx]
            current[existingIdx] = old.copy(quantity = old.quantity + quantity)
        } else {
            current.add(
                DraftBillItem(
                    product = product,
                    variant = variant,
                    quantity = quantity,
                    unitPriceSnapshot = unitRate
                )
            )
        }
        _draftCart.value = current
    }

    fun removeDraftItem(itemId: String) {
        _draftCart.value = _draftCart.value.filterNot { it.id == itemId }
    }

    fun clearDraftCart() {
        _draftCart.value = emptyList()
        _isRepeatCustomerBill.value = false
        _customerReference.value = ""
    }

    fun setPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    fun setRepeatCustomerBill(isRepeat: Boolean) {
        _isRepeatCustomerBill.value = isRepeat
    }

    fun setCustomerReference(ref: String) {
        _customerReference.value = ref
    }

    fun completeCurrentBill(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val items = _draftCart.value
        if (items.isEmpty()) {
            onComplete(false, "Please add at least one product to the bill.")
            return
        }
        viewModelScope.launch {
            val res = repository.createAndSaveBill(
                items = items,
                paymentMethod = _selectedPaymentMethod.value,
                isRepeatCustomer = _isRepeatCustomerBill.value,
                customerRefOptional = _customerReference.value,
                activeStaff = getActiveStaff(),
                viaVoiceAssistant = false
            )
            res.fold(
                onSuccess = { tx ->
                    clearDraftCart()
                    val msg = "Saved Bill ${tx.billNumber} • ₹${String.format(Locale.US, "%.2f", tx.grandTotal)}"
                    _bannerMessage.value = msg
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Failed to save bill."
                    _bannerMessage.value = msg
                    onComplete(false, msg)
                }
            )
        }
    }

    fun cancelCompletedBill(transaction: TransactionEntity, reason: String) {
        viewModelScope.launch {
            val res = repository.cancelBillWithAudit(
                transaction = transaction,
                reason = reason,
                activeStaff = getActiveStaff(),
                activeRole = getActiveRole()
            )
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Bill ${transaction.billNumber} marked CANCELLED in audit history."
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Could not cancel bill."
                }
            )
        }
    }

    fun updateProductSellingRate(product: ProductEntity, newRate: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateProductRate(
                product = product,
                newRate = newRate,
                activeStaff = getActiveStaff(),
                activeRole = getActiveRole()
            )
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Updated ${product.nameEn} rate to ₹${String.format(Locale.US, "%.2f", newRate)} / ${product.unitType}"
                    onSuccess()
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Rate update denied."
                }
            )
        }
    }

    fun createNewProduct(
        nameEn: String,
        nameBn: String,
        category: String,
        unitType: String,
        rate: Double
    ) {
        viewModelScope.launch {
            val res = repository.addNewProduct(
                nameEn = nameEn,
                nameBn = nameBn,
                category = category,
                unitType = unitType,
                rate = rate,
                activeStaff = getActiveStaff(),
                activeRole = getActiveRole()
            )
            res.fold(
                onSuccess = { p ->
                    _bannerMessage.value = "Added ${p.nameEn} to product catalog."
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Failed to add product."
                }
            )
        }
    }

    // Visitor Counter Actions
    fun addOneVisitor() {
        viewModelScope.launch {
            val res = repository.recordVisitor(
                delta = 1,
                correctionReason = null,
                activeStaff = getActiveStaff()
            )
            if (res.isSuccess) {
                _bannerMessage.value = "+1 Visitor recorded by ${getActiveStaff().displayNameEn}"
            }
        }
    }

    fun correctVisitorCountMinusOne(reason: String) {
        viewModelScope.launch {
            val res = repository.recordVisitor(
                delta = -1,
                correctionReason = reason,
                activeStaff = getActiveStaff()
            )
            res.fold(
                onSuccess = {
                    _bannerMessage.value = "Visitor count corrected (-1) with audit reason."
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Correction failed."
                }
            )
        }
    }

    // Freezer Finder Actions
    fun setFreezerSearchQuery(query: String) {
        _freezerSearchQuery.value = query
    }

    fun selectFreezerNumber(number: Int) {
        _selectedFreezerNumber.value = number.coerceIn(1, 7)
    }

    fun updateFreezerSection(
        freezer: FreezerEntity,
        section: FreezerSectionEntity,
        description: String,
        manualNotes: String,
        photoUri: String?,
        mappedProducts: List<Pair<String, String>>
    ) {
        viewModelScope.launch {
            val res = repository.updateFreezerSectionDetails(
                freezer = freezer,
                section = section,
                newDescription = description,
                newManualNotes = manualNotes,
                newPhotoUri = photoUri,
                mappedProductsList = mappedProducts,
                activeStaff = getActiveStaff()
            )
            if (res.isSuccess) {
                _bannerMessage.value = "Updated Freezer ${freezer.freezerNumber} • ${section.sectionCode}"
            }
        }
    }

    fun addSectionToFreezer(freezer: FreezerEntity, sectionCode: String, description: String) {
        viewModelScope.launch {
            val res = repository.addCustomSectionToFreezer(
                freezer = freezer,
                sectionCode = sectionCode,
                description = description,
                activeStaff = getActiveStaff()
            )
            if (res.isSuccess) {
                _bannerMessage.value = "Added $sectionCode to Freezer ${freezer.freezerNumber}"
            }
        }
    }

    fun removeSectionProductMapping(mappingId: String) {
        viewModelScope.launch {
            repository.deleteProductMapping(mappingId)
        }
    }

    // Cloud Sync Action
    fun triggerManualCloudSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncAllPendingToCloud()
            _isSyncing.value = false
            res.fold(
                onSuccess = { count ->
                    _lastCloudSyncTimeMillis.value = System.currentTimeMillis()
                    _bannerMessage.value = if (count > 0) {
                        "Synchronized $count pending record(s) to Kolkata Cloud."
                    } else {
                        "Cloud synchronization verified — all records up to date."
                    }
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Cloud sync encountered an error."
                }
            )
        }
    }

    // Export CSV & PDF
    fun exportReport(
        context: Context,
        asPdf: Boolean,
        periodLabel: String,
        revenue: Double,
        visitorsCount: Int,
        buyingCustomersCount: Int,
        completedBills: Int,
        productLines: Int,
        filteredTransactions: List<TransactionEntity>
    ) {
        viewModelScope.launch {
            val res = if (asPdf) {
                repository.exportPdfReport(
                    context = context,
                    periodLabel = periodLabel,
                    revenue = revenue,
                    visitorsCount = visitorsCount,
                    buyingCustomersCount = buyingCustomersCount,
                    completedBills = completedBills,
                    productLines = productLines,
                    transactions = filteredTransactions
                )
            } else {
                repository.exportCsvReport(
                    context = context,
                    periodLabel = periodLabel,
                    transactions = filteredTransactions,
                    visitorsCount = visitorsCount,
                    buyingCustomersCount = buyingCustomersCount
                )
            }
            res.fold(
                onSuccess = { file ->
                    _bannerMessage.value = "Generated ${file.name} (${file.length() / 1024 + 1} KB) in reports cache."
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (asPdf) "application/pdf" else "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "NEW FARM PRODUCT - $periodLabel Report")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "NEW FARM PRODUCT ($periodLabel)\nGross Revenue: ₹${String.format(Locale.US, "%.2f", revenue)}\nVisitors: $visitorsCount | Buying Customers: $buyingCustomersCount\nCompleted Bills: $completedBills"
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share $periodLabel Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                        // Ignore if chooser cannot open in headless test
                    }
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Export failed."
                }
            )
        }
    }

    // NEW FARM AI Personal Assistant (Gemini 3.8 Flash)
    fun setWakeWordListening(enabled: Boolean) {
        _isWakeWordListeningEnabled.value = enabled
    }

    fun sendQueryToNewFarmAi(
        rawQuery: String,
        onNavigateToFreezer: (Int) -> Unit = {}
    ) {
        val cleaned = rawQuery.trim()
        if (cleaned.isEmpty()) return

        _chatHistory.value = _chatHistory.value + ChatTurn(isUser = true, text = cleaned)
        _aiUiState.value = AiUiState.Thinking

        viewModelScope.launch {
            val contextSummary = buildLiveStoreContextForAi()
            val result = aiAssistant.askNewFarmAi(cleaned, contextSummary)
            result.fold(
                onSuccess = { aiResp ->
                    _aiUiState.value = AiUiState.Ready(aiResp)
                    _chatHistory.value = _chatHistory.value + ChatTurn(
                        isUser = false,
                        text = aiResp.replyText,
                        responseMeta = aiResp
                    )
                    if (aiResp.draftBill != null) {
                        _pendingVoiceDraftBill.value = aiResp.draftBill
                    }
                    if ((aiResp.intentType == "OPEN_FREEZER" || aiResp.intentType == "FREEZER_LOOKUP") &&
                        aiResp.targetFreezerNumber in 1..7
                    ) {
                        _selectedFreezerNumber.value = aiResp.targetFreezerNumber
                        if (aiResp.intentType == "OPEN_FREEZER") {
                            onNavigateToFreezer(aiResp.targetFreezerNumber)
                        }
                    }
                },
                onFailure = { err ->
                    val errMsg = err.localizedMessage ?: "NEW FARM AI encountered an error."
                    _aiUiState.value = AiUiState.Error(errMsg)
                    _chatHistory.value = _chatHistory.value + ChatTurn(
                        isUser = false,
                        text = "দুঃখিত, এই মুহূর্তে উত্তর দিতে সমস্যা হচ্ছে: $errMsg"
                    )
                }
            )
        }
    }

    fun dismissVoiceDraftBill() {
        _pendingVoiceDraftBill.value = null
    }

    fun confirmAndSaveVoiceDraftBill(
        draft: AiVoiceDraftBill,
        paymentMethod: String = "CASH",
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val matchedProduct = products.value.find {
                it.nameEn.contains(draft.productName, ignoreCase = true) ||
                    draft.productName.contains(it.nameEn, ignoreCase = true) ||
                    it.nameBn.contains(draft.productName, ignoreCase = true)
            } ?: ProductEntity(
                id = "prod_voice_custom",
                sortOrder = 99,
                nameEn = draft.productName,
                nameBn = draft.productName,
                nameHi = draft.productName,
                category = "FROZEN_FISH",
                unitType = draft.unit.uppercase(Locale.US),
                allowDecimalQty = true,
                currentRate = draft.unitRate,
                isRateConfigured = draft.unitRate > 0.0
            )

            val effectiveRate = if (draft.unitRate > 0.0) draft.unitRate else matchedProduct.currentRate
            if (effectiveRate <= 0.0) {
                _bannerMessage.value = "Cannot complete voice bill: Selling rate for ${matchedProduct.nameEn} is not configured yet. Please set its rate first."
                return@launch
            }

            val draftItem = DraftBillItem(
                product = matchedProduct,
                variant = null,
                quantity = draft.quantity,
                unitPriceSnapshot = effectiveRate
            )

            val res = repository.createAndSaveBill(
                items = listOf(draftItem),
                paymentMethod = paymentMethod,
                isRepeatCustomer = false,
                customerRefOptional = "Confirmed via NEW FARM AI Voice Draft",
                activeStaff = getActiveStaff(),
                viaVoiceAssistant = true
            )
            res.fold(
                onSuccess = { tx ->
                    _pendingVoiceDraftBill.value = null
                    val confirmText = "বিল ${tx.billNumber} সফলভাবে সেভ হয়েছে — মোট ₹${String.format(Locale.US, "%.2f", tx.grandTotal)}"
                    _chatHistory.value = _chatHistory.value + ChatTurn(
                        isUser = false,
                        text = "✅ $confirmText"
                    )
                    _bannerMessage.value = confirmText
                    onSaved(confirmText)
                },
                onFailure = { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Failed to save voice draft bill."
                }
            )
        }
    }

    private fun buildLiveStoreContextForAi(): String {
        val todayKey = repository.todayDateKey()
        val todayTxs = transactions.value.filter { it.dateKey == todayKey && it.status == "COMPLETED" }
        val todayRevenue = todayTxs.sumOf { it.grandTotal }
        val todayBills = todayTxs.size
        val todayBuyingCustomers = todayTxs.count { !it.isRepeatCustomerBill }
        val todayLines = todayTxs.sumOf { it.lineCount }
        val todayVisitors = visitors.value.filter { it.dateKey == todayKey }.sumOf { it.delta }.coerceAtLeast(0)

        val productsStr = products.value.joinToString("\n") { p ->
            val rateInfo = if (p.isRateConfigured && p.currentRate > 0.0) {
                "₹${p.currentRate}/${p.unitType}"
            } else {
                "Rate Not Configured Yet (₹0 - Owner must set rate)"
            }
            "- ${p.nameEn} (${p.nameBn}): $rateInfo"
        }

        val mappingsStr = sectionMappings.value.joinToString("\n") { m ->
            "- ${m.productName} -> Freezer ${m.freezerNumber}, ${m.sectionCode} (${m.stockNote})"
        }

        return """
            Date: $todayKey
            Active Operator: ${getActiveStaff().displayNameEn} (${getActiveStaff().roleCode})
            Today's Completed Bills: $todayBills
            Today's Gross Revenue: ₹${String.format(Locale.US, "%.2f", todayRevenue)}
            Today's Product Lines Sold: $todayLines
            Today's Visitors Count: $todayVisitors
            Today's Estimated Buying Customers: $todayBuyingCustomers
            
            Configured Products & Selling Rates:
            $productsStr
            
            7-Freezer Product Location Mappings:
            $mappingsStr
        """.trimIndent()
    }

    fun filterTransactionsByPeriod(all: List<TransactionEntity>, period: ReportPeriod): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        return when (period) {
            ReportPeriod.DAILY -> all.filter { it.dateKey == todayKey }
            ReportPeriod.WEEKLY -> {
                val cutoff = now - 7L * 24L * 3600L * 1000L
                all.filter { it.createdAt >= cutoff }
            }
            ReportPeriod.MONTHLY -> {
                val cutoff = now - 30L * 24L * 3600L * 1000L
                all.filter { it.createdAt >= cutoff }
            }
        }
    }

    fun filterVisitorsByPeriod(all: List<VisitorEntity>, period: ReportPeriod): Int {
        val now = System.currentTimeMillis()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val matching = when (period) {
            ReportPeriod.DAILY -> all.filter { it.dateKey == todayKey }
            ReportPeriod.WEEKLY -> all.filter { it.timestamp >= now - 7L * 24L * 3600L * 1000L }
            ReportPeriod.MONTHLY -> all.filter { it.timestamp >= now - 30L * 24L * 3600L * 1000L }
        }
        return matching.sumOf { it.delta }.coerceAtLeast(0)
    }
}
