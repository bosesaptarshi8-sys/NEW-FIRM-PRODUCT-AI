package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiVoiceDraftBill
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.DeepNavyBlue
import com.example.ui.theme.FreshTeal
import com.example.ui.theme.SubtleOrange
import com.example.ui.theme.SyncedEmerald
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.ChatTurn
import java.util.Locale

@Composable
fun NewFarmAiScreen(
    chatHistory: List<ChatTurn>,
    aiUiState: AiUiState,
    pendingVoiceDraftBill: AiVoiceDraftBill?,
    isWakeWordEnabled: Boolean,
    onToggleWakeWord: (Boolean) -> Unit,
    onSendQuery: (String) -> Unit,
    onConfirmVoiceDraftBill: (AiVoiceDraftBill) -> Unit,
    onDismissVoiceDraftBill: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler(onBack = onNavigateBackHome)

    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var isListeningMic by remember { mutableStateOf(false) }
    var voiceLanguageCode by remember { mutableStateOf("bn-IN") } // bn-IN, hi-IN, en-IN
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    // Configure TextToSpeech with Indian Bengali / Hindi / English warm female voice preference
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val bnLocale = Locale.forLanguageTag("bn-IN")
                val result = engine?.setLanguage(bnLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale.forLanguageTag("en-IN"))
                }
                engine?.setPitch(1.05f)
                engine?.setSpeechRate(0.96f)
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    // Automatically speak new AI responses aloud
    LaunchedEffect(chatHistory.size) {
        val lastTurn = chatHistory.lastOrNull()
        if (lastTurn != null && !lastTurn.isUser && chatHistory.size > 1) {
            val langTag = when (lastTurn.responseMeta?.detectedLanguage) {
                "bn" -> "bn-IN"
                "hi" -> "hi-IN"
                else -> "en-IN"
            }
            ttsEngine?.language = Locale.forLanguageTag(langTag)
            ttsEngine?.speak(lastTurn.text, TextToSpeech.QUEUE_FLUSH, null, "nfp_ai_utterance")
        }
    }

    val startVoiceRecognition: () -> Unit = {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLanguageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningMic = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListeningMic = false
                    }
                    override fun onError(error: Int) {
                        isListeningMic = false
                        recognizer.destroy()
                    }
                    override fun onResults(results: Bundle?) {
                        isListeningMic = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull().orEmpty()
                        if (spoken.isNotBlank()) {
                            val cleanCommand = spoken
                                .replace("হাই নিউ ফর্ম প্রোডাক্ট", "", ignoreCase = true)
                                .replace("Hi New Farm Product", "", ignoreCase = true)
                                .trim()
                                .ifEmpty { spoken }
                            onSendQuery(cleanCommand)
                        }
                        recognizer.destroy()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                recognizer.startListening(intent)
            }
        } catch (_: Exception) {
            isListeningMic = false
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        }
    }

    val exampleCommands = listOf(
        "আজ কত বিক্রি হয়েছে?",
        "Vetki Fish Finger কোথায় আছে?",
        "Freezer 3-এর ছবি দেখাও।",
        "এক কেজি Salmon Fish বিক্রি হয়েছে।",
        "আজ কতজন দোকানে এসেছে?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("new_farm_ai_screen")
    ) {
        // 1. NEW FARM AI Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavyBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SubtleOrange,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "NEW FARM AI",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NEW FARM AI • Gemini 3.8 Flash",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Wake Phrase: “হাই নিউ ফর্ম প্রোডাক্ট” (Bengali • Hindi • English)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Switch(
                        checked = isWakeWordEnabled,
                        onCheckedChange = { enabled ->
                            onToggleWakeWord(enabled)
                            if (enabled) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("wake_word_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Language selector for voice input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "bn-IN" to "বাংলা (BN)",
                        "hi-IN" to "हिन्दी (HI)",
                        "en-IN" to "English (EN)"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = voiceLanguageCode == code,
                            onClick = { voiceLanguageCode = code },
                            label = {
                                Text(
                                    text = label,
                                    color = if (voiceLanguageCode == code) DeepNavyBlue else Color.White
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Voice Billing Safety Confirmation Card (Mandatory confirmation before saving!)
        pendingVoiceDraftBill?.let { draft ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_draft_confirmation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Safety Gate",
                            tint = SubtleOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE BILLING DRAFT — CONFIRMATION REQUIRED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Product: ${draft.productName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quantity: ${draft.quantity} ${draft.unit}  |  Unit Rate: ₹${String.format(Locale.US, "%.2f", draft.unitRate)}/${draft.unit}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total Amount: ₹${String.format(Locale.US, "%,.2f", draft.lineTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavyBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "NEW FARM AI never silently completes financial transactions. Tap Confirm below to save this bill.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismissVoiceDraftBill,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("discard_voice_draft_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discard Draft")
                        }
                        Button(
                            onClick = { onConfirmVoiceDraftBill(draft) },
                            colors = ButtonDefaults.buttonColors(containerColor = SyncedEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_voice_draft_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Confirm", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm & Save Bill")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Example Voice/Chat Command Chips
        Text(
            text = "Tap a Sample Voice Command or Speak • উদাহরণ কমান্ড:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(exampleCommands) { cmd ->
                OutlinedButton(
                    onClick = { onSendQuery(cmd) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("ai_quick_cmd_${cmd.hashCode()}")
                ) {
                    Text(cmd, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Conversation History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("ai_chat_history_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatHistory, key = { it.id }) { turn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (turn.isUser) DeepNavyBlue else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth(0.88f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (turn.isUser) "You (দোকান কর্মী / মালিক)" else "NEW FARM AI • Gemini 3.8 Flash",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (turn.isUser) Color.White.copy(alpha = 0.75f) else FreshTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!turn.isUser) {
                                    IconButton(
                                        onClick = {
                                            ttsEngine?.speak(
                                                turn.text,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "replay_tts"
                                            )
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak Aloud",
                                            tint = FreshTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = turn.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (turn.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (aiUiState is AiUiState.Thinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(
                            text = "NEW FARM AI (Gemini 3.8 Flash) ইজ থিংকিং...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 5. Bottom Voice Mic + Text Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListeningMic) AlertCrimson else SubtleOrange
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("ai_voice_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak to NEW FARM AI",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask in বাংলা, हिन्दी, or English...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_text_input")
            )

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendQuery(textInput)
                        textInput = ""
                    }
                },
                enabled = textInput.isNotBlank() && aiUiState !is AiUiState.Thinking,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
