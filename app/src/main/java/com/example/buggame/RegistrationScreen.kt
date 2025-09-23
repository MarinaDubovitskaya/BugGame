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

data class Player(
    val fullName: String,
    val gender: String,
    val course: String
)

@Composable
fun RegistrationScreen() {
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }

    val genderOptions = listOf("Мужской", "Женский")
    val courseOptions = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")

    Column(
        modifier = Modifier
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

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    BugGameTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RegistrationScreen()
        }
    }
}