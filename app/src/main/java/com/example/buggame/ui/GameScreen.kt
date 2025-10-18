package com.example.buggame.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Импорт для Arrangement
import androidx.compose.foundation.layout.Arrangement

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.buggame.GameSettings
import com.example.buggame.utils.PlayerManager
import com.example.buggame.R
import com.example.buggame.model.ScoreEntity
import com.example.buggame.scoreRepository

enum class GameState {
    NOT_STARTED, RUNNING, PAUSED, FINISHED
}

enum class BugType {
    ANT, BEETLE, SPIDER
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val currentPlayer = PlayerManager.getCurrentPlayerName()

    // Получаем сложность из регистрации
    val playerDifficulty = remember { PlayerManager.getCurrentPlayerDifficulty() }

    var score by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.NOT_STARTED) }
    var bugs by remember { mutableStateOf(emptyList<VisualBug>()) }
    var misses by remember { mutableStateOf(0) }
    var lastBugId by remember { mutableStateOf(0) }

    // Используем настройки напрямую из GameSettings
    var roundTimeLeft by remember { mutableStateOf(GameSettings.roundDuration) }
    var showHitEffect by remember { mutableStateOf<Int?>(null) }

    // Получаем актуальные настройки
    val actualGameSpeed = GameSettings.gameSpeed
    val actualMaxBugs = GameSettings.maxBugs
    val actualRoundDuration = GameSettings.roundDuration

    // Отладочная информация
    var debugInfo by remember { mutableStateOf("") }

    // Переменная для хранения времени при паузе
    var savedTime by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    // Игровой цикл
    LaunchedEffect(key1 = gameState) {
        if (gameState == GameState.RUNNING) {
            // Если была пауза, восстанавливаем время, иначе начинаем заново
            if (savedTime > 0) {
                roundTimeLeft = savedTime
                savedTime = 0f
            } else {
                roundTimeLeft = actualRoundDuration
            }

            debugInfo = "Игра началась. Сложность: $playerDifficulty, Макс жуков: $actualMaxBugs"

            while (true) {
                delay(16) // 60 FPS для плавной анимации
                if (gameState != GameState.RUNNING) break

                roundTimeLeft -= 0.016f

                if (roundTimeLeft <= 0) {
                    gameState = GameState.FINISHED
                    break
                }

                // Обновляем позиции жуков с учетом скорости игры
                bugs = bugs.map { bug ->
                    var newX = bug.x + bug.speedX * actualGameSpeed
                    var newY = bug.y + bug.speedY * actualGameSpeed
                    var newSpeedX = bug.speedX
                    var newSpeedY = bug.speedY

                    // Отскок от границ с небольшим случайным изменением
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

                // ИСПРАВЛЕННАЯ ЛОГИКА ПОЯВЛЕНИЯ ЖУКОВ
                if (bugs.size < actualMaxBugs) {
                    // Увеличиваем шанс появления и делаем его более предсказуемым
                    val spawnChance = when {
                        bugs.isEmpty() -> 0.3f // Если жуков нет, высокий шанс появления
                        bugs.size < actualMaxBugs / 2 -> 0.1f
                        else -> 0.05f
                    }

                    if (Random.nextFloat() < spawnChance) {
                        lastBugId++
                        val newBug = createRandomVisualBug(lastBugId, playerDifficulty, actualGameSpeed)
                        bugs = bugs + newBug
                        debugInfo = "Создан жук #$lastBugId. Всего: ${bugs.size}/$actualMaxBugs"
                    }
                }
            }
        } else if (gameState == GameState.PAUSED) {
            // Сохраняем текущее время при паузе
            savedTime = roundTimeLeft
        }
    }

    // Эффект для анимации попадания
    LaunchedEffect(key1 = showHitEffect) {
        showHitEffect?.let {
            delay(300)
            showHitEffect = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // КРАСИВЫЙ СИНЕ-ГОЛУБОЙ ГРАДИЕНТ
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A2980),
                            Color(0xFF26D0CE),
                            Color(0xFF1A2980)
                        )
                    )
                )
        )

        // Область для промахов ПОД жуками
        if (gameState == GameState.RUNNING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        misses++
                        score = (score - 2).coerceAtLeast(0)
                        debugInfo = "Промах! Очки: $score, Промахи: $misses"
                    }
            )
        }

        // Отображение жуков
        bugs.forEach { bug ->
            VisualBugItem(
                bug = bug,
                onClick = {
                    if (gameState == GameState.RUNNING) {
                        val points = when (bug.type) {
                            BugType.ANT -> 10
                            BugType.BEETLE -> 20
                            BugType.SPIDER -> 30
                        }
                        score += points
                        bugs = bugs.filter { it.id != bug.id }
                        showHitEffect = bug.id
                        debugInfo = "Попадание! +$points очков. Осталось жуков: ${bugs.size - 1}"
                    }
                }
            )
        }

        // Эффект попадания
        showHitEffect?.let { hitBugId ->
            val bug = bugs.find { it.id == hitBugId }
            bug?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (it.x * ctx.resources.displayMetrics.widthPixels - 60).dp,
                            y = (it.y * ctx.resources.displayMetrics.heightPixels - 60).dp
                        )
                        .size(120.dp)
                ) {
                    Text(
                        "+${when (it.type) {
                            BugType.ANT -> 10
                            BugType.BEETLE -> 20
                            BugType.SPIDER -> 30
                        }}",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }

        // Интерфейс поверх игры
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Верхняя панель с информацией и кнопкой паузы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Очки: $score",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Промахи: $misses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Игрок: $currentPlayer",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Сложность: $playerDifficulty",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Жуков: ${bugs.size}/$actualMaxBugs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    if (gameState == GameState.RUNNING) {
                        Text(
                            text = "Время: ${roundTimeLeft.toInt()} сек",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Скорость: ${"%.1f".format(actualGameSpeed)}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    // Отладочная информация
                    if (debugInfo.isNotEmpty()) {
                        Text(
                            text = debugInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Yellow
                        )
                    }
                }

                // Кнопка паузы/продолжения в правом верхнем углу (компактный вариант с иконками)
                if (gameState == GameState.RUNNING || gameState == GameState.PAUSED) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.width(90.dp)
                    ) {
                        if (gameState == GameState.RUNNING) {
                            Button(
                                onClick = {
                                    gameState = GameState.PAUSED
                                    debugInfo = "Игра на паузе"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.width(90.dp).height(36.dp)
                            ) {
                                Text("⏸️", fontSize = 14.sp)
                            }
                        } else if (gameState == GameState.PAUSED) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Button(
                                    onClick = {
                                        gameState = GameState.RUNNING
                                        debugInfo = "Игра продолжена"
                                    },
                                    modifier = Modifier.width(90.dp).height(36.dp)
                                ) {
                                    Text("▶️", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        gameState = GameState.NOT_STARTED
                                        score = 0
                                        misses = 0
                                        bugs = emptyList()
                                        savedTime = 0f
                                        debugInfo = "Новая игра"
                                    },
                                    modifier = Modifier.width(90.dp).height(36.dp)
                                ) {
                                    Text("🔄", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Центральные кнопки управления игрой (только для NOT_STARTED и FINISHED)
            when (gameState) {
                GameState.NOT_STARTED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = {
                                gameState = GameState.RUNNING
                                score = 0
                                misses = 0
                                bugs = emptyList()
                                lastBugId = 0
                                savedTime = 0f
                                debugInfo = "Новая игра. Сложность: $playerDifficulty, Макс жуков: $actualMaxBugs"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("Начать игру", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Текущие настройки:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Text("• Скорость: ${"%.1f".format(actualGameSpeed)}x", color = Color.Black)
                                Text("• Макс жуков: $actualMaxBugs", color = Color.Black)
                                Text("• Длительность: ${actualRoundDuration.toInt()} сек", color = Color.Black)
                                Text("• Сложность: $playerDifficulty", color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Как играть:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Text("• Кликайте на насекомых", color = Color.Black)
                                Text("• Муравей = 10 очков", color = Color.Black)
                                Text("• Жук = 20 очков", color = Color.Black)
                                Text("• Паук = 30 очков", color = Color.Black)
                                Text("• Промах = -2 очка", color = Color.Red)
                            }
                        }
                    }
                }
                GameState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Раунд завершен!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Итоговый счет: $score",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.Black
                                )
                                Text(
                                    "Промахов: $misses",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                                Text(
                                    "Сложность: $playerDifficulty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        gameState = GameState.NOT_STARTED
                                        score = 0
                                        misses = 0
                                        bugs = emptyList()
                                        savedTime = 0f
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Новая игра")
                                }
                                LaunchedEffect(Unit) {
                                    if (score > 0 && PlayerManager.getCurrentPlayerId() > 0) {
                                        coroutineScope.launch {
                                            val scoreEntity = ScoreEntity(
                                                playerId = PlayerManager.getCurrentPlayerId(),
                                                score = score,
                                                difficulty = playerDifficulty,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            scoreRepository.insertScore(scoreEntity)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Для RUNNING и PAUSED - пустой spacer, так как кнопки уже в верхнем правом углу
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VisualBugItem(
    bug: VisualBug,
    onClick: () -> Unit
) {
    val bugImage = when (bug.type) {
        BugType.ANT -> R.drawable.bug_ant
        BugType.BEETLE -> R.drawable.bug_beetle
        BugType.SPIDER -> R.drawable.bug_spider
    }

    Box(
        modifier = Modifier
            .offset(
                x = (bug.x * LocalContext.current.resources.displayMetrics.widthPixels - 50).dp,
                y = (bug.y * LocalContext.current.resources.displayMetrics.heightPixels - 50).dp
            )
            .size(100.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = bugImage),
            contentDescription = "Насекомое",
            modifier = Modifier
                .fillMaxSize()
                .rotate(bug.rotation),
            contentScale = ContentScale.Fit
        )
    }
}

data class VisualBug(
    val id: Int,
    val x: Float,
    val y: Float,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    val type: BugType
)

fun createRandomVisualBug(id: Int, difficulty: Int, gameSpeed: Float = 1f): VisualBug {
    val baseSpeed = 0.004f

    val difficultyMultiplier = when (difficulty) {
        1 -> 0.5f  // Легко - медленные
        2 -> 1.0f  // Нормально
        3 -> 2.0f  // Сложно - быстрые
        4 -> 2.5f  // Очень сложно
        5 -> 3.0f  // Эксперт
        else -> 1.0f
    }

    val finalSpeed = baseSpeed * difficultyMultiplier * gameSpeed

    return VisualBug(
        id = id,
        x = Random.nextFloat() * 0.8f + 0.1f,
        y = Random.nextFloat() * 0.8f + 0.1f,
        speedX = (Random.nextFloat() - 0.5f) * finalSpeed,
        speedY = (Random.nextFloat() - 0.5f) * finalSpeed,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 3f,
        type = BugType.entries.random()
    )
}