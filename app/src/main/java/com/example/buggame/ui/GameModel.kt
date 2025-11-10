package com.example.buggame.ui

// Эти классы данных вынесены в отдельный файл,
// чтобы их могли использовать и GameScreen, и GameViewModel.

enum class GameState { NOT_STARTED, RUNNING, PAUSED, FINISHED }
enum class BugType { ANT, BEETLE, SPIDER, GOLD }

data class VisualBonus(
    val id: Int,
    val x: Float,
    val y: Float,
    var lifetime: Float = 5f
)

data class HitEffect(val id: Int, val x: Float, val y: Float, val points: Int)

data class VisualBug(
    val id: Int,
    val x: Float,
    val y: Float,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    val type: BugType,
    val baseSpeedX: Float = speedX,
    val baseSpeedY: Float = speedY
)