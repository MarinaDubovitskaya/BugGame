package com.example.buggame.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import com.example.buggame.model.PlayerEntity
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.*
import com.example.buggame.model.toCalendar
import com.example.buggame.model.toMillis
import com.example.buggame.utils.PlayerManager
import com.example.buggame.ui.theme.BugGameTheme
// Импортируем репозиторий и Koin
import com.example.buggame.data.PlayerRepository
import org.koin.androidx.compose.get

// data class Player (уже есть в старом файле, можно убрать отсюда)
// data class RegisteredPlayer (уже есть в старом файле, можно убрать отсюда)

data class Player(
    val fullName: String,
    val gender: String,
    val course: String
)

data class RegisteredPlayer(
    val id: Long = 0,
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Calendar,
    val zodiac: String
)

@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    // Внедряем зависимость через Koin
    playerRepository: PlayerRepository = get()
) {
    val coroutineScope = rememberCoroutineScope()
    // Используем внедренный репозиторий
    val allPlayers by playerRepository.getAllPlayers().collectAsState(initial = emptyList())

    // A: базовые поля
    var name by rememberSaveable { mutableStateOf("") }
    var isNameError by rememberSaveable { mutableStateOf(false) }
    var selectedGender by rememberSaveable { mutableStateOf("") }
    var selectedCourse by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var resultText by rememberSaveable { mutableStateOf("") }

    // B1: difficulty slider state (1..10)
    var difficultyFloat by rememberSaveable { mutableStateOf(1f) }
    val difficulty: Int = difficultyFloat.toInt()

    // B2: birth date state
    var birthCalendar by rememberSaveable { mutableStateOf(Calendar.getInstance()) }
    val birthLabel by remember(birthCalendar) { derivedStateOf { formatDate(birthCalendar) } }
    val ctx = LocalContext.current

    val genderOptions = listOf("Мужской", "Женский")
    val courseOptions = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Список существующих игроков
        if (allPlayers.isNotEmpty()) {
            Text(
                "Выберите существующего игрока:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.height(150.dp)) {
                items(allPlayers) { player ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val selectedPlayer = RegisteredPlayer(
                                    id = player.id,
                                    fullName = player.fullName,
                                    gender = player.gender,
                                    course = player.course,
                                    difficulty = player.difficulty,
                                    birthDate = player.birthDate.toCalendar(),
                                    zodiac = player.zodiac
                                )
                                PlayerManager.setPlayer(selectedPlayer)
                                resultText = "Выбран игрок: ${player.fullName} (Сложность: ${player.difficulty})"
                            }
                    ) {
                        Text("${player.fullName} - Сложность: ${player.difficulty}", modifier = Modifier.padding(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Или зарегистрируйте нового:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        // Заголовок
        Text(
            text = "Регистрация игрока",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Поле ФИО
        Text(
            text = "ФИО",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                isNameError = false
            },
            label = { Text("Фамилия Имя Отчество") },
            isError = isNameError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium
        )
        if (isNameError) {
            Text(
                text = "Поле не может быть пустым",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Пол
        Text(text = "Пол", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            genderOptions.forEach { gender ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = (selectedGender == gender),
                        onClick = { selectedGender = gender },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(text = gender, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Курс (Dropdown)
        Text(text = "Курс", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCourse,
                onValueChange = {},
                readOnly = true,
                label = { Text("Выберите курс") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Выбрать курс")
                    }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                courseOptions.forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course) },
                        onClick = {
                            selectedCourse = course
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slider (сложность)
        Text(text = "Уровень сложности: $difficulty", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = difficultyFloat,
            onValueChange = { difficultyFloat = it.coerceIn(1f, 10f) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date picker
        Text(text = "Дата рождения", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedButton(
            onClick = {
                showDatePicker(ctx, birthCalendar) { year, month, day ->
                    birthCalendar = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (birthLabel.isBlank()) "Выбрать дату" else "Дата: $birthLabel")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Zodiac preview (text)
        val zodiacPreview = getZodiac(birthCalendar)
        Text(text = "Знак зодиака: $zodiacPreview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        // Zodiac preview (image)
        val zodiacRes = getZodiacDrawableRes(zodiacPreview)
        Image(
            painter = painterResource(id = zodiacRes),
            contentDescription = "Знак зодиака: $zodiacPreview",
            modifier = Modifier
                .size(120.dp)
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Register button
        Button(
            onClick = {
                isNameError = name.isBlank()
                if (isNameError || selectedGender.isEmpty() || selectedCourse.isEmpty()) {
                    resultText = "Заполните все поля"
                    return@Button
                }

                coroutineScope.launch {
                    val existing = allPlayers.find { it.fullName == name.trim() }
                    if (existing != null) {
                        resultText = "Игрок с таким ФИО уже существует"
                        return@launch
                    }

                    val playerEntity = PlayerEntity(
                        fullName = name.trim(),
                        gender = selectedGender,
                        course = selectedCourse,
                        difficulty = difficulty,
                        birthDate = birthCalendar.toMillis(),
                        zodiac = zodiacPreview
                    )
                    // Используем внедренный репозиторий
                    val insertedId = playerRepository.insertPlayer(playerEntity)

                    val registeredPlayer = RegisteredPlayer(
                        id = insertedId,
                        fullName = playerEntity.fullName,
                        gender = playerEntity.gender,
                        course = playerEntity.course,
                        difficulty = playerEntity.difficulty,
                        birthDate = birthCalendar.clone() as Calendar,
                        zodiac = playerEntity.zodiac
                    )
                    PlayerManager.setPlayer(registeredPlayer)

                    resultText = """
                        ✅ Регистрация завершена! ID: $insertedId
                        ФИО: ${registeredPlayer.fullName}
                        Пол: ${registeredPlayer.gender}
                        Курс: ${registeredPlayer.course}
                        Сложность: ${registeredPlayer.difficulty}
                        Дата рождения: ${formatDate(registeredPlayer.birthDate)}
                        Знак зодиака: ${registeredPlayer.zodiac}
                        Теперь можете перейти в игру!
                    """.trimIndent()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Зарегистрироваться", style = MaterialTheme.typography.bodyLarge)
        }

        // Результат
        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// DatePicker helper
private fun showDatePicker(
    context: android.content.Context,
    cal: Calendar,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val dpd = DatePickerDialog(context, { _, y, m, d ->
        onDateSelected(y, m, d)
    }, year, month, day)
    dpd.show()
}

private fun formatDate(cal: Calendar): String {
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    return "$day.$month.$year"
}

// Zodiac calculation
fun getZodiac(day: Int, month: Int): String {
    return when {
        (month == 3 && day >= 21) || (month == 4 && day <= 20) -> "Овен"
        (month == 4 && day >= 21) || (month == 5 && day <= 20) -> "Телец"
        (month == 5 && day >= 21) || (month == 6 && day <= 21) -> "Близнецы"
        (month == 6 && day >= 22) || (month == 7 && day <= 22) -> "Рак"
        (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Лев"
        (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Дева"
        (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Весы"
        (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Скорпион"
        (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Стрелец"
        (month == 12 && day >= 22) || (month == 1 && day <= 20) -> "Козерог"
        (month == 1 && day >= 21) || (month == 2 && day <= 18) -> "Водолей"
        (month == 2 && day >= 19) || (month == 3 && day <= 20) -> "Рыбы"
        else -> "Неизвестно"
    }
}

fun getZodiac(cal: Calendar): String {
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    return getZodiac(day, month)
}

// Функция для изображений знаков зодиака
fun getZodiacDrawableRes(zodiac: String): Int {
    return when (zodiac) {
        "Овен" -> _root_ide_package_.com.example.buggame.R.drawable.img_aries
        "Телец" -> _root_ide_package_.com.example.buggame.R.drawable.img_taurus
        "Близнецы" -> _root_ide_package_.com.example.buggame.R.drawable.img_gemini
        "Рак" -> _root_ide_package_.com.example.buggame.R.drawable.img_cancer
        "Лев" -> _root_ide_package_.com.example.buggame.R.drawable.img_leo
        "Дева" -> _root_ide_package_.com.example.buggame.R.drawable.img_virgo
        "Весы" -> _root_ide_package_.com.example.buggame.R.drawable.img_libra
        "Скорпион" -> _root_ide_package_.com.example.buggame.R.drawable.img_scorpio
        "Стрелец" -> _root_ide_package_.com.example.buggame.R.drawable.img_sagittarius
        "Козерог" -> _root_ide_package_.com.example.buggame.R.drawable.img_capricorn
        "Водолей" -> _root_ide_package_.com.example.buggame.R.drawable.img_aquarius
        "Рыбы" -> _root_ide_package_.com.example.buggame.R.drawable.img_pisces
        else -> _root_ide_package_.com.example.buggame.R.drawable.ic_launcher_foreground
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    BugGameTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Для превью нам нужно передать "фейковый" репозиторий,
            // но Koin не запущен. Для простоты оставим как есть,
            // превью может не работать без Koin.
            // RegistrationScreen(modifier = Modifier.fillMaxSize())
        }
    }
}