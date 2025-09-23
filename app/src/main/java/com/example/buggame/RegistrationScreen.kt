package com.example.buggame

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.*

data class Player(
    val fullName: String,
    val gender: String,
    val course: String
)

@Composable
fun RegistrationScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }

    // B2: дата рождения — состояние
    val birthCalendar = remember { Calendar.getInstance() }
    var birthLabel by remember { mutableStateOf(formatDate(birthCalendar)) }
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

        // Поле ФИО с стилями
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

        AppStyles.sectionSpacing

        // Заголовок Пол
        Text(
            text = "Пол",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Радиокнопки с отступами
        Column {
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
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        AppStyles.sectionSpacing

        // Заголовок Курс
        Text(
            text = "Курс",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Выпадающий список
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
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Выбрать курс"
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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

        AppStyles.sectionSpacing

        // --- Новая часть: выбор даты рождения ---
        Text(
            text = "Дата рождения",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = {
                showDatePicker(ctx, birthCalendar) { year, month, day ->
                    birthCalendar.set(year, month, day)
                    birthLabel = formatDate(birthCalendar)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (birthLabel.isBlank()) "Выбрать дату" else "Дата: $birthLabel")
        }

        AppStyles.sectionSpacing

        // --- v4: превью знака зодиака (текст) ---
        val zodiacPreview = getZodiac(birthCalendar)
        Text(
            text = "Знак зодиака: $zodiacPreview",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Кнопка с стилями
        Button(
            onClick = {
                isNameError = name.isBlank()

                if (!isNameError && selectedGender.isNotEmpty() && selectedCourse.isNotEmpty()) {
                    val player = Player(name, selectedGender, selectedCourse)
                    resultText = """
                        Регистрация завершена!
                        ФИО: ${player.fullName}
                        Пол: ${player.gender}
                        Курс: ${player.course}
                        Дата рождения: $birthLabel
                        Знак зодиака: $zodiacPreview
                    """.trimIndent()
                }
            },
            modifier = AppStyles.buttonModifier,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Зарегистрироваться")
        }

        // Результат
        if (resultText.isNotEmpty()) {
            AppStyles.smallSpacing
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

// ---- v4: zodiac functions ----
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
