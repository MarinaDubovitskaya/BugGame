package com.example.buggame.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buggame.GameSettings
import com.example.buggame.utils.PlayerManager
import com.example.buggame.R
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.media.MediaPlayer
import org.koin.androidx.compose.getViewModel
import android.app.Activity
import android.view.WindowManager
import kotlinx.coroutines.launch


@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val currentPlayer = PlayerManager.getCurrentPlayerName()
    val playerDifficulty = PlayerManager.getCurrentPlayerDifficulty()

    // --- Получаем ViewModel и все состояния из Koin ---
    val viewModel: GameViewModel = getViewModel()
    val goldRate by viewModel.goldRate.collectAsState()
    val score by viewModel.score.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val bugs by viewModel.bugs.collectAsState()
    val misses by viewModel.misses.collectAsState()
    val bonuses by viewModel.bonuses.collectAsState()
    val gravityEnabled by viewModel.gravityEnabled.collectAsState()
    val roundTimeLeft by viewModel.roundTimeLeft.collectAsState()
    val hitEffect by viewModel.hitEffect.collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()

    // --- Настройки (считываем напрямую) ---
    val actualGameSpeed = GameSettings.gameSpeed
    val actualMaxBugs = GameSettings.maxBugs
    val actualRoundDuration = GameSettings.roundDuration
    val actualBonusInterval = GameSettings.bonusInterval
    val goldText = if (goldRate <= 0.0) "N/A" else "%.2f".format(goldRate)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // --- Сенсор (оставляем в Composable) ---
    val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Регистрируем слушатель только когда игра действительно идёт (чтобы не жрать батарею)
    DisposableEffect(gameState) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    try {
                        val ax = it.values[0]
                        val ay = it.values[1]
                        val az = if (it.values.size > 2) it.values[2] else 0f

                        // Используем текущую ориентацию экрана (isLandscape определён выше в GameScreen)
                        val (screenX, screenY) = if (!isLandscape) {
                            // portrait: "вниз" — это +ay на экране
                            Pair(-ax, ay)
                        } else {
                            // landscape: подбираем соответствие осей, чтобы "вниз" на экране соответствовал падению жуков
                            // (если поведение нужно инвертировать — поменяй знаки здесь)
                            Pair(-ay, -ax)
                        }

                        viewModel.updateGravity(
                            x = screenX / SensorManager.GRAVITY_EARTH,
                            y = screenY / SensorManager.GRAVITY_EARTH
                        )
                    } catch (e: Exception) {
                        // безопасно игнорируем ошибки сенсора
                    }
                }
            }


            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gameState == GameState.RUNNING) {
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        onDispose {
            try {
                sensorManager.unregisterListener(listener)
            } catch (_: Exception) {}
        }
    }

    // --- Воспроизведение звука (используем applicationContext чтобы избежать утечек при повороте) ---
    val appCtx = ctx.applicationContext
    val mediaPlayer = remember { MediaPlayer.create(appCtx, R.raw.bug_scream) }

    LaunchedEffect(gravityEnabled) {
        if (gravityEnabled) {
            try {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            } catch (e: Exception) {
                // Игнорируем ошибки воспроизведения
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            } catch (_: Exception) {}
            mediaPlayer.release()
        }
    }


    // --- Главный Игровой Цикл (LAUNCHEDEFFECT) УДАЛЕН ---
    // Вся логика цикла теперь находится в GameViewModel
    LaunchedEffect(isLandscape) {
        if (gameState == GameState.RUNNING) {
            viewModel.pauseGame()
        }
    }

    val bugSizeDp = if (isLandscape) 80f else 100f  // Уменьшаем размер в landscape
    val bugHalfSizeDp = bugSizeDp / 2f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth.value
        val screenHeight = maxHeight.value

        val topUiHeightEstimate = if (isLandscape) 64f else 180f
        val halfDp = bugHalfSizeDp

        // фракционные границы 0..1
        val minXFrac = (halfDp / screenWidth).coerceIn(0f, 0.45f)
        val maxXFrac = (1f - halfDp / screenWidth).coerceIn(0.55f, 1f)

        val topUiFrac = (topUiHeightEstimate / screenHeight).coerceAtLeast(0f)
        var minYFrac = ((halfDp / screenHeight) + topUiFrac)
        var maxYFrac = (1f - halfDp / screenHeight)

        // Защита от схлопывания диапазона (гарантируем минимальный вертикальный запас)
        val minRange = 0.15f
        if (maxYFrac - minYFrac < minRange) {
            val center = ((minYFrac + maxYFrac) / 2f).coerceIn(0.5f - 0.4f, 0.5f + 0.4f)
            minYFrac = (center - minRange / 2f).coerceAtLeast(0f)
            maxYFrac = (center + minRange / 2f).coerceAtMost(1f)
        }

        // Ремап позиций при изменении видимой области — сохраняет относительные позиции
        val prevBounds = remember { mutableStateOf(floatArrayOf(minXFrac, maxXFrac, minYFrac, maxYFrac)) }
        LaunchedEffect(minXFrac, maxXFrac, minYFrac, maxYFrac) {
            val old = prevBounds.value
            // old: oldMinX, oldMaxX, oldMinY, oldMaxY
            if (old[0] != minXFrac || old[1] != maxXFrac || old[2] != minYFrac || old[3] != maxYFrac) {
                viewModel.remapPositions(old[0], old[1], old[2], old[3], minXFrac, maxXFrac, minYFrac, maxYFrac)
                prevBounds.value = floatArrayOf(minXFrac, maxXFrac, minYFrac, maxYFrac)
            }
        }

        // --- фон ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF1A2980))
                    )
                )
        )

        // область промаха
        if (gameState == GameState.RUNNING) {
            Box(modifier = Modifier.fillMaxSize().clickable {
                viewModel.onMissClick()
            })
        }

        // --- Отображение Жуков ---
        bugs.forEach { bug ->
            val clampedXFrac = bug.x.coerceIn(minXFrac, maxXFrac)
            val clampedYFrac = bug.y.coerceIn(minYFrac, maxYFrac)

            val bugX = (clampedXFrac * screenWidth - halfDp).dp
            val bugY = (clampedYFrac * screenHeight - halfDp).dp

            VisualBugItem(
                bug = bug,
                xPos = bugX,
                yPos = bugY,
                size = bugSizeDp.dp,
                onClick = { viewModel.onBugClick(bug.id) }
            )
        }

        // --- Отображение Бонусов ---
        bonuses.forEach { bonus ->
            val bonusX = ((bonus.x).coerceIn(minXFrac, maxXFrac) * screenWidth - halfDp).dp
            val bonusY = ((bonus.y).coerceIn(minYFrac, maxYFrac) * screenHeight - halfDp).dp

            Box(
                modifier = Modifier
                    .offset(x = bonusX, y = bonusY)
                    .size(bugSizeDp.dp)
                    .clickable { viewModel.onBonusClick(bonus.id) }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bonus_icon),
                    contentDescription = "Бонус",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // --- Эффект Попадания ---
        hitEffect?.let { he ->
            val hitX = ((he.x).coerceIn(minXFrac, maxXFrac) * screenWidth - halfDp - 10f).dp
            val hitY = ((he.y).coerceIn(minYFrac, maxYFrac) * screenHeight - halfDp - 10f).dp

            Box(modifier = Modifier.offset(x = hitX, y = hitY).size(120.dp)) {
                Text("+${he.points}", color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
            }
        }


        // --- UI Сверху (Панель информации) ---
        val uiModifier = if (isLandscape) Modifier.fillMaxHeight().padding(16.dp) else Modifier.fillMaxSize().padding(16.dp)
        val arrangement = if (isLandscape) Arrangement.Center else Arrangement.SpaceBetween

        Row(modifier = uiModifier, horizontalArrangement = arrangement, verticalAlignment = Alignment.Top) {
            // Левая колонка (Инфо)
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

            // Правая колонка (Кнопки Паузы/Продолжить)
            if (gameState == GameState.RUNNING || gameState == GameState.PAUSED) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(90.dp)) {
                    if (gameState == GameState.RUNNING) {
                        Button(onClick = { viewModel.pauseGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.width(90.dp).height(36.dp)) {
                            Text("⏸️", fontSize = 14.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Button(onClick = { viewModel.resumeGame() }, modifier = Modifier.width(90.dp).height(36.dp)) { Text("▶️", fontSize = 14.sp) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { viewModel.resetGame() }, modifier = Modifier.width(90.dp).height(36.dp)) { Text("🔄", fontSize = 14.sp) }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))

            // --- Центральные Элементы (Старт / Конец Игры) ---
            when (gameState) {
                GameState.NOT_STARTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Button(
                            onClick = { viewModel.startGame() }, // Вызываем метод VM
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
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
                                Button(
                                    onClick = { viewModel.resetGame() }, // Вызываем метод VM
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Новая игра")
                                }
                                // LaunchedEffect для сохранения УДАЛЕН.
                                // VM сам сохраняет счет при переходе в FINISHED.
                            }
                        }
                    }
                }
                else -> Spacer(modifier = Modifier.weight(1f)) // RUNNING или PAUSED
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


@Composable
fun VisualBugItem(
    bug: VisualBug,
    xPos: androidx.compose.ui.unit.Dp, // Принимаем готовые Dp
    yPos: androidx.compose.ui.unit.Dp,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val bugImage = when (bug.type) {
        BugType.ANT -> R.drawable.bug_ant
        BugType.BEETLE -> R.drawable.bug_beetle
        BugType.SPIDER -> R.drawable.bug_spider
        BugType.GOLD -> R.drawable.bug_gold
    }

    Box(
        modifier = Modifier
            .offset(x = xPos, y = yPos)
            .size(size)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = bugImage),
            contentDescription = "Насекомое",
            modifier = Modifier.fillMaxSize().rotate(bug.rotation),
            contentScale = ContentScale.Fit
        )
    }
}