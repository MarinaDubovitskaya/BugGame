package com.example.buggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.buggame.ui.theme.BugGameTheme
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BugGameTheme {
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("Регистрация игрока") }) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Передаём padding и fillMaxSize в RegistrationScreen - предотвращает наложение под topBar
                    RegistrationScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    BugGameTheme {
        Surface {
            RegistrationScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
