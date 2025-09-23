package com.example.buggame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationScreen() {
    // A1: Состояния для ФИО
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }

    // A2: Состояния для выбора пола
    var selectedGender by remember { mutableStateOf("") }
    val genderOptions = listOf("Мужской", "Женский")

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

        // Радиокнопки для выбора пола
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

        // --- Кнопка для проверки ---
        Button(
            onClick = {
                // A1: Проверяем ФИО
                isNameError = name.isBlank()

                // Если ошибок нет, выводим данные
                if (!isNameError && selectedGender.isNotEmpty()) {
                    println("Данные: ФИО=$name, Пол=$selectedGender")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить данные")
        }

        // Показываем выбранные данные
        if (name.isNotBlank() && selectedGender.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Вы ввели:\nФИО: $name\nПол: $selectedGender",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
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