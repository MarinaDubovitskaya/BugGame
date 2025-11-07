package com.example.buggame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buggame.data.CurrencyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _goldRate = MutableStateFlow(0.0)
    val goldRate: StateFlow<Double> = _goldRate

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                refreshOnce()
                delay(60_000L)
            }
        }
    }

    suspend fun refreshOnce() {
        val rate = currencyRepository.fetchGoldRateRUB()
        _goldRate.value = rate
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
