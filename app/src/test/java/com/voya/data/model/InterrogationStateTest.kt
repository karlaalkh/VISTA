package com.voya.data.model

import com.example.voya.R
import org.junit.Assert.*
import org.junit.Test

class InterrogationStateTest {
    @Test
    fun scriptRecordsOnlyAfterTheThreeQuestionsAndPlaysEveryClip() {
        var state = InterrogationState()
        val played = mutableListOf<Int>()
        val responsePrompts = mutableListOf<Int>()
        while (state.currentLine != null) {
            val line = state.currentLine!!
            assertFalse(state.awaitingResponse)
            // A recording callback cannot skip unfinished agent audio.
            assertEquals(state, state.responseRecorded())
            played += line.audioResId
            state = state.audioFinished()
            if (state.awaitingResponse) {
                responsePrompts += line.audioResId
                assertEquals(line, state.currentLine)
                assertEquals(state, state.audioFinished())
                state = state.responseRecorded()
            }
        }
        assertEquals(interrogationLines.map { it.audioResId }, played)
        assertEquals(listOf(R.raw.question_1, R.raw.question_2, R.raw.question_3), responsePrompts)
        assertFalse(state.awaitingResponse)
        assertEquals(state, state.audioFinished())
        assertEquals(state, state.responseRecorded())
    }
}
