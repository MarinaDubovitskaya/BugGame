package com.example.buggame

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Long,  // Храним как timestamp (millis)
    val zodiac: String
)

// Конвертеры для Calendar <-> Long (используем в Database)
fun Calendar.toMillis(): Long = this.timeInMillis
fun Long.toCalendar(): Calendar = Calendar.getInstance().apply { timeInMillis = this@toCalendar }