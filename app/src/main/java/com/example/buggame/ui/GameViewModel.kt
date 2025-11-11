package com.example.buggame.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buggame.GameSettings
import com.example.buggame.data.CurrencyRepository
import com.example.buggame.data.ScoreRepository
import com.example.buggame.model.ScoreEntity
import com.example.buggame.utils.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.math.pow


class GameViewModel(
    private val currencyRepository: CurrencyRepository,
    private val scoreRepository: ScoreRepository // Внедряем ScoreRepository
) : ViewModel() {

    // --- Состояние курса золота (как и было) ---
    private val _goldRate = MutableStateFlow(0.0)
    val goldRate: StateFlow<Double> = _goldRate.asStateFlow()
    private var currencyRefreshJob: Job? = null

    // --- Игровое Состояние (Перенесено из GameScreen) ---
    private val _gameState = MutableStateFlow(GameState.NOT_STARTED)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _misses = MutableStateFlow(0)
    val misses: StateFlow<Int> = _misses.asStateFlow()

    private val _bugs = MutableStateFlow<List<VisualBug>>(emptyList())
    val bugs: StateFlow<List<VisualBug>> = _bugs.asStateFlow()

    private val _bonuses = MutableStateFlow<List<VisualBonus>>(emptyList())
    val bonuses: StateFlow<List<VisualBonus>> = _bonuses.asStateFlow()

    private val _roundTimeLeft = MutableStateFlow(GameSettings.roundDuration)
    val roundTimeLeft: StateFlow<Float> = _roundTimeLeft.asStateFlow()

    private val _hitEffect = MutableStateFlow<HitEffect?>(null)
    val hitEffect: StateFlow<HitEffect?> = _hitEffect.asStateFlow()

    private val _gravityEnabled = MutableStateFlow(false)
    val gravityEnabled: StateFlow<Boolean> = _gravityEnabled.asStateFlow()

    private val _debugInfo = MutableStateFlow("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    // --- Приватные переменные для логики ---
    private var lastBugId = 0
    private var lastBonusId = 0
    private var lastBugSpawnTime = 0f
    private var lastBonusSpawnTime = 0f
    private var lastGoldBugSpawnTime = 0f
    private var elapsedTime = 0f
    private var gravityTimer = 0f
    private var gravityX = 0f
    private var gravityY = 0f

    private var gameLoopJob: Job? = null

    init {
        // Запускаем авто-обновление курса золота
        startAutoRefreshGoldRate()
    }

    // --- Управление Игрой ---

    fun startGame() {
        if (_gameState.value == GameState.RUNNING) return
        resetGameStats()
        _gameState.value = GameState.RUNNING
        _debugInfo.value = "Игра началась. Сложность: ${PlayerManager.getCurrentPlayerDifficulty()}"
        // Запускаем игровой цикл
        startGameLoop()
        // Единоразово обновляем курс золота при старте
        viewModelScope.launch { refreshGoldRateOnce() }
    }

    fun pauseGame() {
        if (_gameState.value != GameState.RUNNING) return
        _gameState.value = GameState.PAUSED
        _debugInfo.value = "Пауза"
        gameLoopJob?.cancel() // Останавливаем цикл
    }

    fun resumeGame() {
        if (_gameState.value != GameState.PAUSED) return
        _gameState.value = GameState.RUNNING
        _debugInfo.value = "Продолжено"
        startGameLoop() // Возобновляем цикл
    }

    fun resetGame() {
        _gameState.value = GameState.NOT_STARTED
        resetGameStats()
        gameLoopJob?.cancel()
    }

    private fun finishGame() {
        _gameState.value = GameState.FINISHED
        gameLoopJob?.cancel()
        // Сохраняем результат
        saveScore()
    }

    private fun resetGameStats() {
        _score.value = 0
        _misses.value = 0
        _bugs.value = emptyList()
        _bonuses.value = emptyList()
        _roundTimeLeft.value = GameSettings.roundDuration
        _hitEffect.value = null
        _gravityEnabled.value = false
        lastBugId = 0
        lastBonusId = 0
        lastBugSpawnTime = 0f
        lastBonusSpawnTime = 0f
        lastGoldBugSpawnTime = 0f
        elapsedTime = 0f
        gravityTimer = 0f
    }

    // --- Игровой Цикл (Логика из LaunchedEffect) ---

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60 FPS
                if (_gameState.value != GameState.RUNNING) break

                val dt = 0.016f // delta time
                _roundTimeLeft.value -= dt
                elapsedTime += dt

                // 1. Проверка на окончание раунда
                if (_roundTimeLeft.value <= 0) {
                    finishGame()
                    break
                }

                // 2. Логика гравитации
                if (_gravityEnabled.value) {
                    gravityTimer -= dt
                    if (gravityTimer <= 0f) {
                        _gravityEnabled.value = false
                        // Восстанавливаем базовую скорость жуков
                        _bugs.value = _bugs.value.map { b ->
                            val factorX = 0.9f + Random.nextFloat() * 0.2f
                            val factorY = 0.9f + Random.nextFloat() * 0.2f
                            b.copy(speedX = b.baseSpeedX * factorX, speedY = b.baseSpeedY * factorY)
                        }
                        _debugInfo.value = "Гравитация завершена"
                    }
                }

                // 3. Спавн бонусов
                if (elapsedTime - lastBonusSpawnTime >= GameSettings.bonusInterval) {
                    lastBonusId++
                    val newBonus = VisualBonus(
                        id = lastBonusId,
                        x = Random.nextFloat(), // Координаты будут ограничены в GameScreen
                        y = Random.nextFloat()
                    )
                    _bonuses.value = _bonuses.value + newBonus
                    lastBonusSpawnTime = elapsedTime
                }
                // Уменьшаем время жизни бонусов
                _bonuses.value = _bonuses.value.map { it.copy(lifetime = it.lifetime - dt) }.filter { it.lifetime > 0 }

                // 4. Спавн обычных жуков
                val bugSpawnInterval = 0.5f
                if (elapsedTime - lastBugSpawnTime >= bugSpawnInterval && _bugs.value.count { it.type != BugType.GOLD } < GameSettings.maxBugs) {
                    lastBugId++
                    val newBug = createRandomVisualBug(lastBugId, PlayerManager.getCurrentPlayerDifficulty(), GameSettings.gameSpeed)
                    _bugs.value = _bugs.value + newBug
                    lastBugSpawnTime = elapsedTime
                }

                // 5. Спавн золотого таракана
                val goldInterval = 20f
                if (elapsedTime - lastGoldBugSpawnTime >= goldInterval) {
                    lastBugId++
                    val gold = createGoldBug(lastBugId, GameSettings.gameSpeed)
                    _bugs.value = _bugs.value + gold
                    lastGoldBugSpawnTime = elapsedTime
                    _debugInfo.value = "Появился золотой таракан (курс=${"%.2f".format(_goldRate.value)} RUB)"
                }

                // 6. Обновление позиций жуков (логика движения)
                _bugs.value = _bugs.value.map { bug ->
                    updateBugPosition(bug, dt, _gravityEnabled.value, gravityX, gravityY, GameSettings.gameSpeed)
                }

                // (опционально) отладка
                if (elapsedTime.toInt() % 5 == 0) {
                    val avgSpeed = if (_bugs.value.isNotEmpty()) _bugs.value.map { hypot(it.speedX.toDouble(), it.speedY.toDouble()) }.average() else 0.0
                    _debugInfo.value = "Багов:${_bugs.value.size} avgSpeed=${"%.4f".format(avgSpeed)}"
                }
            }
        }
    }

    // --- Обработчики Событий (Клики) ---

    fun onBugClick(bugId: Int) {
        if (_gameState.value != GameState.RUNNING) return

        val bug = _bugs.value.find { it.id == bugId } ?: return

        val points = when (bug.type) {
            BugType.ANT -> 10
            BugType.BEETLE -> 20
            BugType.SPIDER -> 30
            BugType.GOLD -> {
                computeGoldPointsRobust(_goldRate.value, PlayerManager.getCurrentPlayerDifficulty())
            }
        }

        _hitEffect.value = HitEffect(id = bug.id, x = bug.x, y = bug.y, points = points)
        _bugs.value = _bugs.value.filter { it.id != bug.id }
        _score.value += points
        _debugInfo.value = "Попадание +$points (goldRate=${"%.2f".format(_goldRate.value)})"

        // Сбрасываем эффект через 300мс
        viewModelScope.launch {
            delay(300)
            if (_hitEffect.value?.id == bug.id) {
                _hitEffect.value = null
            }
        }
    }

    fun onBonusClick(bonusId: Int) {
        if (_gameState.value != GameState.RUNNING) return

        val bonus = _bonuses.value.find { it.id == bonusId } ?: return

        _bonuses.value = _bonuses.value.filter { it.id != bonus.id }
        _gravityEnabled.value = true
        gravityTimer = 5f // 5 секунд гравитации
        _debugInfo.value = "Бонус активирован"
        // Воспроизведение звука остается в GameScreen, так как VM не должен знать о MediaPlayer
    }

    fun onMissClick() {
        if (_gameState.value != GameState.RUNNING) return
        _misses.value += 1
        _score.value = (_score.value - 2).coerceAtLeast(0)
        _debugInfo.value = "Промах! ${_score.value}"
    }

    // --- Обновление Гравитации (от сенсора) ---
    fun updateGravity(x: Float, y: Float) {
        gravityX = x
        gravityY = y
    }

    // --- Логика Сохранения ---
    private fun saveScore() {
        val currentScore = _score.value
        val playerId = PlayerManager.getCurrentPlayerId()
        if (currentScore > 0 && playerId > 0) {
            viewModelScope.launch {
                val scoreEntity = ScoreEntity(
                    playerId = playerId,
                    score = currentScore,
                    difficulty = PlayerManager.getCurrentPlayerDifficulty(),
                    timestamp = System.currentTimeMillis()
                )
                try {
                    scoreRepository.insertScore(scoreEntity)
                    Log.d("GameViewModel", "Score $currentScore saved for player $playerId")
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Failed to save score", e)
                }
            }
        }
    }

    // --- Логика Курса Валют (как и было) ---
    private fun startAutoRefreshGoldRate() {
        currencyRefreshJob?.cancel()
        currencyRefreshJob = viewModelScope.launch {
            while (true) {
                refreshGoldRateOnce()
                delay(60_000L) // обновляем каждую минуту
            }
        }
    }

    suspend fun refreshGoldRateOnce() {
        val rate = currencyRepository.fetchGoldRateRUB()
        if (rate > 0.0) {
            _goldRate.value = rate
        }
    }

    // --- Хелперы (перенесены из GameScreen) ---

    // Замените существующую updateBugPosition на эту:
    private fun updateBugPosition(
        bug: VisualBug,
        dt: Float,
        gravityOn: Boolean,
        gravityX: Float,
        gravityY: Float,
        gameSpeed: Float
    ): VisualBug {
        // frameFactor — количество "60fps кадров" в этом dt
        val frameFactor = (dt / 0.016f).coerceAtLeast(0.001f)

        // коэффициент ускорения от гравитации — подберите (0.02..0.05)
        val gravityAccelFactor = 0.03f

        // аккумулируем ускорение от гравитации (gravityX/Y нормированы в updateGravity)
        val accelX = if (gravityOn) gravityX * gravityAccelFactor else 0f
        val accelY = if (gravityOn) gravityY * gravityAccelFactor else 0f

        // Обновляем скорость (используем frameFactor, чтобы быть совместимым с исходным движком)
        var newSpeedX = bug.speedX + accelX * frameFactor
        var newSpeedY = bug.speedY + accelY * frameFactor

        // Демпфирование (экспоненциальное, а не простой множитель каждый кадр).
        // Используем pow для более стабильного результата при разном frameFactor.
        val baseDamping = if (gravityOn) 0.985f else 0.9975f
        // перевод в экспоненциальную форму: dampingEffective = baseDamping ^ frameFactor
        val dampingEffective = baseDamping.pow(frameFactor)
        newSpeedX *= dampingEffective
        newSpeedY *= dampingEffective

        // Увеличенные разумные пределы скорости (чтобы движение было заметным)
        val maxSpeed = if (bug.type == BugType.GOLD) 0.12f else 0.18f
        newSpeedX = newSpeedX.coerceIn(-maxSpeed, maxSpeed)
        newSpeedY = newSpeedY.coerceIn(-maxSpeed, maxSpeed)

        // Перемещение — используем ту же нормализацию, что и раньше: движение = speed * gameSpeed * frameFactor
        val movedX = newSpeedX * gameSpeed * frameFactor
        val movedY = newSpeedY * gameSpeed * frameFactor

        var finalX = bug.x + movedX
        var finalY = bug.y + movedY
        val newRotation = bug.rotation + bug.rotationSpeed * frameFactor

        // Границы экрана в VM — используем 0..1, но НЕ "схлопываем" их в жесткие 0.05..0.95
        if (finalX < 0f) {
            finalX = 0f
            newSpeedX = -newSpeedX * 0.75f
        } else if (finalX > 1f) {
            finalX = 1f
            newSpeedX = -newSpeedX * 0.75f
        }
        if (finalY < 0f) {
            finalY = 0f
            newSpeedY = -newSpeedY * 0.75f
        } else if (finalY > 1f) {
            finalY = 1f
            newSpeedY = -newSpeedY * 0.75f
        }

        return bug.copy(
            x = finalX,
            y = finalY,
            speedX = newSpeedX,
            speedY = newSpeedY,
            rotation = newRotation
        )
    }

    // Добавьте эту функцию в класс ViewModel — вызывается из GameScreen при изменении видимой области:
    fun remapPositions(
        oldMinX: Float, oldMaxX: Float,
        oldMinY: Float, oldMaxY: Float,
        newMinX: Float, newMaxX: Float,
        newMinY: Float, newMaxY: Float
    ) {
        // защитимся от некорректных диапазонов
        val safeOldWidthX = (oldMaxX - oldMinX).takeIf { it > 0.0001f } ?: 1f
        val safeOldWidthY = (oldMaxY - oldMinY).takeIf { it > 0.0001f } ?: 1f
        val safeNewWidthX = (newMaxX - newMinX).takeIf { it > 0.0001f } ?: 1f
        val safeNewWidthY = (newMaxY - newMinY).takeIf { it > 0.0001f } ?: 1f

        _bugs.value = _bugs.value.map { b ->
            // нормализуем позицию внутри старой области, затем отобразим в новую
            val normX = ((b.x - oldMinX) / safeOldWidthX).coerceIn(0f, 1f)
            val normY = ((b.y - oldMinY) / safeOldWidthY).coerceIn(0f, 1f)
            val mappedX = (newMinX + normX * safeNewWidthX).coerceIn(0f, 1f)
            val mappedY = (newMinY + normY * safeNewWidthY).coerceIn(0f, 1f)
            b.copy(x = mappedX, y = mappedY)
        }
    }



    private fun createRandomVisualBug(id: Int, difficulty: Int, gameSpeed: Float): VisualBug {
        val baseSpeed = 0.02f
        val difficultyMultiplier = when (difficulty.coerceIn(1, 10)) {
            1 -> 0.5f; 2 -> 0.8f; 3 -> 1.0f; 4 -> 1.2f; 5 -> 1.5f
            6 -> 1.8f; 7 -> 2.1f; 8 -> 2.4f; 9 -> 2.7f; 10 -> 3.0f
            else -> 1.0f
        }
        val finalSpeed = baseSpeed * difficultyMultiplier // gameSpeed учтем в updateBugPosition

        var speedX = (Random.nextFloat() - 0.5f) * finalSpeed * 2f
        var speedY = (Random.nextFloat() - 0.5f) * finalSpeed * 2f
        val minSpeed = 0.005f
        if (abs(speedX) < minSpeed) speedX = minSpeed * if (speedX >= 0) 1f else -1f
        if (abs(speedY) < minSpeed) speedY = minSpeed * if (speedY >= 0) 1f else -1f

        return VisualBug(
            id = id,
            x = Random.nextFloat(), // 0.0 .. 1.0
            y = Random.nextFloat(), // 0.0 .. 1.0
            speedX = speedX,
            speedY = speedY,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 3f,
            type = BugType.values().filter { it != BugType.GOLD }.random(),
            baseSpeedX = speedX,
            baseSpeedY = speedY
        )
    }

    private fun createGoldBug(id: Int, gameSpeed: Float): VisualBug {
        val speed = 0.01f // gameSpeed учтем в updateBugPosition
        var speedX = (Random.nextFloat() - 0.5f) * speed * 2f
        var speedY = (Random.nextFloat() - 0.5f) * speed * 2f
        val minSpeed = 0.002f
        if (abs(speedX) < minSpeed) speedX = minSpeed * if (speedX >= 0) 1f else -1f
        if (abs(speedY) < minSpeed) speedY = minSpeed * if (speedY >= 0) 1f else -1f

        return VisualBug(
            id = id,
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            speedX = speedX,
            speedY = speedY,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
            type = BugType.GOLD,
            baseSpeedX = speedX,
            baseSpeedY = speedY
        )
    }

    private fun computeGoldPointsRobust(goldRate: Double, playerDifficulty: Int): Int {
        val isPerOunce = false
        var divisor = 1000.0
        val difficultyMultiplier = when (playerDifficulty.coerceIn(1, 10)) {
            1 -> 0.5; 2 -> 0.8; 3 -> 1.0; 4 -> 1.2; 5 -> 1.5
            6 -> 1.8; 7 -> 2.1; 8 -> 2.4; 9 -> 2.8; 10 -> 3.2
            else -> 1.0
        }
        val ratePerGram = if (isPerOunce) goldRate / 31.1034768 else goldRate
        val safeRate = if (ratePerGram.isFinite() && ratePerGram > 0.0) ratePerGram else 0.0
        val raw = (safeRate / divisor) * difficultyMultiplier
        val pts = raw.roundToInt().coerceAtLeast(1)
        Log.d("GoldPoints", "goldRate=$goldRate ratePerGram=$ratePerGram divisor=$divisor difficulty=$playerDifficulty mult=$difficultyMultiplier -> raw=$raw pts=$pts")
        return pts
    }

    override fun onCleared() {
        currencyRefreshJob?.cancel()
        gameLoopJob?.cancel()
        super.onCleared()
    }
}