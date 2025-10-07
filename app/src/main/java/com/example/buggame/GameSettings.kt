package com.example.buggame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GameSettings {
    var gameSpeed by mutableStateOf(1f)          // 0.5 .. 3.0
    var maxBugs by mutableStateOf(5)             // 1..50
    var bonusInterval by mutableStateOf(10f)     // сек
    var roundDuration by mutableStateOf(60f)     // сек

    fun resetToDefaults() {
        gameSpeed = 1f
        maxBugs = 5
        bonusInterval = 10f
        roundDuration = 60f
    }
}