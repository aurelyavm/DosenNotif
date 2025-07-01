package com.example.dosennotif.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.io.IOException

class AssetsJsonUploader(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()
    private val TAG = "AssetsJsonUploader"

    // ======================================
    // READ JSON FROM ASSETS
    // ======================================

    /**
     * Read JSON file from assets folder
     */
    private fun readJsonFromAssets(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading $fileName from assets: ${e.message}")
            null
        }
    }

    /**
     * Parse JSON string to List of Maps
     */
    private fun parseJsonToList(jsonString: String): List<Map<String, Any>> {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
            emptyList()
        }
    }

    // ======================================
    // UPLOAD FUNCTIONS
    // ======================================

    /**
     * Upload all JSON files from assets to Firebase
     */
    suspend fun uploadAllFromAssets(): Boolean {
        return try {
            Log.d(TAG, "📤 Starting upload from assets...")

            // Read JSON files from assets
            val prodi3Data = readAndParseAssets("prodi_3.json")
            val prodi4Data = readAndParseAssets("prodi_4.json")
            val prodi6Data = readAndParseAssets("prodi_6.json")
            val prodi58Data = readAndParseAssets("prodi_58.json")

            // Combine all data
            val allData = mapOf(
                "prodi_3" to prodi3Data,
                "prodi_4" to prodi4Data,
                "prodi_6" to prodi6Data,
                "prodi_58" to prodi58Data
            )

            // Calculate stats
            val totalSchedules = prodi3Data.size + prodi4Data.size + prodi6Data.size + prodi58Data.size
            val uniqueLecturers = allData.values.flatten()
                .mapNotNull { it["nidn_dosen"] as? String }
                .distinct()
                .size

            // Prepare document data
            val documentData = mapOf(
                "data" to allData,
                "last_updated" to System.currentTimeMillis(),
                "source" to "assets_upload",
                "stats" to mapOf(
                    "total_schedules" to totalSchedules,
                    "by_prodi" to mapOf(
                        "3" to prodi3Data.size,
                        "4" to prodi4Data.size,
                        "6" to prodi6Data.size,
                        "58" to prodi58Data.size
                    ),
                    "unique_lecturers" to uniqueLecturers
                )
            )

            // Upload to Firebase
            firestore.collection("api_responses")
                .document("periode_20242")
                .set(documentData)
                .await()

            Log.d(TAG, "✅ Upload successful!")
            Log.d(TAG, "📊 Total schedules: $totalSchedules")
            Log.d(TAG, "👨‍🏫 Unique lecturers: $uniqueLecturers")

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed: ${e.message}", e)
            false
        }
    }

    /**
     * Upload from single combined JSON file
     */
    suspend fun uploadFromCombinedJson(): Boolean {
        return try {
            Log.d(TAG, "📤 Starting upload from combined JSON...")

            val combinedJsonString = readJsonFromAssets("api_responses.json")
            if (combinedJsonString == null) {
                Log.e(TAG, "Failed to read api_responses.json")
                return false
            }

            // Parse combined JSON
            val combinedData = try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(combinedJsonString, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing combined JSON: ${e.message}")
                return false
            }

            // Add metadata
            val documentData = combinedData.toMutableMap()
            documentData["last_updated"] = System.currentTimeMillis()
            documentData["source"] = "combined_assets_upload"

            // Upload to Firebase
            firestore.collection("api_responses")
                .document("periode_20242")
                .set(documentData)
                .await()

            Log.d(TAG, "✅ Combined upload successful!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Combined upload failed: ${e.message}", e)
            false
        }
    }

    /**
     * Helper: Read and parse single JSON file
     */
    private fun readAndParseAssets(fileName: String): List<Map<String, Any>> {
        val jsonString = readJsonFromAssets(fileName)
        return if (jsonString != null) {
            parseJsonToList(jsonString)
        } else {
            Log.w(TAG, "File $fileName not found, using empty list")
            emptyList()
        }
    }

    // ======================================
    // VERIFICATION FUNCTIONS
    // ======================================

    /**
     * Check what files exist in assets
     */
    fun checkAssetsFiles(): List<String> {
        return try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            val jsonFiles = assetFiles.filter { it.endsWith(".json") }

            Log.d(TAG, "📁 Found JSON files in assets: $jsonFiles")
            jsonFiles
        } catch (e: Exception) {
            Log.e(TAG, "Error checking assets: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get file info from assets
     */
    fun getAssetFileInfo(fileName: String): String {
        return try {
            val jsonString = readJsonFromAssets(fileName)
            if (jsonString != null) {
                val data = parseJsonToList(jsonString)
                "📄 $fileName: ${data.size} records, ${jsonString.length} chars"
            } else {
                "❌ $fileName: Not found"
            }
        } catch (e: Exception) {
            "❌ $fileName: Error - ${e.message}"
        }
    }

    /**
     * Test read specific lecturer from assets
     */
    fun testReadLecturerFromAssets(nidn: String): List<Map<String, Any>> {
        val allFiles = listOf("prodi_3.json", "prodi_4.json", "prodi_6.json", "prodi_58.json")
        val result = mutableListOf<Map<String, Any>>()

        allFiles.forEach { fileName ->
            val data = readAndParseAssets(fileName)
            val filtered = data.filter { it["nidn_dosen"] == nidn }
            result.addAll(filtered)
        }

        Log.d(TAG, "🔍 Found ${result.size} schedules for lecturer $nidn in assets")
        return result
    }
}