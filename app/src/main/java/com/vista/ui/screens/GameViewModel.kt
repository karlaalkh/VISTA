package com.vista.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.R
import com.vista.data.DialogueClient
import com.vista.data.DialogueReply
import com.vista.data.DialogueService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class GamePhase { OPENING, INTRO, RECORDING, PROCESSING, REPLY, ERROR, COMPLETE }

data class GameUiState(
    val phase: GamePhase = GamePhase.OPENING, val opening: Boolean = true, val introIndex: Int = 0,
    val dialogueId: String = "Q1_wo_ist_dein_vater", val question: String = "Where is your father?",
    val suspicion: Int = 0, val detectedLieCount: Int = 0, val reply: DialogueReply? = null,
    val audioFile: File? = null, val segmentIndex: Int = 0, val error: String? = null
)

class GameViewModel(private val client: DialogueService = DialogueClient()) : ViewModel() {
    private val mutableState = MutableStateFlow(GameUiState())
    val state = mutableState.asStateFlow()
    private val history = mutableListOf<Pair<String, String>>()
    private var recording: File? = null
    private var replyFiles: List<File> = emptyList()

    val intro = listOf(
        R.raw.listen to "Listen to me very carefully.",
        R.raw.no_time to "I don't have time for games.",
        R.raw.question_1 to "Where is your father?"
    )

    fun prepareOpening(cacheDir: File) {
        if (state.value.phase == GamePhase.OPENING) requestAnswer(cacheDir)
    }

    fun introFinished() {
        val current = state.value
        if (current.phase != GamePhase.INTRO) return
        mutableState.value = if (current.introIndex < intro.lastIndex) {
            current.copy(introIndex = current.introIndex + 1)
        } else current.copy(phase = GamePhase.RECORDING)
    }

    fun answer(file: File?, cacheDir: File) {
        if (state.value.phase != GamePhase.RECORDING) {
            file?.delete()
            return
        }
        recording?.delete()
        recording = file
        requestAnswer(cacheDir)
    }

    fun retry(cacheDir: File) {
        if (state.value.phase == GamePhase.ERROR) requestAnswer(cacheDir)
    }

    fun recordAgain() {
        if (state.value.phase != GamePhase.ERROR || state.value.opening) return
        recording?.delete()
        recording = null
        mutableState.value = state.value.copy(phase = GamePhase.RECORDING, error = null)
    }

    private fun requestAnswer(cacheDir: File) {
        val current = state.value
        mutableState.value = current.copy(phase = GamePhase.PROCESSING, error = null)
        viewModelScope.launch {
            try {
                val reply = if (current.opening) {
                    val segments = client.opening()
                    DialogueReply("", segments.joinToString(" ") { it.text }, current.dialogueId,
                        "StartDialogue", current.suspicion, "", segments, current.detectedLieCount)
                } else client.answer(current.dialogueId, current.suspicion, history.toList(), recording,
                    current.detectedLieCount)
                val files = mutableListOf<File>()
                try {
                    reply.segments.forEach { segment ->
                        val suffix = if (segment.mimeType == "audio/wav") ".wav" else ".mp3"
                        val file = File.createTempFile("dialogue_", suffix, cacheDir)
                        files += file
                        file.writeBytes(segment.audio)
                    }
                } catch (e: Exception) {
                    files.forEach { it.delete() }
                    throw e
                }
                replyFiles.forEach { it.delete() }
                replyFiles = files
                recording?.delete()
                recording = null
                if (!current.opening) history += current.question to reply.transcript
                mutableState.value = current.copy(phase = GamePhase.REPLY, reply = reply,
                    audioFile = files.first(), segmentIndex = 0, suspicion = reply.suspicion,
                    detectedLieCount = reply.detectedLieCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = current.copy(phase = GamePhase.ERROR,
                    error = when (e) {
                        is java.net.ConnectException, is java.net.SocketTimeoutException ->
                            "Could not reach the dialogue server. Check the connection and retry."
                        is java.io.IOException, is IllegalArgumentException -> e.message
                        else -> "Could not process this answer. Please retry."
                    })
            }
        }
    }

    fun replyFinished() {
        val current = state.value
        if (current.phase != GamePhase.REPLY) return
        val reply = current.reply ?: return
        if (current.segmentIndex < reply.segments.lastIndex) {
            val next = current.segmentIndex + 1
            mutableState.value = current.copy(segmentIndex = next, audioFile = replyFiles[next])
            return
        }
        if (current.opening) {
            mutableState.value = current.copy(phase = GamePhase.INTRO, opening = false, reply = null,
                audioFile = null, segmentIndex = 0)
            return
        }
        mutableState.value = if (reply.action == "StartDialogue") {
            current.copy(phase = GamePhase.RECORDING, dialogueId = reply.nextDialogue,
                question = questions[reply.nextDialogue] ?: reply.text)
        } else current.copy(phase = GamePhase.COMPLETE)
    }

    fun reset() {
        viewModelScope.coroutineContext.cancelChildren()
        recording?.delete()
        replyFiles.forEach { it.delete() }
        recording = null
        replyFiles = emptyList()
        history.clear()
        mutableState.value = GameUiState()
    }

    override fun onCleared() {
        recording?.delete()
        replyFiles.forEach { it.delete() }
    }

    private val questions = mapOf(
        "Q1_wo_ist_dein_vater" to "Where is your father?",
        "Q2_sag_mir_wo_er_ist" to "Tell me where he is.",
        "Q3_wann_hattest_du_zuletzt_kontakt" to "When did you last have contact with your father?",
        "Q4_was_hat_er_gesagt" to "What did he tell you?",
        "Q5_wo_befindet_sich_diese_werkstatt" to "Where is this workshop?",
        "Q6_wo_befindet_sich_diese_werkstatt" to "Where is this workshop?"
    )
}
