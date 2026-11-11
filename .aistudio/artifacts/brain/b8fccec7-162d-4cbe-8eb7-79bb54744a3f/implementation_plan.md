# NEW FARM PRODUCT — Smart Retail Management & Personal AI Assistant

A production-oriented, offline-first Android retail management system and multilingual Personal AI Assistant (**NEW FARM AI** powered by `gemini-3.8-flash`) built for **NEW FARM PRODUCT** in Asansol, West Bengal (*"50 Years of Trust. A Smarter Way Forward."*). It enables effortless daily shop operations (Smart POS Billing, 7-Freezer Section & Photo Finder, Visitor vs. Buyer Tracking, and Role-Based Staff Management) while providing real-time cloud synchronization and remote owner oversight from Kolkata.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed architectural choices and recommended business defaults below. Click **Proceed** when you are ready for us to provision the backend and build the application.

- **Confirmed Decision 1 — NEW FARM AI Personal Assistant (`gemini-3.8-flash`)**: The dedicated Personal Assistant for **NEW FARM PRODUCT** will be powered by **Firebase AI Logic** using the **`gemini-3.8-flash`** model with structured JSON output (`Schema.obj`) and real-time streaming. It understands Bengali, Hindi, and English commands to query live shop sales, locate items across the 7 deep freezers, inspect visitor traffic, and prepare safe draft bills that require explicit human confirmation before saving.
- **Confirmed Decision 2 — Native Android (Kotlin + Jetpack Compose) Architecture**: Because Google AI Studio's Android runtime compiles native Kotlin/Jetpack Compose APKs directly via Gradle (rather than Flutter/Dart or external Supabase instances), we will implement a 100% native Android architecture using **Jetpack Compose (Material 3)**, **Room SQLite** for guaranteed offline-first local storage, and **Firebase (Cloud Firestore + Google Sign-In + AI Logic)** for cloud sync and remote monitoring from Kolkata.
- **Recommended Default 1 — Unspecified Product Rates**: Products with explicitly stated prices (`Salmon Fish` at ₹500/kg, `Vetki Fish Fry` at ₹1,200/kg, `Vetki Fish Finger` at ₹1,200/kg) come pre-configured. Staples without specified rates (`Rice`, `Dal`, `Potato`, `Onion`, `Garlic`, `Eggs`, `Ice Cream` variants, `Fish Fry`, `Fish Finger`) default to `Rate Not Set (₹0.00)` with a one-tap Owner/Authorized Rate Prompt during billing or in Settings so no fake prices are ever recorded as real revenue.
- **Recommended Default 2 — Buying Customers vs. Completed Bills**: Walk-in purchases remain frictionless and anonymous. By default, each completed bill increments **Buying Customers** by 1, while a prominent **"Same Customer / Split Bill"** toggle on the POS screen lets staff record additional bills for the same shopper without inflating unique Buying Customer counts.

---

## 1. Overview, Project Analysis & Technical Limitations

### What It Does
**NEW FARM PRODUCT** unifies daily counter operations in Asansol with remote executive visibility in Kolkata across six integrated pillars:
1. **Smart Billing & Daily Sales (POS)**: Rapid multi-item checkout supporting weight-based (`kg`, `g` with decimals) and unit-based (`piece`, `packet`) items, historical price snapshots, zero minimum bill thresholds, and strict non-destructive bill cancellation with audit trails.
2. **Seven-Freezer Photo & Section Finder**: Dedicated profiles for **Freezer 1 through Freezer 7**, configurable compartments (`Section A, B, C, D+`), camera/photo-picker capture, previous photo history, gradual product-to-section mapping, and instant search with staleness timestamps.
3. **Visitor & Buying Customer Tracking**: Large one-tap **+1 Visitor** counter with staff attribution, timestamped logs, and audited corrections—strictly separating *Visitors*, *Buying Customers*, *Transactions*, *Product Lines*, and *Revenue*.
4. **Role-Based Staff Management**: Dedicated shop profiles (**Owner**, **Manager / Family Member**, **Staff Member 1**, **Staff Member 2**) enforcing granular permission boundaries and full activity attribution.
5. **Remote Owner Dashboard & Reports**: Daily, Weekly, and Monthly analytics (with CSV and PDF export), cloud sync health indicators, and clear separation of gross revenue from profit (profit remains locked until purchase cost and operating expense inputs are enabled).
6. **NEW FARM AI Personal Assistant (`gemini-3.8-flash`)**: Contextual Bengali, Hindi, and English voice and chat assistant accessible via a prominent microphone control, capable of answering live store queries and preparing structured draft bills with mandatory human confirmation.

### Technical Limitations & Environment Evaluation

| Proposed Component | Environment Reality & Limitation | Recommended Production Path in AI Studio |
| :--- | :--- | :--- |
| **Flutter Android Frontend** | This build environment is a native Android Gradle container (`Kotlin` + `Jetpack Compose`), not a Dart/Flutter SDK container. | Build a native **Kotlin + Jetpack Compose** app. It delivers superior camera/microphone hardware integration, faster startup on shop phones, and native APK generation. |
| **Supabase / PostgreSQL** | External Supabase clusters cannot be auto-provisioned inside the container without manual external credentials. | Use **Room (SQLite)** for normalized relational local storage paired with **Firebase Cloud Firestore** (provisioned via AI Studio) for real-time Kolkata owner sync. |
| **Always-On Background Wake Word** (*"হাই নিউ ফর্ম প্রোডাক্ট"*) | Android 14–16 strictly throttles background microphone access, requires persistent foreground-service notifications, and drains battery if an ASR loop runs 24/7. | Provide **In-App Hands-Free Wake Word Listening** (active while the POS/Dashboard screen is open on the counter) plus a prominent **1-Tap Floating Microphone Button** on every screen. |
| **Visual Product Detection in Freezers** | Photographs of stacked frozen packets cannot reliably identify items buried underneath top layers. | Enforce explicit **Last Updated Timestamp + Staff Attribution** on every section photo and rely on staff-curated section mappings with photo verification. |

### Missing Business Decisions Identified (Handled Safely in V1)
- **GST / Tax & Receipt Format**: Frozen processed foods vs. raw staples (Rice, Potato, Onion) have different tax treatments. V1 records clean net selling rates and generates printable/shareable digital bill summaries without assuming GST percentages until configured by the Owner.
- **Ice Cream Variants & Configurable Fish Items**: Pre-seeded with practical variant support (e.g., Vanilla, Chocolate, Butterscotch, Kulfi, Family Pack) and configurable rates so the Owner can set exact prices on first launch.
- **Multi-Device Counter Contention**: Bill numbers use a deterministic prefix + timestamp + device/staff short-code (`NFP-YYYYMMDD-XXXX`) alongside stable UUIDs so offline bills from two phones never collide during sync.

---

## 2. User Experience, Visual Design & Screen-by-Screen Plan

### Visual Identity & Brand System
- **Aesthetic Direction**: **Heritage Modern Retail** — combining the warmth and 50-year legacy of an Asansol family institution with the crisp, hygienic clarity of a modern cold-chain store.
- **Color Palette**:
  - **Primary (`Deep Navy Blue` — `#0D253F`)**: Authority, 50-year legacy, high-contrast headers, primary navigation state, and executive cards.
  - **Secondary (`Fresh Cold-Chain Teal` — `#00897B` / `#E0F2F1` container)**: Represents hygienic deep-freezer freshness, active sync badges, and Freezer Finder highlights.
  - **Surface & Background (`Warm Ivory White` — `#FAF9F5` & Pure Card Surface `#FFFFFF`)**: Reduces glare under bright shop fluorescent lighting while keeping long billing sessions comfortable.
  - **Accent (`Subtle Warm Amber/Orange` — `#F57C00` / `#FFF3E0` container)**: Quick action shortcuts, voice assistant pulse ring, and attention badges (such as unconfigured product rates or pending offline syncs).
  - **Semantic Status Colors**: Emerald Green (`#2E7D32`) for cloud-synced bills; Crimson (`#C62828`) for audited bill cancellations and permission alerts.
- **Typography & Multilingual Clarity**:
  - Locally bundled Google Fonts (`Plus Jakarta Sans` for bold, scannable tabular numbers and headings + `Noto Sans` / `Inter` for clean English, Bengali `বাংলা`, and Hindi `हिन्दी` script rendering).
  - Bilingual UI labels (`English + বাংলা`, switchable to `English + हिन्दी` or single-language mode in Settings) so shop staff and family members can read every button effortlessly.
- **Touch & Ergonomics**:
  - Minimum `56.dp` height on primary billing and visitor buttons (`48.dp+` minimum interactive target everywhere).
  - High-contrast elevation cards with `16.dp` rounded corners and zero visual clutter.

### Screen-by-Screen UI Plan

1. **Header Bar & Role Switcher / Auth Gate (Persistent Top Context)**
   - Displays **NEW FARM PRODUCT** brand emblem, bilingual tagline, active operator badge (`Owner`, `Manager`, `Staff 1`, `Staff 2`), **Language Pill (`EN / বাংলা / हिं`)**, and **Cloud Sync Status Pill** (`Synced • 2m ago` or `Offline • 3 Pending`).
2. **Screen 1: Home Dashboard (`Home`)**
   - **Demo Mode vs. Live Business Data Banner**: Starts with clean zero-state live counters; includes an explicit toggle to preview clearly badged `[DEMO MODE]` sample data without polluting real store records.
   - **4 Primary Action Shortcuts (Top Grid)**: Large tactile cards for **New Sale (নতুন বিল)**, **+1 Visitor (কাস্টমার এন্ট্রি)**, **Freezer Finder (ফ্রিজার খুঁজুন)**, and **NEW FARM AI (এআই অ্যাসিস্ট্যান্ট)**.
   - **Live Today's Pulse Cards**: Today's Sales (`₹`), Today's Visitors, Buying Customers (with estimation badge), Completed Bills, and Product Lines Sold.
   - **Staff-Wise Sales Breakdown & Latest Transactions**: Real-time summary of counter activity and recent bills with status chips.
3. **Screen 2: Smart Billing / POS (`Billing`)**
   - **Fast Product Grid & Bilingual Search**: Instant filter chips (`All`, `Fish & Frozen`, `Daily Staples`, `Ice Cream`) and 1-tap product cards for the 12 core products + variants.
   - **Smart Quantity & Rate Entry Sheet**:
     - Weight-based items (`kg`, `g`) offer quick-tap presets (`0.25 kg`, `0.5 kg`, `1 kg`, `2 kg`) plus decimal keypad entry.
     - Piece/Packet items offer `+ / -` steppers and quick presets (`1`, `2`, `6 pcs`, `12 pcs`).
     - Displays unit price snapshot; allows rate editing only if the active role has permission (or prompts authorized rate setup if rate is ₹0).
   - **Live Bill Cart & Checkout Bar**:
     - Shows all line items (`Quantity × Unit Rate = Line Total`), **Grand Total**, Payment Method selector (`Cash`, `UPI / QR`, `Card`), and **"Repeat / Same Customer"** checkbox.
     - Actions: **Save & Complete Bill**, **Cancel Draft**, and **Bill History / Audited Cancellation Drawer** (never deletes completed bills; records cancellation reason and staff ID).
4. **Screen 3: Seven-Freezer Photo & Section Finder (`Freezers`)**
   - **Global Product-to-Freezer Search Bar**: Type or speak e.g. *"Vetki Fish Finger"* to immediately highlight **`Freezer 3 → Section B`** with the latest section photo, manual notes, last updated timestamp, and staff name.
   - **7 Deep Freezer Cards (`Freezer 1` – `Freezer 7`)**: Visual grid showing section count, mapped items, and last photo update age.
   - **Freezer & Section Detail View**:
     - Tabs for `Section A`, `Section B`, `Section C`, `Section D`, plus **`+ Add Section`**.
     - **Camera Capture & Android Photo Picker**: Take a fresh section photo or pick from gallery; automatically archives the previous photo into **Photo History** with timestamp and staff attribution.
     - **Gradual Product Mapping**: Tag products or type manual stock placement notes with a explicit honesty notice: *"Photos show top-layer arrangement as of [Timestamp]. Verify lower layers manually."*
5. **Screen 4: Visitor Counter & Customer Conversion (`Visitors` Modal / Section)**
   - Accessible directly from Home or Reports: Giant **`+1 Visitor (দোকানে আগমন)`** button, today's hourly visitor timeline, audited **`-1 Correction (with mandatory reason)`**, and the 5-metric funnel (*Visitors → Buying Customers → Transactions → Product Lines → Revenue*) with conversion rate and average bill value.
6. **Screen 5: Remote Owner Dashboard & Reports (`Reports`)**
   - **Period Selector**: **Daily**, **Weekly**, and **Monthly** report tabs.
   - **Executive Metrics & Breakdown**: Revenue, Visitors, Buying Customers, Completed vs. Cancelled Bills, Product-Wise Performance table, and Staff-Wise Sales Leaderboard.
   - **Revenue vs. Profit Guardrail**: Explicit notice stating that values represent **Gross Revenue**, with Profit Calculation locked until cost price and expense tracking are enabled.
   - **Export & Sync Controls**: **Export CSV**, **Export PDF Report** (via Android `PdfDocument` & Share sheet), and **Sync Now / Sync Queue Inspector**.
7. **Screen 6: NEW FARM AI Personal Assistant (`NEW FARM AI`)**
   - Accessible via the bottom navigation bar and the prominent floating microphone button.
   - **Powered by `gemini-3.8-flash` (Firebase AI Logic)**:
     - **Wake-Word & Voice Input**: Supports in-app wake phrase detection (*"হাই নিউ ফর্ম প্রোডাক্ট"* / *"Hi New Farm Product"*) via Android `SpeechRecognizer` and speaks responses aloud using `TextToSpeech` configured for a warm Indian Bengali/Hindi/English female voice.
     - **Live Store Grounding**: Reads real-time Room/Firestore store metrics to answer *"আজ কত বিক্রি হয়েছে?"* (Today's sales), *"Vetki Fish Finger কোথায় আছে?"* (Freezer 3, Section B + opens photo card), *"Freezer 3-এর ছবি দেখাও"*, and *"আজ কতজন দোকানে এসেছে?"* (Visitor count).
     - **Safe Voice Billing Workflow**: When staff say *"এক কেজি Salmon Fish বিক্রি হয়েছে"*, Gemini 3.8 Flash parses the intent into a **Structured Draft Bill Card** (`Product`, `Quantity`, `Unit`, `Unit Rate`, `Line Total`) with **Confirm & Add to Bill** vs. **Edit / Discard** buttons—never saving a financial transaction without human confirmation.
8. **Screen 7: Staff & Product Settings (`Settings`)**
   - **Role & Staff Switcher / Google Cloud Sign-In**: Switch active shop operator (`Owner`, `Manager`, `Staff 1`, `Staff 2`) with PIN/Role verification, plus Google Sign-In for cloud synchronization.
   - **Product Catalogue & Rate Manager**: Owner-authorized editor for the 12+ initial products, unit types, variants, and selling rates.
   - **Audit Log Viewer**: Immutable chronological trail of rate edits, bill cancellations, visitor corrections, and freezer updates.

---

## 3. Key Product Decisions & Phased Roadmap

### Architectural & Product Decisions
- **Decision 1: Offline-First Room SQLite + Idempotent Cloud Firestore Sync**
  - *Chosen Approach*: Every sale, visitor tap, freezer photo record, and audit log writes immediately to **Room SQLite** inside a database transaction alongside a `sync_queue` entry with a stable UUID. When internet is available and the user is signed in, the background sync engine pushes pending records idempotently to **Cloud Firestore** (using the stable UUID as the document key so retries never duplicate bills).
  - *Why*: Internet connectivity in Asansol can fluctuate; the counter must never freeze or lose a ₹10 sale, while the Owner in Kolkata still gets real-time cloud updates.
- **Decision 2: Structured Output (`Schema.obj`) with `gemini-3.8-flash` for NEW FARM AI**
  - *Chosen Approach*: Use `Firebase.ai.generativeModel("gemini-3.8-flash")` with a strict JSON `responseSchema` that returns:
    1. Spoken/displayed response text in the user's language (`bn`, `hi`, or `en`),
    2. Recognized intent (`SALES_SUMMARY`, `FREEZER_LOOKUP`, `OPEN_FREEZER`, `VISITOR_SUMMARY`, `DRAFT_BILL`, `GENERAL_HELP`),
    3. Matched freezer/section IDs or structured draft bill line items (`productName`, `quantity`, `unit`, `rate`, `total`).
  - *Why*: Guarantees deterministic UI cards (such as the Voice Billing Draft Confirmation Card and Freezer Photo Card) alongside natural multilingual conversation.

### Realistic Phased Development Estimate & AI Studio Capabilities

| Phase | Scope & Deliverables | Real-World Team Estimate | AI Studio Direct Generation vs. External Setup |
| :--- | :--- | :--- | :--- |
| **Phase 0** | Architecture, Normalized 14-Entity Schema, Multilingual Design System | 3–5 Days | **100% Generated Directly** in AI Studio. |
| **Phase 1** | Home Dashboard, Smart POS Billing, 7-Freezer Finder + Camera/Photo Picker, Visitor Counter, Room Offline DB | 2–3 Weeks | **100% Generated Directly** (Room SQLite, Camera/Photo Picker, PDF/CSV generators). |
| **Phase 2** | Role-Based Staff Auth, Cloud Firestore Sync, Remote Kolkata Owner Dashboard, Staff & Period Reports | 2 Weeks | **Generated Directly via AI Studio Firebase Provisioning** (requires 1-click Firebase consent in UI). |
| **Phase 3** | **NEW FARM AI** Personal Assistant (`gemini-3.8-flash`), Bengali/Hindi/English Voice I/O, Safe Voice Billing Drafts | 1.5–2 Weeks | **Generated Directly via Firebase AI Logic (`gemini-3.8-flash`)** + Android `SpeechRecognizer` & `TextToSpeech`. |
| **Phase 4** | Customer Reference Tagging, Purchase Cost & Profit Accounting, Multi-Store Analytics | 2 Weeks | Schema hooks included now; profit calculations safely locked until cost data is entered. |

---

## 4. Technical Architecture & Normalized Database Schema

### System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                   NEW FARM PRODUCT — ANDROID APPLICATION                      │
│          “50 Years of Trust. A Smarter Way Forward.” (EN / বাংলা / हिं)        │
└───────────────────────────────────────┬───────────────────────────────────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        ▼                               ▼                               ▼
┌──────────────────────┐   ┌─────────────────────────┐   ┌──────────────────────┐
│  POS & SHOP SCREENS  │   │  NEW FARM AI ASSISTANT  │   │ OWNER REPORTS & SYNC │
│ • Home Dashboard     │   │ • gemini-3.8-flash      │   │ • Daily/Weekly/Month │
│ • Smart Billing POS  │   │ • Bengali/Hindi/English │   │ • Staff Sales Matrix │
│ • 7-Freezer Finder   │   │ • Draft Bill Safety Gate│   │ • CSV & PDF Exporter │
│ • +1 Visitor Counter │   │ • In-App Wake Word + TTS│   │ • Audit Log Inspector│
└──────────┬───────────┘   └────────────┬────────────┘   └──────────┬───────────┘
           │                            │                           │
           └────────────────────────────┼───────────────────────────┘
                                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                VIEWMODEL & DOMAIN LAYER (StateFlow + RBAC Gate)               │
│ • Role Permission Enforcer (OWNER / MANAGER / STAFF_1 / STAFF_2)              │
│ • Price Snapshot & Non-Destructive Cancellation Engine                        │
│ • Idempotent Offline-to-Cloud Sync Coordinator                                │
└──────────────────┬─────────────────────────────────────────┬──────────────────┘
                   ▼                                         ▼
┌─────────────────────────────────────────┐   ┌─────────────────────────────────┐
│   LOCAL OFFLINE-FIRST DB (Room SQLite)  │   │   CLOUD & AI BACKEND (Firebase) │
│ • 14 Normalized Entities + Foreign Keys │   │ • Firebase AI Logic             │
│ • Instant Zero-Latency Reads/Writes     │──▶│   (gemini-3.8-flash + AppCheck) │
│ • Outbox `sync_queue` + Photo Queue     │   │ • Cloud Firestore (Kolkata Sync)│
│ • Immutable `audit_logs`                │   │ • Google Sign-In (CredentialMgr)│
└─────────────────────────────────────────┘   └─────────────────────────────────┘
```

### Normalized 14-Entity Database Schema Proposal (Room SQLite + Cloud Sync Mirror)

All primary keys use stable UUID strings (`id: String`) so records created offline on shop devices synchronize idempotently to the cloud without ID collisions.

1. **`roles`**: `id (PK)`, `code` (`OWNER`, `MANAGER`, `STAFF`), `nameEn`, `nameBn`, `canEditRates`, `canCancelBills`, `canViewOwnerReports`, `canManageStaff`.
2. **`users`**: `id (PK)`, `username`, `roleId (FK -> roles.id)`, `pinHash` (SHA-256 salted hash, never plain text), `googleUid`, `isActive`, `createdAt`.
3. **`staff_profiles`**: `id (PK)`, `userId (FK -> users.id, Unique)`, `displayName`, `displayNameBn`, `phone`, `shiftNotes`, `lastActiveAt`.
4. **`products`**: `id (PK)`, `nameEn`, `nameBn`, `category`, `unitType` (`KG`, `GRAM`, `PIECE`, `PACKET`), `allowDecimalQty`, `currentRate` (`Double?` — nullable/0.0 when unconfigured), `isRateConfigured`, `isActive`, `updatedAt`, `updatedByStaffId`.
   - *Pre-seeded with the 12 core items*: Rice, Dal, Potato, Onion, Garlic, Eggs, Ice Cream, Salmon Fish (₹500/kg), Fish Fry (configurable), Fish Finger (configurable), Vetki Fish Fry (₹1,200/kg), Vetki Fish Finger (₹1,200/kg).
5. **`product_variants`**: `id (PK)`, `productId (FK -> products.id)`, `variantNameEn`, `variantNameBn`, `rateOverride`, `isActive` (supports multiple Ice Cream flavors/pack sizes and fish pack variants).
6. **`transactions`**: `id (PK, UUID)`, `billNumber (Unique Index, e.g. NFP-20261111-0001)`, `createdAt (Index)`, `staffId (FK -> staff_profiles.id, Index)`, `staffNameSnapshot`, `grandTotal`, `paymentMethod` (`CASH`, `UPI`, `CARD`), `status` (`COMPLETED`, `CANCELLED`), `isRepeatCustomerBill` (`Boolean` — controls Buying Customer calculation), `customerRefOptional`, `cancellationReason`, `cancelledByStaffId`, `cancelledAt`, `syncStatus` (`PENDING`, `SYNCED`, `FAILED`).
7. **`transaction_items`**: `id (PK, UUID)`, `transactionId (FK -> transactions.id, Index)`, `productId (FK -> products.id)`, `variantId`, `productNameSnapshot`, `quantity`, `unitTypeSnapshot`, `unitPriceSnapshot` (locks historical rate at time of sale), `lineTotal`.
8. **`visitors`**: `id (PK, UUID)`, `timestamp (Index)`, `dateKey (YYYY-MM-DD, Index)`, `delta` (`+1` for entry, `-1` for audited correction), `staffId (FK -> staff_profiles.id)`, `staffNameSnapshot`, `correctionReason`, `syncStatus`.
9. **`freezers`**: `id (PK)`, `freezerNumber (1..7, Unique)`, `freezerNameEn`, `freezerNameBn`, `description`, `lastUpdatedAt`, `lastUpdatedByStaffName`.
10. **`freezer_sections`**: `id (PK)`, `freezerId (FK -> freezers.id, Index)`, `sectionCode` (`Section A`, `Section B`, `Section C`, `Section D`, etc.), `description`, `manualNotes`, `currentPhotoUri`, `lastUpdatedAt`, `updatedByStaffId`, `updatedByStaffName`.
11. **`freezer_photos`**: `id (PK, UUID)`, `freezerId (FK -> freezers.id)`, `sectionId (FK -> freezer_sections.id, Index)`, `localPhotoUri`, `cloudPhotoUrl`, `notesSnapshot`, `capturedAt`, `uploadedByStaffId`, `uploadedByStaffName`, `uploadStatus` (`LOCAL_QUEUED`, `UPLOADED`, `FAILED`), `isCurrent`.
12. **`section_product_mappings`**: `id (PK, UUID)`, `sectionId (FK -> freezer_sections.id, Index)`, `freezerId (FK -> freezers.id)`, `productNameQuery` (indexed for fast search, e.g. *"Vetki Fish Finger"*), `stockNote`, `updatedAt`, `updatedByStaffName`.
13. **`audit_logs`**: `id (PK, UUID)`, `timestamp (Index)`, `actorStaffId`, `actorName`, `actorRole`, `actionType` (`BILL_CREATED`, `BILL_CANCELLED`, `RATE_UPDATED`, `FREEZER_UPDATED`, `VISITOR_CORRECTED`, `VOICE_DRAFT_CONFIRMED`), `entityType`, `entityId`, `detailsJson`, `syncStatus`.
14. **`sync_queue`**: `id (PK, UUID)`, `entityType`, `entityId (Unique per operation)`, `operation` (`UPSERT`, `CANCEL`, `PHOTO_UPLOAD`), `payloadJson`, `retryCount`, `status` (`PENDING`, `IN_FLIGHT`, `SYNCED`, `FAILED`), `lastAttemptAt`, `createdAt`.

### Interactive Component & State Mapping
- **POS Checkout & Price Snapshot Integrity**: Adding items computes `lineTotal = quantity * unitPriceSnapshot`. Editing a product's rate in Settings updates `products.currentRate` for future bills while leaving `transaction_items.unitPriceSnapshot` on past bills untouched.
- **Non-Destructive Bill Cancellation**: Cancelling a bill requires Manager/Owner permission and a mandatory reason; it transitions `transactions.status` to `CANCELLED`, records `cancelledByStaffId`, appends an `audit_logs` entry, and excludes the cancelled total from net revenue while preserving the row in history.
- **NEW FARM AI Voice & Text Pipeline (`gemini-3.8-flash`)**:
  - User taps the microphone or speaks the wake phrase → Android `SpeechRecognizer` captures Bengali/Hindi/English speech → ViewModel builds a prompt enriched with live store context (today's sales total, visitor count, product rates, and 7-freezer section mappings) and calls `Firebase.ai.generativeModel("gemini-3.8-flash")` with `responseSchema` → UI renders the assistant's reply, speaks it via `TextToSpeech`, and if the command is a sale (e.g., *"এক কেজি Salmon Fish বিক্রি হয়েছে"*), presents the **Voice Draft Bill Confirmation Modal** requiring explicit staff tap on **Confirm & Save Bill** before writing to the database.
