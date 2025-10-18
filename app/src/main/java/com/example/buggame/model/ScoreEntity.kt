package com.example.buggame.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val score: Int,
    val difficulty: Int,
    val timestamp: Long  // Дата/время игры в millis
)