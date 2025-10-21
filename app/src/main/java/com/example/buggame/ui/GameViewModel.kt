package com.example.buggame.ui

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.example.buggame.model.ScoreEntity
import com.example.buggame.data.ScoreRepository
import com.example.buggame.utils.PlayerManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class GameViewModel(private val scoreRepository: ScoreRepository) : ViewModel() {
    private val _gameState = mutableStateOf(GameState.NOT_STARTED)
    val gameState: State<GameState> = _gameState

    private val _score = mutableStateOf(0)
    val score: State<Int> = _score

    // ... другие состояния (misses, bugs, isTiltMode, tiltX, tiltY и т.д.)

    init {
        launchBonusSpawner()
    }

    private fun launchBonusSpawner() {
        viewModelScope.launch {
            while (true) {
                delay(15000)
                // Спавн бонуса, обновление состояния
            }
        }
    }

    fun saveScore() {
        viewModelScope.launch {
            val scoreEntity = ScoreEntity(
                playerId = PlayerManager.getCurrentPlayerId(),
                score = _score.value,
                difficulty = PlayerManager.getCurrentPlayerDifficulty(),
                timestamp = System.currentTimeMillis()
            )
            scoreRepository.insertScore(scoreEntity)
        }
    }

    // Методы для бонуса, наклона, золотого таракана и т.д.
}