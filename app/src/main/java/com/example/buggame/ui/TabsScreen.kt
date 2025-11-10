package com.example.buggame.ui

import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.buggame.GameSettings
import com.example.buggame.R
// Импортируем Koin get
import org.koin.androidx.compose.get
import com.example.buggame.data.PlayerRepository
import com.example.buggame.data.ScoreRepository

/**
 * TabsScreen — содержит 6 вкладок.
 * Теперь он использует Koin для передачи зависимостей во вкладки (Registration, Records)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(modifier: Modifier = Modifier) {
    val tabs = listOf("Регистрация", "Правила", "Авторы", "Настройки", "Игра", "Рекорды")
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (selectedTab) {
                // 0. RegistrationScreen теперь получает PlayerRepository из Koin (по умолчанию)
                0 -> RegistrationScreen(modifier = Modifier.fillMaxSize())
                1 -> RulesTab(modifier = Modifier.fillMaxSize())
                2 -> AuthorsTab(modifier = Modifier.fillMaxSize().padding(8.dp))
                3 -> SettingsTab(modifier = Modifier.fillMaxSize().padding(8.dp))
                // 4. GameScreen получает свой ViewModel из Koin (по умолчанию)
                4 -> GameScreen(modifier = Modifier.fillMaxSize())
                // 5. RecordsTab теперь получает репозитории из Koin
                5 -> RecordsTab(modifier = Modifier.fillMaxSize().padding(8.dp))
            }
        }
    }
}

@Composable
private fun RulesTab(modifier: Modifier = Modifier) {
    val html = stringResource(id = R.string.rules_html)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        modifier = modifier
    )
}

data class Author(val name: String, @DrawableRes val photoRes: Int? = null)

@Composable
private fun AuthorsTab(modifier: Modifier = Modifier) {
    val authors = listOf(
        Author("Чистых Ксения", null),
        Author("Дубовицкая Марина", null),
    )

    val ctx = LocalContext.current
    LazyColumn(modifier = modifier) {
        items(authors) { author ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (author.photoRes != null) {
                    Image(
                        painter = painterResource(id = author.photoRes),
                        contentDescription = "photo",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 12.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 12.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = author.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Студент группы ИП-215", style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "Подробнее",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            Toast.makeText(ctx, "Автор: ${author.name}", Toast.LENGTH_SHORT).show()
                        }
                        .padding(8.dp)
                )
            }
            Divider()
        }
    }
}

@Composable
private fun SettingsTab(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Настройки игры", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))

        Text("Скорость игры: ${"%.1f".format(GameSettings.gameSpeed)}x")
        Slider(
            value = GameSettings.gameSpeed,
            onValueChange = { GameSettings.gameSpeed = it },
            valueRange = 0.5f..3f,
            steps = 10,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Макс тараканов на экране: ${GameSettings.maxBugs}")
        Slider(
            value = GameSettings.maxBugs.toFloat(),
            onValueChange = { GameSettings.maxBugs = it.toInt() },
            valueRange = 1f..20f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Интервал появления бонусов (сек): ${GameSettings.bonusInterval.toInt()}")
        Slider(
            value = GameSettings.bonusInterval,
            onValueChange = { GameSettings.bonusInterval = it },
            valueRange = 1f..60f,
            steps = 59,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Длительность раунда (сек): ${GameSettings.roundDuration.toInt()}")
        Slider(
            value = GameSettings.roundDuration,
            onValueChange = { GameSettings.roundDuration = it },
            valueRange = 10f..600f,
            steps = 59,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        Row {
            Button(onClick = {
                Toast.makeText(
                    ctx,
                    "Сохранено: speed=${"%.1f".format(GameSettings.gameSpeed)} " +
                            "max=${GameSettings.maxBugs} " +
                            "bonus=${GameSettings.bonusInterval.toInt()} " +
                            "dur=${GameSettings.roundDuration.toInt()}",
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Text("Сохранить настройки")
            }

            Spacer(Modifier.width(12.dp))

            OutlinedButton(onClick = {
                GameSettings.resetToDefaults()
                Toast.makeText(ctx, "Восстановлены значения по умолчанию", Toast.LENGTH_SHORT).show()
            }) {
                Text("Сбросить")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Информация о текущих настройках
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Текущие настройки:", style = MaterialTheme.typography.titleSmall)
                Text("• Скорость: ${"%.1f".format(GameSettings.gameSpeed)}x")
                Text("• Макс жуков: ${GameSettings.maxBugs}")
                Text("• Интервал бонусов: ${GameSettings.bonusInterval.toInt()} сек")
                Text("• Длительность раунда: ${GameSettings.roundDuration.toInt()} сек")
            }
        }
    }
}

@Composable
private fun RecordsTab(
    modifier: Modifier = Modifier,
    // Внедряем зависимости через Koin
    scoreRepository: ScoreRepository = get(),
    playerRepository: PlayerRepository = get()
) {
    // Используем внедренные репозитории
    val allScores by scoreRepository.getAllScores().collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val coroutineScope = rememberCoroutineScope()

    // produceState для сопоставления ID игрока с именем
    val playerNames by produceState<Map<Long, String>>(initialValue = emptyMap(), key1 = allScores) {
        val map = mutableMapOf<Long, String>()
        coroutineScope.launch {
            allScores.forEach { score ->
                // Используем внедренный репозиторий
                val player = playerRepository.getPlayerById(score.playerId)
                map[score.playerId] = player?.fullName ?: "Неизвестный игрок"
                Log.d("RecordsTab", "Mapped ${score.playerId} to ${player?.fullName ?: "null"}")
            }
            value = map
        }
    }

    LazyColumn(modifier = modifier) {
        items(allScores) { score ->
            Card(modifier = Modifier.padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Игрок: ${playerNames[score.playerId] ?: "..."}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Очки: ${score.score}")
                    Text("Сложность: ${score.difficulty}")
                    Text("Дата: ${dateFormat.format(score.timestamp)}")
                }
            }
        }
    }
}