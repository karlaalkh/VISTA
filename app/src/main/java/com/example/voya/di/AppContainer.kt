package com.example.voya.di

import com.example.voya.data.repository.CounterRepository
import com.example.voya.data.repository.InMemoryCounterRepository

/** Application-scoped dependencies. Replace implementations here as the app grows. */
class AppContainer {
    val counterRepository: CounterRepository = InMemoryCounterRepository()
}
