package com.example.voya.data.repository

import com.example.voya.data.model.Counter
import kotlinx.coroutines.flow.Flow

interface CounterRepository {
    val counter: Flow<Counter>
    suspend fun increment()
    suspend fun reset()
}
