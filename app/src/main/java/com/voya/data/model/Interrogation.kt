package com.voya.data.model

import com.example.voya.R

data class InterrogationLine(
    val audioResId: Int,
    val caption: String,
    val expectsResponse: Boolean = false
)

// Fixed script: only questions hand the microphone to Maya.
val interrogationLines = listOf(
    InterrogationLine(R.raw.listen, "Listen to me very carefully."),
    InterrogationLine(R.raw.no_time, "I don't have time for games."),
    InterrogationLine(R.raw.question_1, "Where is your father?", true),
    InterrogationLine(R.raw.question_2, "When did you last have contact with him?", true),
    InterrogationLine(R.raw.vitals, "Check her vitals."),
    InterrogationLine(R.raw.check1, "Biometrics stable. Consistency high. Deception probability: low.\n"),
    InterrogationLine(R.raw.lie_number_2, "That was lie number two. Last chance."),
    InterrogationLine(R.raw.consequences, "Lie to me one more time and there will be consequences."),
    InterrogationLine(R.raw.next_words, "Think very carefully about your next words before they leave your mouth."),
    InterrogationLine(R.raw.smartphone, "Check her smartphone."),
    InterrogationLine(R.raw.check2, "Incoming call: Dad — five minutes ago."),
    InterrogationLine(R.raw.dementia, "Looks like someone is suffering from dementia."),
    InterrogationLine(R.raw.mocking_help, "Let's give your memory a little help."),
    InterrogationLine(R.raw.no_cooparation, "If you don't cooperate, I'll take you with me and we'll continue this conversation somewhere else."),
    InterrogationLine(R.raw.question_3, "What did he tell you?", true)
)

data class InterrogationState(
    val lineIndex: Int = 0,
    val awaitingResponse: Boolean = false
) {
    val currentLine: InterrogationLine? get() = interrogationLines.getOrNull(lineIndex)

    fun audioFinished(): InterrogationState = when {
        currentLine == null || awaitingResponse -> this
        currentLine?.expectsResponse == true -> copy(awaitingResponse = true)
        else -> copy(lineIndex = lineIndex + 1)
    }

    fun responseRecorded(): InterrogationState = if (awaitingResponse) {
        copy(lineIndex = lineIndex + 1, awaitingResponse = false)
    } else {
        this
    }
}
