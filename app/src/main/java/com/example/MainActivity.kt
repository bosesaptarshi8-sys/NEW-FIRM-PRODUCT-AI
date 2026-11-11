package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.NewFarmDatabase
import com.example.data.remote.CloudStoreRepository
import com.example.data.remote.configureAppCheck
import com.example.data.repository.StoreRepository
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.FreezerFinderScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NewFarmAiScreen
import com.example.ui.screens.OwnerReportsScreen
import com.example.ui.screens.SettingsAndStaffScreen
import com.example.ui.screens.SmartBillingScreen
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.NewFarmProductTheme
import com.example.ui.theme.SubtleOrange
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainNavSection(
    val route: String,
    val labelEn: String,
    val labelBn: String,
    val icon: ImageVector
) {
    HOME("home", "Home", "হোম", Icons.Default.Home),
    BILLING("billing", "Billing", "বিলিং", Icons.Default.PointOfSale),
    FREEZERS("freezers", "Freezers", "ফ্রিজার", Icons.Default.AcUnit),
    REPORTS("reports", "Reports", "রিপোর্ট", Icons.Default.Assessment),
    NEW_FARM_AI("new_farm_ai", "NEW FARM AI", "এআই ভয়েস", Icons.Default.Mic),
    SETTINGS("settings", "Settings", "সেটিংস", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            NewFarmProductTheme {
                NewFarmProductRootApp()
            }
        }
    }
}

@Composable
fun NewFarmProductRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    // Allow shop employees in Asansol to also enter Offline Counter Mode (backed by Room SQLite)
    // while keeping Google Sign-In available at all times.
    var offlineCounterEntered by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !offlineCounterEntered) {
        AuthGateScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onContinueOfflineCounter = { offlineCounterEntered = true }
        )
    } else {
        val sessionKey = currentUser?.uid ?: "offline_shop_counter"
        MainStoreScaffold(
            sessionKey = sessionKey,
            googleUserEmail = currentUser?.email,
            onRequireAuthScreen = { offlineCounterEntered = false },
            onAuthUpdated = { currentUser = Firebase.auth.currentUser }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStoreScaffold(
    sessionKey: String,
    googleUserEmail: String?,
    onRequireAuthScreen: () -> Unit,
    onAuthUpdated: () -> Unit
) {
    val context = LocalContext.current
    val storeViewModel: StoreViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val roomDb = NewFarmDatabase.getInstance(app)
                val cloudRepo = CloudStoreRepository(firestore)
                val storeRepo = StoreRepository(roomDb, roomDb.appDao(), cloudRepo)
                StoreViewModel(storeRepo)
            }
        }
    )

    LaunchedEffect(Unit) {
        storeViewModel.initializeCatalog(context)
    }

    var currentSection by remember { mutableStateOf(MainNavSection.HOME) }

    val language by storeViewModel.selectedLanguage.collectAsStateWithLifecycle()
    val staffProfiles by storeViewModel.staffProfiles.collectAsStateWithLifecycle()
    val roles by storeViewModel.roles.collectAsStateWithLifecycle()
    val activeStaffId by storeViewModel.activeStaffId.collectAsStateWithLifecycle()
    val products by storeViewModel.products.collectAsStateWithLifecycle()
    val variants by storeViewModel.productVariants.collectAsStateWithLifecycle()
    val transactions by storeViewModel.transactions.collectAsStateWithLifecycle()
    val transactionItems by storeViewModel.transactionItems.collectAsStateWithLifecycle()
    val visitors by storeViewModel.visitors.collectAsStateWithLifecycle()
    val freezers by storeViewModel.freezers.collectAsStateWithLifecycle()
    val freezerSections by storeViewModel.freezerSections.collectAsStateWithLifecycle()
    val freezerPhotos by storeViewModel.freezerPhotos.collectAsStateWithLifecycle()
    val sectionMappings by storeViewModel.sectionMappings.collectAsStateWithLifecycle()
    val auditLogs by storeViewModel.auditLogs.collectAsStateWithLifecycle()
    val pendingSyncQueue by storeViewModel.pendingSyncQueue.collectAsStateWithLifecycle()
    val isDemoMode by storeViewModel.isDemoMode.collectAsStateWithLifecycle()
    val selectedPeriod by storeViewModel.selectedReportPeriod.collectAsStateWithLifecycle()
    val lastSyncMillis by storeViewModel.lastCloudSyncTimeMillis.collectAsStateWithLifecycle()
    val bannerMessage by storeViewModel.bannerMessage.collectAsStateWithLifecycle()

    val draftCart by storeViewModel.draftCart.collectAsStateWithLifecycle()
    val selectedPaymentMethod by storeViewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val isRepeatCustomerBill by storeViewModel.isRepeatCustomerBill.collectAsStateWithLifecycle()

    val freezerSearchQuery by storeViewModel.freezerSearchQuery.collectAsStateWithLifecycle()
    val selectedFreezerNumber by storeViewModel.selectedFreezerNumber.collectAsStateWithLifecycle()

    val chatHistory by storeViewModel.chatHistory.collectAsStateWithLifecycle()
    val aiUiState by storeViewModel.aiUiState.collectAsStateWithLifecycle()
    val pendingVoiceDraftBill by storeViewModel.pendingVoiceDraftBill.collectAsStateWithLifecycle()
    val isWakeWordEnabled by storeViewModel.isWakeWordListeningEnabled.collectAsStateWithLifecycle()

    // Ensure reactive recomposition when staffProfiles or activeStaffId updates
    val activeStaff = remember(staffProfiles, activeStaffId) { storeViewModel.getActiveStaff() }
    val activeRole = remember(roles, activeStaff) { storeViewModel.getActiveRole() }

    val filteredTransactions = remember(transactions, selectedPeriod) {
        storeViewModel.filterTransactionsByPeriod(transactions, selectedPeriod)
    }
    val periodVisitorsCount = remember(visitors, selectedPeriod) {
        storeViewModel.filterVisitorsByPeriod(visitors, selectedPeriod)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavyBlue,
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = "NEW FARM PRODUCT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeStaff.displayNameEn} (${activeStaff.roleCode}) • 50 Yrs Trust",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Surface(
                        color = FreshTeal,
                        shape = RoundedCornerShape(50),
                        onClick = { storeViewModel.cycleLanguage() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("language_cycle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Switch Language",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = language.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentSection != MainNavSection.NEW_FARM_AI) {
                FloatingActionButton(
                    onClick = { currentSection = MainNavSection.NEW_FARM_AI },
                    containerColor = SubtleOrange,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_new_farm_ai_mic")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Open NEW FARM AI Voice Assistant"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEW FARM AI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                MainNavSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = currentSection == section,
                        onClick = { currentSection = section },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.labelEn
                            )
                        },
                        label = {
                            Text(
                                text = section.labelEn,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${section.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Optional Toast/Confirmation Banner
            bannerMessage?.let { msg ->
                Surface(
                    color = FreshTeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { storeViewModel.clearBannerMessage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Banner",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentSection) {
                    MainNavSection.HOME -> HomeDashboardScreen(
                        language = language,
                        activeStaff = activeStaff,
                        isDemoMode = isDemoMode,
                        selectedPeriod = selectedPeriod,
                        filteredTransactions = filteredTransactions,
                        visitorCount = periodVisitorsCount,
                        pendingSyncCount = pendingSyncQueue.size,
                        isCloudSignedIn = googleUserEmail != null,
                        lastSyncTimeMillis = lastSyncMillis,
                        onToggleDemoMode = { storeViewModel.toggleDemoMode() },
                        onSelectPeriod = { storeViewModel.setReportPeriod(it) },
                        onAddVisitor = { storeViewModel.addOneVisitor() },
                        onCorrectVisitorMinusOne = { reason -> storeViewModel.correctVisitorCountMinusOne(reason) },
                        onNavigateToBilling = { currentSection = MainNavSection.BILLING },
                        onNavigateToFreezers = { currentSection = MainNavSection.FREEZERS },
                        onNavigateToAi = { currentSection = MainNavSection.NEW_FARM_AI },
                        onTriggerSync = {
                            if (googleUserEmail == null) {
                                onRequireAuthScreen()
                            } else {
                                storeViewModel.triggerManualCloudSync()
                            }
                        }
                    )

                    MainNavSection.BILLING -> SmartBillingScreen(
                        products = products,
                        variants = variants,
                        draftCart = draftCart,
                        recentTransactions = transactions,
                        selectedPaymentMethod = selectedPaymentMethod,
                        isRepeatCustomer = isRepeatCustomerBill,
                        activeRole = activeRole,
                        onAddDraftItem = { prod, variant, qty, rate ->
                            storeViewModel.addOrUpdateDraftItem(prod, variant, qty, rate)
                        },
                        onRemoveDraftItem = { storeViewModel.removeDraftItem(it) },
                        onClearDraftCart = { storeViewModel.clearDraftCart() },
                        onSelectPaymentMethod = { storeViewModel.setPaymentMethod(it) },
                        onToggleRepeatCustomer = { storeViewModel.setRepeatCustomerBill(it) },
                        onSaveBill = { storeViewModel.completeCurrentBill() },
                        onCancelCompletedBill = { tx, reason -> storeViewModel.cancelCompletedBill(tx, reason) },
                        onUpdateProductRate = { prod, newRate -> storeViewModel.updateProductSellingRate(prod, newRate) },
                        onNavigateBackHome = { currentSection = MainNavSection.HOME }
                    )

                    MainNavSection.FREEZERS -> FreezerFinderScreen(
                        freezers = freezers,
                        sections = freezerSections,
                        photosHistory = freezerPhotos,
                        mappings = sectionMappings,
                        searchQuery = freezerSearchQuery,
                        selectedFreezerNumber = selectedFreezerNumber,
                        onSearchQueryChange = { storeViewModel.setFreezerSearchQuery(it) },
                        onSelectFreezerNumber = { storeViewModel.selectFreezerNumber(it) },
                        onUpdateSection = { freezer, sec, desc, notes, photoUri, mappedList ->
                            storeViewModel.updateFreezerSection(freezer, sec, desc, notes, photoUri, mappedList)
                        },
                        onAddCustomSection = { freezer, code, desc ->
                            storeViewModel.addSectionToFreezer(freezer, code, desc)
                        },
                        onDeleteMapping = { storeViewModel.removeSectionProductMapping(it) },
                        onNavigateBackHome = { currentSection = MainNavSection.HOME }
                    )

                    MainNavSection.REPORTS -> OwnerReportsScreen(
                        activeRole = activeRole,
                        selectedPeriod = selectedPeriod,
                        filteredTransactions = filteredTransactions,
                        allTransactionItems = transactionItems,
                        visitorCount = periodVisitorsCount,
                        pendingSyncCount = pendingSyncQueue.size,
                        lastSyncTimeMillis = lastSyncMillis,
                        onSelectPeriod = { storeViewModel.setReportPeriod(it) },
                        onTriggerSync = {
                            if (googleUserEmail == null) {
                                onRequireAuthScreen()
                            } else {
                                storeViewModel.triggerManualCloudSync()
                            }
                        },
                        onExportReport = { asPdf, periodLbl, rev, vis, buyers, bills, lines, txs ->
                            storeViewModel.exportReport(
                                context,
                                asPdf,
                                periodLbl,
                                rev,
                                vis,
                                buyers,
                                bills,
                                lines,
                                txs
                            )
                        },
                        onNavigateBackHome = { currentSection = MainNavSection.HOME }
                    )

                    MainNavSection.NEW_FARM_AI -> NewFarmAiScreen(
                        chatHistory = chatHistory,
                        aiUiState = aiUiState,
                        pendingVoiceDraftBill = pendingVoiceDraftBill,
                        isWakeWordEnabled = isWakeWordEnabled,
                        onToggleWakeWord = { storeViewModel.setWakeWordListening(it) },
                        onSendQuery = { query ->
                            storeViewModel.sendQueryToNewFarmAi(
                                rawQuery = query,
                                onNavigateToFreezer = { fNum ->
                                    storeViewModel.selectFreezerNumber(fNum)
                                    currentSection = MainNavSection.FREEZERS
                                }
                            )
                        },
                        onConfirmVoiceDraftBill = { draft ->
                            storeViewModel.confirmAndSaveVoiceDraftBill(draft)
                        },
                        onDismissVoiceDraftBill = { storeViewModel.dismissVoiceDraftBill() },
                        onNavigateBackHome = { currentSection = MainNavSection.HOME }
                    )

                    MainNavSection.SETTINGS -> SettingsAndStaffScreen(
                        staffProfiles = staffProfiles,
                        activeStaff = activeStaff,
                        activeRole = activeRole,
                        products = products,
                        auditLogs = auditLogs,
                        googleUserEmail = googleUserEmail,
                        onSwitchStaffWithPin = { staff, pin, cb ->
                            storeViewModel.switchStaffWithPin(staff, pin, cb)
                        },
                        onUpdateProductRate = { prod, rate ->
                            storeViewModel.updateProductSellingRate(prod, rate)
                        },
                        onAddNewProduct = { nameEn, nameBn, cat, unit, rate ->
                            storeViewModel.createNewProduct(nameEn, nameBn, cat, unit, rate)
                        },
                        onGoogleAuthSuccess = onAuthUpdated,
                        onGoogleSignOut = onAuthUpdated,
                        onNavigateBackHome = { currentSection = MainNavSection.HOME }
                    )
                }
            }
        }
    }
}
