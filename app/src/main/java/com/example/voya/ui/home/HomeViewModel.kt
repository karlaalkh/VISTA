package com.example.voya.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.voya.data.repository.CounterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: CounterRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = repository.counter
        .map { HomeUiState(count = it.value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun increment() {
        viewModelScope.launch { repository.increment() }
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }

    companion object {
        fun factory(repository: CounterRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
