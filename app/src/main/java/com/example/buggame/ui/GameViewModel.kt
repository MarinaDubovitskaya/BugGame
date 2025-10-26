/*package com.example.buggame.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buggame.GameSettings
import com.example.buggame.R
import com.example.buggame.model.ScoreEntity
import com.example.buggame.scoreRepository
import com.example.buggame.utils.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// Состояние UI, которое будет наблюдать Composable
data class GameUiState(
    val score: Int = 0,
    val gameState: GameState = GameState.NOT_STARTED,
    val bugs: List<VisualBug> = emptyList(),
    val bonuses: List<VisualBonus> = emptyList(), // Для бонусов
    val misses: Int = 0,
    val roundTimeLeft: Float = GameSettings.roundDuration,
    val showHitEffect: Int? = null,
    val debugInfo: String = "",
    val currentPlayerName: String = "Игрок",
    val playerDifficulty: Int = 1,
    // Настройки из GameSettings
    val actualGameSpeed: Float = GameSettings.gameSpeed,
    val actualMaxBugs: Int = GameSettings.maxBugs,
    val actualRoundDuration: Float = GameSettings.roundDuration,
    // Состояние для бонуса
    val isBonusActive: Boolean = false,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f
)

// Модели для бонусов (аналогично VisualBug)
data class VisualBonus(
    val id: Int,
    val x: Float,
    val y: Float
)

class GameViewModel(
    private val application: Application // Инжектим контекст для сенсора и звука
) : ViewModel(), SensorEventListener {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var savedTime: Float = 0f
    private var lastBugId: Int = 0
    private var lastBonusId: Int = 0
    private var bonusSpawnTimer: Float = GameSettings.bonusInterval

    // Для сенсора
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    // Для звука
    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(2).build()

    // ❗ ВАЖНО: Эта строка вызовет ошибку, пока вы не добавите файл 'bug_scream.mp3'
    // в папку 'res/raw'.
    private val bugScreamSoundId: Int = soundPool.load(application, R.raw.bug_scream, 1)

    init {
        // Загружаем имя игрока и сложность при инициализации
        _uiState.update {
            it.copy(
                currentPlayerName = PlayerManager.getCurrentPlayerName(),
                playerDifficulty = PlayerManager.getCurrentPlayerDifficulty()
            )
        }
    }

    // --- Управление сенсором ---

    private fun startSensor() {
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(this)
        _uiState.update { it.copy(gravityX = 0f, gravityY = 0f) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (_uiState.value.isBonusActive && event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            _uiState.update {
                it.copy(
                    // Подбираем коэффициенты для комфортной игры
                    gravityX = event.values[0] * -0.05f,
                    gravityY = event.values[1] * 0.05f
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Не используется */ }

    // --- Управление звуком ---

    private fun playBugScream() {
        if (bugScreamSoundId != 0) {
            soundPool.play(bugScreamSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // --- Публичные методы (события от UI) ---

    fun startGame() {
        // 1. Сбрасываем переменные ViewModel
        savedTime = 0f
        lastBugId = 0
        lastBonusId = 0
        bonusSpawnTimer = GameSettings.bonusInterval

        // 2. Сбрасываем состояние UI
        _uiState.update {
            it.copy(
                gameState = GameState.RUNNING,
                score = 0,
                misses = 0,
                bugs = emptyList(),
                bonuses = emptyList(),
                isBonusActive = false,
                roundTimeLeft = it.actualRoundDuration,
                debugInfo = "Новая игра. Сложность: ${it.playerDifficulty}, Макс жуков: ${it.actualMaxBugs}"
            )
        }

        // 3. Запускаем цикл
        startGameLoop()
    }

    fun pauseGame() {
        if (_uiState.value.gameState != GameState.RUNNING) return
        gameLoopJob?.cancel()
        gameLoopJob = null
        savedTime = _uiState.value.roundTimeLeft
        stopSensor() // Останавливаем сенсор на паузе
        _uiState.update { it.copy(gameState = GameState.PAUSED, debugInfo = "Игра на паузе") }
    }

    fun resumeGame() {
        if (_uiState.value.gameState != GameState.PAUSED) return
        _uiState.update { it.copy(gameState = GameState.RUNNING, debugInfo = "Игра продолжена") }
        if (savedTime > 0) {
            _uiState.update { it.copy(roundTimeLeft = savedTime) }
            savedTime = 0f
        }
        if (_uiState.value.isBonusActive) {
            startSensor() // Возобновляем сенсор, если бонус был активен
        }
        startGameLoop()
    }

    fun resetGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        stopSensor()
        _uiState.update {
            it.copy(
                gameState = GameState.NOT_STARTED,
                score = 0,
                misses = 0,
                bugs = emptyList(),
                bonuses = emptyList(),
                isBonusActive = false,
                debugInfo = "Новая игра"
            )
        }
        savedTime = 0f
    }

    fun onBugClicked(bug: VisualBug) {
        if (_uiState.value.gameState != GameState.RUNNING) return

        val points = when (bug.type) {
            BugType.ANT -> 10
            BugType.BEETLE -> 20
            BugType.SPIDER -> 30
        }
        val newBugs = _uiState.value.bugs.filter { it.id != bug.id }
        _uiState.update {
            it.copy(
                score = it.score + points,
                bugs = newBugs,
                showHitEffect = bug.id,
                debugInfo = "Попадание! +$points очков. Осталось жуков: ${newBugs.size}"
            )
        }
        // Запускаем эффект попадания
        viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(showHitEffect = null) }
        }
    }

    fun onBonusClicked(bonus: VisualBonus) {
        if (_uiState.value.gameState != GameState.RUNNING) return

        playBugScream() // 🔊 ЗВУК!
        startSensor()   //  accelerometer ВКЛ

        _uiState.update {
            it.copy(
                isBonusActive = true,
                bonuses = it.bonuses.filter { b -> b.id != bonus.id },
                debugInfo = "БОНУС АКТИВИРОВАН!"
            )
        }

        // Бонус длится 5 секунд
        viewModelScope.launch {
            delay(5000)
            if (_uiState.value.isBonusActive) { // Проверяем, что игра не сбросилась
                _uiState.update { it.copy(isBonusActive = false, debugInfo = "Бонус истек") }
                stopSensor() // accelerometer ВЫКЛ
            }
        }
    }

    fun onMiss() {
        if (_uiState.value.gameState != GameState.RUNNING) return
        val newScore = (_uiState.value.score - 2).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                misses = it.misses + 1,
                score = newScore,
                debugInfo = "Промах! Очки: $newScore, Промахи: ${it.misses + 1}"
            )
        }
    }

    // --- Приватная логика (Игровой цикл) ---

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60 FPS

                val currentState = _uiState.value
                if (currentState.gameState != GameState.RUNNING) break

                var newTimeLeft = currentState.roundTimeLeft - 0.016f

                // 1. Проверяем конец игры
                if (newTimeLeft <= 0) {
                    finishGame(currentState)
                    break
                }

                // 2. Обновляем таймер бонуса
                updateBonusTimer(0.016f)

                // 3. Обновляем жуков
                val updatedBugs = updateBugPositions(currentState)

                // 4. Порождаем жуков
                val (finalBugs, debugMsg) = spawnBugs(updatedBugs, currentState)

                // 5. Обновляем состояние
                _uiState.update {
                    it.copy(
                        roundTimeLeft = newTimeLeft,
                        bugs = finalBugs,
                        bonuses = _uiState.value.bonuses, // Убедимся, что бонусы тоже обновляются
                        // Обновляем debug info, только если оно изменилось
                        debugInfo = debugMsg ?: it.debugInfo
                    )
                }
            }
        }
    }

    private fun updateBugPositions(currentState: GameUiState): List<VisualBug> {
        return currentState.bugs.map { bug ->
            var newX = bug.x + bug.speedX * currentState.actualGameSpeed
            var newY = bug.y + bug.speedY * currentState.actualGameSpeed

            // ⭐ ЛОГИКА БОНУСА (Гравитация) ⭐
            if (currentState.isBonusActive) {
                newX += currentState.gravityX
                newY += currentState.gravityY
            }

            var newSpeedX = bug.speedX
            var newSpeedY = bug.speedY

            // Отскок от границ
            if (newX < 0.02f || newX > 0.98f) {
                newSpeedX = -newSpeedX * (0.9f + Random.nextFloat() * 0.2f)
            }
            if (newY < 0.02f || newY > 0.98f) {
                newSpeedY = -newSpeedY * (0.9f + Random.nextFloat() * 0.2f)
            }

            bug.copy(
                x = newX.coerceIn(0.02f, 0.98f),
                y = newY.coerceIn(0.02f, 0.98f),
                speedX = newSpeedX,
                speedY = newSpeedY,
                rotation = bug.rotation + bug.rotationSpeed
            )
        }
    }

    // Эта функция была исправлена
    private fun spawnBugs(
        currentBugs: List<VisualBug>,
        currentState: GameUiState // <- Здесь была опечатка 'GameUiState>'
    ): Pair<List<VisualBug>, String?> {

        var bugs = currentBugs
        var debugMsg: String? = null

        if (bugs.size < currentState.actualMaxBugs) {
            val spawnChance = when {
                bugs.isEmpty() -> 0.3f
                bugs.size < currentState.actualMaxBugs / 2 -> 0.1f
                else -> 0.05f
            }

            if (Random.nextFloat() < spawnChance) {
                lastBugId++ // Используем свойство ViewModel
                val newBug = createRandomVisualBug(
                    lastBugId,
                    currentState.playerDifficulty,
                    currentState.actualGameSpeed
                )
                bugs = bugs + newBug
                debugMsg = "Создан жук #$lastBugId. Всего: ${bugs.size}/${currentState.actualMaxBugs}"
            }
        }
        return Pair(bugs, debugMsg)
    }

    private fun updateBonusTimer(deltaTime: Float) {
        if (_uiState.value.isBonusActive || _uiState.value.bonuses.isNotEmpty()) {
            // Не спавним новый бонус, если один уже активен или на экране
            return
        }

        bonusSpawnTimer -= deltaTime
        if (bonusSpawnTimer <= 0) {
            bonusSpawnTimer = GameSettings.bonusInterval // Сбрасываем таймер
            lastBonusId++
            val newBonus = VisualBonus(
                id = lastBonusId,
                x = Random.nextFloat() * 0.8f + 0.1f,
                y = Random.nextFloat() * 0.8f + 0.1f
            )
            _uiState.update { it.copy(bonuses = it.bonuses + newBonus) }
        }
    }

    private fun finishGame(currentState: GameUiState) {
        stopSensor()
        _uiState.update { it.copy(gameState = GameState.FINISHED, isBonusActive = false) }

        // Сохранение счета
        val playerId = PlayerManager.getCurrentPlayerId()
        if (currentState.score > 0 && playerId > 0) {
            viewModelScope.launch {
                val scoreEntity = ScoreEntity(
                    playerId = playerId,
                    score = currentState.score,
                    difficulty = currentState.playerDifficulty,
                    timestamp = System.currentTimeMillis()
                )
                scoreRepository.insertScore(scoreEntity)
            }
        }
    }

    override fun onCleared() {
        // Обязательно очищаем ресурсы
        sensorManager.unregisterListener(this)
        soundPool.release()
        gameLoopJob?.cancel()
        super.onCleared()
    }
}*/