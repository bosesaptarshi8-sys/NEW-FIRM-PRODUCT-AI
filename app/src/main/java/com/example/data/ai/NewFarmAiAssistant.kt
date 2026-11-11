package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "NewFarmAiAssistant"

data class AiVoiceDraftBill(
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitRate: Double,
    val lineTotal: Double
)

data class AiAssistantResponse(
    val replyText: String,
    val detectedLanguage: String, // "bn", "hi", "en"
    val intentType: String, // SALES_SUMMARY, FREEZER_LOOKUP, OPEN_FREEZER, VISITOR_COUNT, DRAFT_BILL, GENERAL_HELP
    val targetFreezerNumber: Int = 0,
    val targetSectionCode: String = "",
    val draftBill: AiVoiceDraftBill? = null
)

class NewFarmAiAssistant {

    private val assistantSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(
                description = "Warm, helpful, natural response from NEW FARM AI in the user's language (Bengali, Hindi, or English)."
            ),
            "detectedLanguage" to Schema.enumeration(
                values = listOf("bn", "hi", "en"),
                description = "Language code of the reply."
            ),
            "intentType" to Schema.enumeration(
                values = listOf(
                    "SALES_SUMMARY",
                    "FREEZER_LOOKUP",
                    "OPEN_FREEZER",
                    "VISITOR_COUNT",
                    "DRAFT_BILL",
                    "GENERAL_HELP"
                ),
                description = "Classified retail operation intent."
            ),
            "targetFreezerNumber" to Schema.integer(
                description = "Freezer number 1 to 7 if relevant, or 0 if not applicable."
            ),
            "targetSectionCode" to Schema.string(
                description = "Section code like 'Section B' if relevant, or empty string."
            ),
            "draftProductName" to Schema.string(
                description = "Exact matched product name when intentType is DRAFT_BILL, else empty string."
            ),
            "draftQuantity" to Schema.double(
                description = "Numeric quantity when intentType is DRAFT_BILL, else 0.0."
            ),
            "draftUnit" to Schema.string(
                description = "Unit type (KG, GRAM, PIECE, PACKET) when intentType is DRAFT_BILL, else empty string."
            ),
            "draftRate" to Schema.double(
                description = "Unit selling rate in INR when intentType is DRAFT_BILL, else 0.0."
            ),
            "draftTotal" to Schema.double(
                description = "Calculated line total (quantity * draftRate) when intentType is DRAFT_BILL, else 0.0."
            )
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = assistantSchema
    }

    // Mandated lazy initialization of Firebase.ai with gemini-3.8-flash
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun askNewFarmAi(
        userQuery: String,
        storeContextSummary: String
    ): Result<AiAssistantResponse> {
        val systemAndContextPrompt = """
            You are "NEW FARM AI" (নিউ ফার্ম এআই), the intelligent, warm, trustworthy Personal Retail Assistant for NEW FARM PRODUCT in Asansol, West Bengal, India ("50 Years of Trust. A Smarter Way Forward.").
            Wake phrase: "হাই নিউ ফর্ম প্রোডাক্ট" / "Hi New Farm Product".
            Persona: Friendly, respectful Indian Bengali female retail assistant fluent in Bengali (বাংলা), Hindi (हिन्दी), and English.
            
            CRITICAL SAFETY & HONESTY RULES:
            1. Respond in the same language the user used (Bengali if Bengali script or transliterated Bengali, Hindi if Hindi, English if English).
            2. Use ONLY the live store data provided below. Never invent fake sales numbers or fake product prices.
            3. If the user states a sale or asks to bill an item (e.g. "এক কেজি Salmon Fish বিক্রি হয়েছে" or "Bill 2 kg Vetki Fish Finger"), set intentType = "DRAFT_BILL" and populate draftProductName, draftQuantity, draftUnit, draftRate, and draftTotal. Remind the user that the draft bill is ready on screen and requires their explicit confirmation before saving.
            4. If the user asks where an item is stored (e.g. "Vetki Fish Finger কোথায় আছে?"), set intentType = "FREEZER_LOOKUP", set targetFreezerNumber and targetSectionCode from the live mappings, and mention the freezer number and section clearly.
            5. If the user asks to open/show a specific freezer (e.g. "Freezer 3-এর ছবি দেখাও"), set intentType = "OPEN_FREEZER" and targetFreezerNumber = 3.
            6. If the user asks about today's sales ("আজ কত বিক্রি হয়েছে?"), set intentType = "SALES_SUMMARY" and quote the exact revenue, completed bills, and buying customers from the live store context.
            7. If the user asks about today's visitors ("আজ কতজন দোকানে এসেছে?"), set intentType = "VISITOR_COUNT" and quote the exact visitor count and buying customer count.
            
            LIVE STORE CONTEXT:
            $storeContextSummary
            
            USER COMMAND / QUESTION:
            $userQuery
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(systemAndContextPrompt)
            }
            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                return Result.failure(IllegalStateException("NEW FARM AI returned an empty response."))
            }

            val json = JSONObject(jsonString)
            val replyText = json.optString("replyText", "আমি আপনার অনুরোধটি প্রক্রিয়া করেছি।")
            val detectedLang = json.optString("detectedLanguage", "bn")
            val intentType = json.optString("intentType", "GENERAL_HELP")
            val freezerNum = json.optInt("targetFreezerNumber", 0)
            val sectionCode = json.optString("targetSectionCode", "")
            val draftProd = json.optString("draftProductName", "")
            val draftQty = json.optDouble("draftQuantity", 0.0)
            val draftUnit = json.optString("draftUnit", "KG")
            val draftRate = json.optDouble("draftRate", 0.0)
            val draftTotal = json.optDouble("draftTotal", draftQty * draftRate)

            val draftBill = if (intentType == "DRAFT_BILL" && draftProd.isNotBlank() && draftQty > 0.0) {
                AiVoiceDraftBill(
                    productName = draftProd,
                    quantity = draftQty,
                    unit = draftUnit.ifBlank { "KG" },
                    unitRate = draftRate,
                    lineTotal = if (draftTotal > 0.0) draftTotal else draftQty * draftRate
                )
            } else {
                null
            }

            Result.success(
                AiAssistantResponse(
                    replyText = replyText,
                    detectedLanguage = detectedLang,
                    intentType = intentType,
                    targetFreezerNumber = freezerNum,
                    targetSectionCode = sectionCode,
                    draftBill = draftBill
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini 3.8 Flash inference timed out", e)
            Result.failure(Exception("NEW FARM AI timed out after 60 seconds. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 3.8 Flash inference failed", e)
            Result.failure(e)
        }
    }
}
