package com.example.buggame

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun RegistrationScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }

    // B1: difficulty slider state (1..10)
    var difficultyFloat by remember { mutableStateOf(1f) }
    // CORRECTED: обычная локальная переменная, обновляется при recomposition
    val difficulty: Int = difficultyFloat.toInt()

    val genderOptions = listOf("Мужской", "Женский")
    val courseOptions = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")

    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        // ФИО
        Text(text = "ФИО", style = MaterialTheme.typography.titleMedium)
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
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        )
        if (isNameError) {
            Text(
                "Поле не может быть пустым",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Пол
        Text(text = "Пол", style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Курс (Dropdown)
        Text(text = "Курс", style = MaterialTheme.typography.titleMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCourse,
                onValueChange = {},
                readOnly = true,
                label = { Text("Выберите курс") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Выбрать курс")
                    }
                },
                shape = MaterialTheme.shapes.medium
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

        Spacer(modifier = Modifier.height(12.dp))

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

        // Кнопка (частичная, пока собирает только основные поля)
        Button(
            onClick = {
                isNameError = name.isBlank()
                if (!isNameError) {
                    resultText = "ФИО: $name\nПол: ${if (selectedGender.isNotEmpty()) selectedGender else "Не указан"}\nКурс: ${if (selectedCourse.isNotEmpty()) selectedCourse else "Не выбран"}\nСложность: $difficulty"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Показать (частично)")
        }

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = resultText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview_v2() {
    RegistrationScreen(modifier = Modifier.fillMaxSize())
}
