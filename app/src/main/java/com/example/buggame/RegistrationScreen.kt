package com.example.buggame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationScreen() {
    // Состояния: текст поля и флаг ошибки
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Вертикальная колонка
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Поле ввода ФИО
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                isError = false // убираем ошибку при изменении текста
            },
            label = { Text("ФИО") },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        // Сообщение об ошибке (красным под полем)
        if (isError) {
            Text(
                text = "Поле не может быть пустым",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка проверки
        Button(
            onClick = {
                if (name.isBlank()) {
                    isError = true
                } else {
                    // Здесь будет обработка успешного ввода
                    // например, переход на следующий экран или сохранение данных
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
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
