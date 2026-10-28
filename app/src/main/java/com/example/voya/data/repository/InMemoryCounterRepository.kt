package com.example.voya.data.repository

import com.example.voya.data.model.Counter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Demo storage: survives rotation, but resets when the application process ends. */
class InMemoryCounterRepository : CounterRepository {
    private val mutableCounter = MutableStateFlow(Counter())
    override val counter = mutableCounter.asStateFlow()

    override suspend fun increment() {
        mutableCounter.update { current ->
            current.copy(value = if (current.value < Int.MAX_VALUE) current.value + 1 else current.value)
        }
    }

    override suspend fun reset() {
        mutableCounter.value = Counter()
    }
}
