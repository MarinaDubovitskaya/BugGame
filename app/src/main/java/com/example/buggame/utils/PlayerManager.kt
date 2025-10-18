package com.example.buggame.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.buggame.ui.RegisteredPlayer

object PlayerManager {
    var currentPlayer: RegisteredPlayer? by mutableStateOf(null)

    fun setPlayer(player: RegisteredPlayer) {
        currentPlayer = player
    }
    fun getCurrentPlayerId(): Long = currentPlayer?.id ?: 0

    fun getCurrentPlayerName(): String {
        return currentPlayer?.fullName ?: "Игрок"
    }

    fun getCurrentPlayerDifficulty(): Int {
        return currentPlayer?.difficulty ?: 1
    }
}