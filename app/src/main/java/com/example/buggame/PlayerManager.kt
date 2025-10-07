package com.example.buggame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlayerManager {
    var currentPlayer: RegisteredPlayer? by mutableStateOf(null)

    fun setPlayer(player: RegisteredPlayer) {
        currentPlayer = player
    }

    fun getCurrentPlayerName(): String {
        return currentPlayer?.fullName ?: "Игрок"
    }

    fun getCurrentPlayerDifficulty(): Int {
        return currentPlayer?.difficulty ?: 1
    }
}