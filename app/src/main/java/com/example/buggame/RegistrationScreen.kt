package com.example.buggame

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.*

/**
 * Существующий Player (оставляем, чтобы не вмешиваться в код A).
 * Для финальной регистрации используем RegisteredPlayer ниже.
 */
data class Player(
    val fullName: String,
    val gender: String,
    val course: String
)

/**
 * Финальная структура регистрации (Student B).
 */
data class RegisteredPlayer(
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Calendar,
    val zodiac: String
)

@Composable
fun RegistrationScreen(modifier: Modifier = Modifier) {
    // A: базовые поля
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }

    // B1: difficulty slider state (1..10)
    var difficultyFloat by remember { mutableStateOf(1f) }
    val difficulty: Int = difficultyFloat.toInt()

    // B2: birth date state
    var birthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
// birthLabel сделаем derived (чтобы не обновлять вручную)
    val birthLabel by remember(birthCalendar) { derivedStateOf { formatDate(birthCalendar) } }
    val ctx = LocalContext.current

    val genderOptions = listOf("Мужской", "Женский")
    val courseOptions = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок ФИО
        Text(
            text = "ФИО",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Поле ФИО
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                isNameError = false
            },
            label = { Text("Фамилия Имя Отчество") },
            isError = isNameError,
            modifier = AppStyles.textFieldModifier,
            shape = MaterialTheme.shapes.medium
        )
        if (isNameError) {
            Text(
                text = "Поле не может быть пустым",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                modifier = AppStyles.textFieldModifier,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Выбрать курс")
                    }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                courseOptions.forEach { course ->
                    DropdownMenuItem(text = { Text(course) }, onClick = {
                        selectedCourse = course
                        expanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // B1: Slider (сложность)
        Text(text = "Уровень сложности: $difficulty", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = difficultyFloat,
            onValueChange = { difficultyFloat = it.coerceIn(1f, 10f) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // B2: Date picker
        Text(text = "Дата рождения", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedButton(
            onClick = {
                showDatePicker(ctx, birthCalendar) { year, month, day ->
                    // создаём новый Calendar и присваиваем в state — это вызовет recomposition
                    birthCalendar = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    // birthLabel обновится автоматически (derivedStateOf)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (birthLabel.isBlank()) "Выбрать дату" else "Дата: $birthLabel")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // B3: zodiac preview (text)
        val zodiacPreview = getZodiac(birthCalendar)
        Text(text = "Знак зодиака: $zodiacPreview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        // B4: zodiac preview (image) — placeholder by default
        val zodiacRes = getZodiacDrawableRes(zodiacPreview)
        Image(
            painter = painterResource(id = zodiacRes),
            contentDescription = "Знак зодиака",
            modifier = Modifier
                .size(96.dp)
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // FINAL: Register button — собираем RegisteredPlayer
        Button(
            onClick = {
                isNameError = name.isBlank()
                if (isNameError) return@Button

                if (selectedGender.isEmpty() || selectedCourse.isEmpty()) {
                    // краткая валидация
                    resultText = "Выберите пол и курс"
                    return@Button
                }

                val player = RegisteredPlayer(
                    fullName = name.trim(),
                    gender = selectedGender,
                    course = selectedCourse,
                    difficulty = difficulty,
                    birthDate = birthCalendar.clone() as Calendar,
                    zodiac = zodiacPreview
                )

                resultText = """
                    Регистрация завершена!
                    ФИО: ${player.fullName}
                    Пол: ${player.gender}
                    Курс: ${player.course}
                    Сложность: ${player.difficulty}
                    Дата рождения: ${formatDate(player.birthDate)}
                    Знак зодиака: ${player.zodiac}
                """.trimIndent()
            },
            modifier = AppStyles.buttonModifier,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Зарегистрироваться")
        }

        // Результат
        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
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

// Zodiac -> drawable mapping (placeholder)
fun getZodiacDrawableRes(zodiac: String): Int {
    return when (zodiac) {
        "Овен" -> R.drawable.img_aries
        "Телец" -> R.drawable.img_taurus
        "Близнецы" -> R.drawable.img_gemini
        "Рак" -> R.drawable.img_cancer
        "Лев" -> R.drawable.img_leo
        "Дева" -> R.drawable.img_virgo
        "Весы" -> R.drawable.img_libra
        "Скорпион" -> R.drawable.img_scorpio
        "Стрелец" -> R.drawable.img_sagittarius
        "Козерог" -> R.drawable.img_capricorn
        "Водолей" -> R.drawable.img_aquatius
        "Рыбы" -> R.drawable.img_pisces
        else -> R.drawable.ic_launcher_foreground
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
            RegistrationScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
