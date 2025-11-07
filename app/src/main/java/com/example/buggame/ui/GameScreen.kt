package com.example.buggame.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.buggame.GameSettings
import com.example.buggame.utils.PlayerManager
import com.example.buggame.R
import com.example.buggame.model.ScoreEntity
import com.example.buggame.scoreRepository
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import org.koin.androidx.compose.getViewModel
import kotlin.math.roundToInt
import android.util.Log

enum class GameState { NOT_STARTED, RUNNING, PAUSED, FINISHED }
enum class BugType { ANT, BEETLE, SPIDER, GOLD }

data class VisualBonus(
    val id: Int,
    val x: Float,
    val y: Float,
    var lifetime: Float = 5f
)

data class HitEffect(val id: Int, val x: Float, val y: Float, val points: Int)
fun computeGoldPointsRobust(goldRate: Double, playerDifficulty: Int): Int {
    val isPerOunce = false
    var divisor = 1000.0
    val difficultyMultiplier = when (playerDifficulty.coerceIn(1, 10)) {
        1 -> 0.5
        2 -> 0.8
        3 -> 1.0
        4 -> 1.2
        5 -> 1.5
        6 -> 1.8
        7 -> 2.1
        8 -> 2.4
        9 -> 2.8
        10 -> 3.2
        else -> 1.0
    }

    val ratePerGram = if (isPerOunce) {

        goldRate / 31.1034768
    } else {
        goldRate
    }

    val safeRate = if (ratePerGram.isFinite() && ratePerGram > 0.0) ratePerGram else 0.0

    val raw = (safeRate / divisor) * difficultyMultiplier
    val pts = raw.roundToInt().coerceAtLeast(1)

    Log.d("GoldPoints", "goldRate=$goldRate ratePerGram=$ratePerGram divisor=$divisor difficulty=$playerDifficulty mult=$difficultyMultiplier -> raw=$raw pts=$pts")

    return pts
}

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val currentPlayer = PlayerManager.getCurrentPlayerName()
    val playerDifficulty = remember { PlayerManager.getCurrentPlayerDifficulty() }

    // Получаем GameViewModel из Koin
    val viewModel: GameViewModel = getViewModel()
    val goldRate by viewModel.goldRate.collectAsState()

    var score by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.NOT_STARTED) }
    var bugs by remember { mutableStateOf(emptyList<VisualBug>()) }
    var misses by remember { mutableStateOf(0) }
    var lastBugId by remember { mutableStateOf(0) }

    // Золотой таракан
    var lastGoldBugSpawnTime by remember { mutableStateOf(0f) }

    // Бонусы
    var bonuses by remember { mutableStateOf(emptyList<VisualBonus>()) }
    var lastBonusId by remember { mutableStateOf(0) }
    var lastBonusSpawnTime by remember { mutableStateOf(0f) }
    var lastBugSpawnTime by remember { mutableStateOf(0f) }
    var elapsedTime by remember { mutableStateOf(0f) }

    // Гравитация
    var gravityEnabled by remember { mutableStateOf(false) }
    var gravityTimer by remember { mutableStateOf(0f) }
    var gravityX by remember { mutableStateOf(0f) }
    var gravityY by remember { mutableStateOf(0f) }

    val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val goldText = if (goldRate <= 0.0) "N/A" else "%.2f".format(goldRate)


    DisposableEffect(gameState) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    gravityX = -it.values[0] / SensorManager.GRAVITY_EARTH
                    gravityY = it.values[1] / SensorManager.GRAVITY_EARTH
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (gameState == GameState.RUNNING) {
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    var roundTimeLeft by remember { mutableStateOf(GameSettings.roundDuration) }
    var hitEffect by remember { mutableStateOf<HitEffect?>(null) }

    val actualGameSpeed = GameSettings.gameSpeed
    val actualMaxBugs = GameSettings.maxBugs
    val actualRoundDuration = GameSettings.roundDuration
    val actualBonusInterval = GameSettings.bonusInterval

    var debugInfo by remember { mutableStateOf("") }
    var savedTime by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val bugSizeDp = 100f
    val bugHalfSizeDp = bugSizeDp / 2f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth.value
        val screenHeight = maxHeight.value

        val topUiHeightEstimate = 200f
        val minX = bugHalfSizeDp / screenWidth
        val maxX = 1f - bugHalfSizeDp / screenWidth
        val minY = (bugHalfSizeDp + topUiHeightEstimate) / screenHeight
        val maxY = 1f - bugHalfSizeDp / screenHeight

        LaunchedEffect(key1 = gameState) {
            if (gameState == GameState.RUNNING) {
                if (savedTime > 0) {
                    roundTimeLeft = savedTime
                    savedTime = 0f
                } else {
                    roundTimeLeft = actualRoundDuration
                    elapsedTime = 0f
                    lastBonusSpawnTime = 0f
                    lastBugSpawnTime = 0f
                    lastGoldBugSpawnTime = 0f
                    gravityEnabled = false
                }

                debugInfo = "Игра началась. Сложность: $playerDifficulty, Макс жуков: $actualMaxBugs"

                while (true) {
                    delay(16)
                    if (gameState != GameState.RUNNING) break

                    roundTimeLeft -= 0.016f
                    elapsedTime += 0.016f

                    if (roundTimeLeft <= 0) {
                        gameState = GameState.FINISHED
                        break
                    }

                    if (gravityEnabled) {
                        gravityTimer -= 0.016f
                        if (gravityTimer <= 0f) {
                            gravityEnabled = false
                            bugs = bugs.map { b ->
                                val factorX = 0.9f + Random.nextFloat() * 0.2f
                                val factorY = 0.9f + Random.nextFloat() * 0.2f
                                b.copy(speedX = b.baseSpeedX * factorX, speedY = b.baseSpeedY * factorY)
                            }
                            debugInfo = "Гравитация завершена"
                        }
                    }

                    if (elapsedTime - lastBonusSpawnTime >= actualBonusInterval) {
                        lastBonusId++
                        val newBonus = VisualBonus(
                            id = lastBonusId,
                            x = Random.nextFloat() * (maxX - minX) + minX,
                            y = Random.nextFloat() * (maxY - minY) + minY
                        )
                        bonuses = bonuses + newBonus
                        lastBonusSpawnTime = elapsedTime
                    }
                    bonuses = bonuses.map { it.copy(lifetime = it.lifetime - 0.016f) }.filter { it.lifetime > 0 }

                    val bugSpawnInterval = 0.5f
                    if (elapsedTime - lastBugSpawnTime >= bugSpawnInterval && bugs.count { it.type != BugType.GOLD } < actualMaxBugs) {
                        lastBugId++
                        val newBug = createRandomVisualBug(lastBugId, playerDifficulty, actualGameSpeed, minX, maxX, minY, maxY)
                        bugs = bugs + newBug
                        lastBugSpawnTime = elapsedTime
                    }

                    val goldInterval = 20f
                    if (elapsedTime - lastGoldBugSpawnTime >= goldInterval) {
                        lastBugId++
                        val gold = createGoldBug(lastBugId, actualGameSpeed, minX, maxX, minY, maxY)
                        bugs = bugs + gold
                        lastGoldBugSpawnTime = elapsedTime
                        debugInfo = "Появился золотой таракан (курс=${"%.2f".format(goldRate)} RUB)"
                    }

                    bugs = bugs.map { bug ->
                        var accelX = 0f
                        var accelY = 0f
                        if (gravityEnabled) {
                            accelX = gravityX * 0.005f
                            accelY = gravityY * 0.005f
                        }

                        var newSpeedX = bug.speedX + accelX
                        var newSpeedY = bug.speedY + accelY

                        if (gravityEnabled) {
                            newSpeedX *= 0.999f
                            newSpeedY *= 0.999f
                        }

                        val maxSpeed = if (bug.type == BugType.GOLD) 0.04f else 0.08f
                        newSpeedX = newSpeedX.coerceIn(-maxSpeed, maxSpeed)
                        newSpeedY = newSpeedY.coerceIn(-maxSpeed, maxSpeed)

                        var newX = bug.x + newSpeedX * actualGameSpeed
                        var newY = bug.y + newSpeedY * actualGameSpeed
                        var newRotation = bug.rotation + bug.rotationSpeed

                        if (newX < minX || newX > maxX) {
                            newSpeedX = -newSpeedX * (0.95f + Random.nextFloat() * 0.05f)
                            newX = newX.coerceIn(minX, maxX)
                        }
                        if (newY < minY || newY > maxY) {
                            newSpeedY = -newSpeedY * (0.95f + Random.nextFloat() * 0.05f)
                            newY = newY.coerceIn(minY, maxY)
                        }

                        bug.copy(
                            x = newX.coerceIn(minX, maxX),
                            y = newY.coerceIn(minY, maxY),
                            speedX = newSpeedX,
                            speedY = newSpeedY,
                            rotation = newRotation
                        )
                    }

                    val avgSpeed = if (bugs.isNotEmpty()) bugs.map { hypot(it.speedX.toDouble(), it.speedY.toDouble()) }.average() else 0.0
                    if (elapsedTime.toInt() % 5 == 0) debugInfo = "Багов:${bugs.size} avgSpeed=${"%.4f".format(avgSpeed)}"
                }
            } else if (gameState == GameState.PAUSED) {
                savedTime = roundTimeLeft
            }
        }

        LaunchedEffect(key1 = hitEffect) {
            hitEffect?.let {
                delay(300)
                hitEffect = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF1A2980))
                    )
                )
        )

        if (gameState == GameState.RUNNING) {
            Box(modifier = Modifier.fillMaxSize().clickable {
                misses++
                score = (score - 2).coerceAtLeast(0)
                debugInfo = "Промах! $score"
            })
        }

        bugs.forEach { bug ->
            VisualBugItem(
                bug = bug,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                bugHalfSizeDp = bugHalfSizeDp,
                onClick = {
                    if (gameState == GameState.RUNNING) {
                        val points = when (bug.type) {
                            BugType.ANT -> 10
                            BugType.BEETLE -> 20
                            BugType.SPIDER -> 30
                            BugType.GOLD -> {
                                computeGoldPointsRobust(goldRate, playerDifficulty)
                            }
                        }
                        // эффект попадания (используем текущие координаты)
                        hitEffect = HitEffect(id = bug.id, x = bug.x, y = bug.y, points = points)
                        bugs = bugs.filter { it.id != bug.id }
                        score += points
                        debugInfo = "Попадание +$points (goldRate=${"%.2f".format(goldRate)})"
                    }
                }
            )
        }

        bonuses.forEach { bonus ->
            Box(
                modifier = Modifier
                    .offset(x = (bonus.x * screenWidth - bugHalfSizeDp).dp, y = (bonus.y * screenHeight - bugHalfSizeDp).dp)
                    .size(bugSizeDp.dp)
                    .clickable {
                        if (gameState == GameState.RUNNING) {
                            bonuses = bonuses.filter { it.id != bonus.id }
                            gravityEnabled = true
                            gravityTimer = 5f
                            try {
                                val mp = MediaPlayer.create(ctx, R.raw.bug_scream)
                                mp?.let { it.setOnCompletionListener { p -> p.release() }; it.start() }
                            } catch (_: Exception) {}
                            debugInfo = "Бонус активирован"
                        }
                    }
            ) {
                Image(painter = painterResource(id = R.drawable.bonus_icon), contentDescription = "Бонус", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }


        hitEffect?.let { he ->
            Box(modifier = Modifier.offset(x = (he.x * screenWidth - bugHalfSizeDp - 10f).dp, y = (he.y * screenHeight - bugHalfSizeDp - 10f).dp).size(120.dp)) {
                Text("+${he.points}", color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Очки: $score", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Промахи: $misses", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Игрок: $currentPlayer", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Сложность: $playerDifficulty", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Жуков: ${bugs.size}/$actualMaxBugs", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    if (gameState == GameState.RUNNING) Text("Время: ${roundTimeLeft.toInt()} сек", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Скорость: ${"%.1f".format(actualGameSpeed)}x", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Гравитация: ${if (gravityEnabled) "Вкл" else "Выкл"}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(text = "Курс золота: $goldText руб", style = MaterialTheme.typography.bodySmall, color = Color.Yellow)
                    if (debugInfo.isNotEmpty()) Text(debugInfo, style = MaterialTheme.typography.bodySmall, color = Color.Yellow)
                }

                if (gameState == GameState.RUNNING || gameState == GameState.PAUSED) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(90.dp)) {
                        if (gameState == GameState.RUNNING) {
                            Button(onClick = { gameState = GameState.PAUSED; debugInfo = "Пауза" }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.width(90.dp).height(36.dp)) {
                                Text("⏸️", fontSize = 14.sp)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Button(onClick = { gameState = GameState.RUNNING; debugInfo = "Продолжено" }, modifier = Modifier.width(90.dp).height(36.dp)) { Text("▶️", fontSize = 14.sp) }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { gameState = GameState.NOT_STARTED; score = 0; misses = 0; bugs = emptyList(); bonuses = emptyList(); savedTime = 0f; debugInfo = "Новая игра" }, modifier = Modifier.width(90.dp).height(36.dp)) { Text("🔄", fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            when (gameState) {
                GameState.NOT_STARTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Button(onClick = {
                            gameState = GameState.RUNNING
                            score = 0; misses = 0; bugs = emptyList(); bonuses = emptyList(); lastBugId = 0; savedTime = 0f
                            coroutineScope.launch { viewModel.refreshOnce() }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)) {
                            Text("Начать игру", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Текущие настройки:", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                Text("• Скорость: ${"%.1f".format(actualGameSpeed)}x", color = Color.Black)
                                Text("• Макс жуков: $actualMaxBugs", color = Color.Black)
                                Text("• Длительность: ${actualRoundDuration.toInt()} сек", color = Color.Black)
                                Text("• Сложность: $playerDifficulty", color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Как играть:", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                Text("• Кликайте на насекомых", color = Color.Black)
                                Text("• Муравей = 10 очков", color = Color.Black)
                                Text("• Жук = 20 очков", color = Color.Black)
                                Text("• Паук = 30 очков", color = Color.Black)
                                Text("• Золотой таракан = очки, пропорциональные курсу золота ЦБ", color = Color.Black)
                                Text("• Промах = -2 очка", color = Color.Red)
                            }
                        }
                    }
                }
                GameState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Раунд завершен!", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Итоговый счет: $score", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                                Text("Промахов: $misses", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                                Text("Сложность: $playerDifficulty", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { gameState = GameState.NOT_STARTED; score = 0; misses = 0; bugs = emptyList(); bonuses = emptyList(); savedTime = 0f }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Новая игра")
                                }
                                LaunchedEffect(Unit) {
                                    if (score > 0 && PlayerManager.getCurrentPlayerId() > 0) {
                                        coroutineScope.launch {
                                            val scoreEntity = ScoreEntity(playerId = PlayerManager.getCurrentPlayerId(), score = score, difficulty = playerDifficulty, timestamp = System.currentTimeMillis())
                                            scoreRepository.insertScore(scoreEntity)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
fun createGoldBug(id: Int, gameSpeed: Float, minX: Float, maxX: Float, minY: Float, maxY: Float): VisualBug {
    val speed = 0.01f
    var speedX = (Random.nextFloat() - 0.5f) * speed * 2f
    var speedY = (Random.nextFloat() - 0.5f) * speed * 2f
    val minSpeed = 0.002f
    if (abs(speedX) < minSpeed) speedX = minSpeed * if (speedX >= 0) 1f else -1f
    if (abs(speedY) < minSpeed) speedY = minSpeed * if (speedY >= 0) 1f else -1f

    return VisualBug(
        id = id,
        x = Random.nextFloat() * (maxX - minX) + minX,
        y = Random.nextFloat() * (maxY - minY) + minY,
        speedX = speedX,
        speedY = speedY,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
        type = BugType.GOLD,
        baseSpeedX = speedX,
        baseSpeedY = speedY
    )
}

@Composable
fun VisualBugItem(bug: VisualBug, screenWidth: Float, screenHeight: Float, bugHalfSizeDp: Float, onClick: () -> Unit) {
    val bugImage = when (bug.type) {
        BugType.ANT -> R.drawable.bug_ant
        BugType.BEETLE -> R.drawable.bug_beetle
        BugType.SPIDER -> R.drawable.bug_spider
        BugType.GOLD -> R.drawable.bug_gold
    }

    Box(
        modifier = Modifier
            .offset(x = (bug.x * screenWidth - bugHalfSizeDp).dp, y = (bug.y * screenHeight - bugHalfSizeDp).dp)
            .size(100.dp)
            .clickable { onClick() }
    ) {
        Image(painter = painterResource(id = bugImage), contentDescription = "Насекомое", modifier = Modifier.fillMaxSize().rotate(bug.rotation), contentScale = ContentScale.Fit)
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
    val type: BugType,
    val baseSpeedX: Float = speedX,
    val baseSpeedY: Float = speedY
)

fun createRandomVisualBug(id: Int, difficulty: Int, gameSpeed: Float = 1f, minX: Float, maxX: Float, minY: Float, maxY: Float): VisualBug {
    val baseSpeed = 0.02f


    val difficultyMultiplier = when (difficulty.coerceIn(1, 10)) {
        1 -> 0.5f
        2 -> 0.8f
        3 -> 1.0f
        4 -> 1.2f
        5 -> 1.5f
        6 -> 1.8f
        7 -> 2.1f
        8 -> 2.4f
        9 -> 2.7f
        10 -> 3.0f
        else -> 1.0f
    }

    val finalSpeed = baseSpeed * difficultyMultiplier * gameSpeed

    var speedX = (Random.nextFloat() - 0.5f) * finalSpeed * 2f
    var speedY = (Random.nextFloat() - 0.5f) * finalSpeed * 2f
    val minSpeed = 0.005f
    if (abs(speedX) < minSpeed) speedX = minSpeed * if (speedX >= 0) 1f else -1f
    if (abs(speedY) < minSpeed) speedY = minSpeed * if (speedY >= 0) 1f else -1f

    return VisualBug(
        id = id,
        x = Random.nextFloat() * (maxX - minX) + minX,
        y = Random.nextFloat() * (maxY - minY) + minY,
        speedX = speedX,
        speedY = speedY,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 3f,
        type = BugType.values().filter { it != BugType.GOLD }.random(),
        baseSpeedX = speedX,
        baseSpeedY = speedY
    )
}

