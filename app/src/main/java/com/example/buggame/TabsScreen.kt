package com.example.buggame

import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * TabsScreen — содержит 4 вкладки:
 * 0 Registration (использует существующий RegistrationScreen)
 * 1 Rules (HTML из ресурсов -> WebView)
 * 2 Authors (кастомизированный list: фото + имя)
 * 3 Settings (скорость, макс тараканов, интервал бонусов, длительность раунда)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(modifier: Modifier = Modifier) {
    val tabs = listOf("Регистрация", "Правила", "Авторы", "Настройки")
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
                0 -> RegistrationScreen(modifier = Modifier.fillMaxSize())
                1 -> RulesTab(modifier = Modifier.fillMaxSize())
                2 -> AuthorsTab(modifier = Modifier.fillMaxSize().padding(8.dp))
                3 -> SettingsTab(modifier = Modifier.fillMaxSize().padding(8.dp))
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

    var speed by rememberSaveable { mutableStateOf(1f) }          // 0.5 .. 3.0
    var maxCockroaches by rememberSaveable { mutableStateOf(5f) } // 1..50
    var bonusInterval by rememberSaveable { mutableStateOf(10f) } // сек
    var roundDuration by rememberSaveable { mutableStateOf(60f) } // сек

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Настройки игры", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))

        Text("Скорость игры: ${"%.1f".format(speed)}x")
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..3f, steps = 10, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Text("Макс тараканов на экране: ${maxCockroaches.toInt()}")
        Slider(value = maxCockroaches, onValueChange = { maxCockroaches = it }, valueRange = 1f..50f, steps = 49, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Text("Интервал появления бонусов (сек): ${bonusInterval.toInt()}")
        Slider(value = bonusInterval, onValueChange = { bonusInterval = it }, valueRange = 1f..60f, steps = 59, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Text("Длительность раунда (сек): ${roundDuration.toInt()}")
        Slider(value = roundDuration, onValueChange = { roundDuration = it }, valueRange = 10f..600f, steps = 59, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(18.dp))

        Row {
            Button(onClick = {
                Toast.makeText(
                    ctx,
                    "Сохранено: speed=${"%.1f".format(speed)} max=${maxCockroaches.toInt()} bonus=${bonusInterval.toInt()} dur=${roundDuration.toInt()}",
                    Toast.LENGTH_LONG
                ).show()
                // TODO: сохранить в DataStore/SharedPreferences
            }) {
                Text("Сохранить настройки")
            }

            Spacer(Modifier.width(12.dp))

            OutlinedButton(onClick = {
                speed = 1f
                maxCockroaches = 5f
                bonusInterval = 10f
                roundDuration = 60f
                Toast.makeText(ctx, "Восстановлены значения по умолчанию", Toast.LENGTH_SHORT).show()
            }) {
                Text("Сбросить")
            }
        }
    }
}
