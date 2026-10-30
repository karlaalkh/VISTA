package com.voya.data

import android.util.Base64
import com.example.voya.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class DialogueSegment(val speaker: String, val text: String, val mimeType: String, val audio: ByteArray,
    val kind: String = "dialogue")

data class DialogueReply(
    val transcript: String, val text: String, val nextDialogue: String,
    val action: String, val suspicion: Int, val reason: String, val segments: List<DialogueSegment>,
    val detectedLieCount: Int = 0
)

interface DialogueService {
    suspend fun opening(): List<DialogueSegment>
    suspend fun answer(dialogueId: String, suspicion: Int, history: List<Pair<String, String>>,
        recording: File?, detectedLieCount: Int = 0): DialogueReply
}

class DialogueClient : DialogueService {
    private fun checkServerVersion() {
        val connection = URI(BuildConfig.DIALOGUE_URL.trimEnd('/') + "/health").toURL()
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            if (connection.responseCode !in 200..299) {
                throw IOException("The dialogue server health check failed. Check the Python terminal.")
            }
            val health = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val features = health.optJSONArray("features")
            val hasDroneChecks = features != null && (0 until features.length()).any {
                features.optString(it) == "drone_checks"
            }
            if (health.optInt("protocolVersion", 0) < 5 || !hasDroneChecks) {
                throw IOException("The dialogue server needs the narrator and drone sound update. In its terminal, press Ctrl+C, " +
                    "then run python server/app.py again in the same terminal. Your recording can be retried.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val connection = URI(BuildConfig.DIALOGUE_URL.trimEnd('/') + path).toURL()
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 240_000
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val success = connection.responseCode in 200..299
            val stream = if (success) connection.inputStream else connection.errorStream
            val json = stream?.bufferedReader()?.use { JSONObject(it.readText()) }
                ?: throw IOException("The dialogue server did not respond.")
            if (!success) throw IOException(json.optString("error", "Could not process this scene."))
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSegments(json: JSONObject): List<DialogueSegment> {
        val parts = json.optJSONArray("segments") ?: throw IOException(
            "The server returned an older audio format. Restart python server/app.py to load the scene update.")
        require(parts.length() in 1..12) { "Invalid audio sequence. Please retry." }
        val segments = (0 until parts.length()).map { index ->
            val part = parts.getJSONObject(index)
            DialogueSegment(part.getString("speaker"), part.getString("text"), part.getString("mimeType"),
                Base64.decode(part.getString("audioBase64"), Base64.DEFAULT), part.optString("kind", "dialogue"))
        }
        require(segments.all { it.audio.isNotEmpty() && it.mimeType in setOf("audio/mpeg", "audio/wav") }) {
            "A scene's audio was missing or invalid. Please retry."
        }
        return segments
    }

    override suspend fun opening(): List<DialogueSegment> = withContext(Dispatchers.IO) {
        checkServerVersion()
        parseSegments(post("/opening", JSONObject()))
    }

    override suspend fun answer(dialogueId: String, suspicion: Int,
        history: List<Pair<String, String>>, recording: File?, detectedLieCount: Int
    ): DialogueReply = withContext(Dispatchers.IO) {
        checkServerVersion()
        require(recording == null || recording.length() in 1..8L * 1024 * 1024) {
            "Recording is too large. Please record a shorter answer."
        }
        val turns = JSONArray()
        history.takeLast(12).forEach { (question, answer) ->
            turns.put(JSONObject().put("question", question).put("answer", answer))
        }
        val body = JSONObject().put("dialogueId", dialogueId)
            .put("suspicion", suspicion).put("history", turns).put("silent", recording == null)
            .put("detectedLieCount", detectedLieCount)
        if (recording != null) {
            body.put("audioBase64", Base64.encodeToString(recording.readBytes(), Base64.NO_WRAP))
        }
        val json = post("/turn", body)
        val action = json.getString("nextAction")
        require(action in setOf("StartDialogue", "FreeRoam", "GameOver", "TakeToStation")) { "Invalid story action." }
        val segments = parseSegments(json)
        if (dialogueId in setOf("Q1_wo_ist_dein_vater", "Q3_wann_hattest_du_zuletzt_kontakt") &&
            action != "GameOver" && segments.none { it.speaker == "Drone" && it.kind == "dialogue" }) {
            throw IOException("The drone report is missing from this reply. Restart the dialogue server, then retry your answer.")
        }
        DialogueReply(
            transcript = json.getString("transcript"), text = json.getString("reply"),
            nextDialogue = json.getString("nextDialogue"), action = action,
            suspicion = json.getInt("suspicion"), reason = json.getString("suspicionReason"),
            segments = segments, detectedLieCount = json.optInt("detectedLieCount", detectedLieCount)
        )
    }
}
