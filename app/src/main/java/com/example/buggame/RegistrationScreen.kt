package com.example.buggame

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// A4: Data class для игрока
data class Player(
    val fullName: String,
    val gender: String,
    val course: String
)

@Composable
fun RegistrationScreen() {
    // A1: Состояния для ФИО
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }

    // A2: Состояния для выбора пола
    var selectedGender by remember { mutableStateOf("") }
    val genderOptions = listOf("Мужской", "Женский")

    // A3: Состояния для выбора курса
    var selectedCourse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val courseOptions = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")

    // A4: Состояние для отображения результата
    var resultText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- A1: Поле ввода ФИО ---
        Text(
            text = "ФИО",
            style = MaterialTheme.typography.bodyLarge,
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
            modifier = Modifier.fillMaxWidth()
        )
        if (isNameError) {
            Text(
                text = "Поле не может быть пустым",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- A2: Выбор пола ---
        Text(
            text = "Пол",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                        onClick = { selectedGender = gender }
                    )
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- A3: Выбор курса ---
        Text(
            text = "Курс",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCourse,
                onValueChange = {},
                readOnly = true,
                label = { Text("Выберите курс") },
                modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(24.dp))

        // --- A4: Кнопка регистрации и вывод результата ---
        Button(
            onClick = {
                // Проверяем все поля
                isNameError = name.isBlank()

                if (!isNameError && selectedGender.isNotEmpty() && selectedCourse.isNotEmpty()) {
                    // Создаем объект Player
                    val player = Player(
                        fullName = name,
                        gender = selectedGender,
                        course = selectedCourse
                    )

                    // Выводим результат
                    resultText = """
                        Регистрация завершена!
                        ФИО: ${player.fullName}
                        Пол: ${player.gender}
                        Курс: ${player.course}
                    """.trimIndent()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зарегистрироваться")
        }

        // A4: Поле для вывода результата
        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Green,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    MaterialTheme {
        RegistrationScreen()
    }
}