package com.example.buggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.example.buggame.data.DatabaseProvider
import com.example.buggame.data.PlayerRepository
import com.example.buggame.data.ScoreRepository
import com.example.buggame.ui.TabsScreen
import com.example.buggame.ui.theme.BugGameTheme
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

lateinit var playerRepository: PlayerRepository
lateinit var scoreRepository: ScoreRepository

class GameViewModel : ViewModel() {
    // Add any necessary logic here if needed
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DatabaseProvider.getDatabase(this)
        playerRepository = PlayerRepository(db.playerDao())
        scoreRepository = ScoreRepository(db.scoreDao())

        startKoin {
            androidContext(this@MainActivity)
            modules(
                module {
                    single { playerRepository }
                    single { scoreRepository }
                    viewModel { GameViewModel() }
                }
            )
        }

        setContent {
            BugGameTheme {
                Scaffold { innerPadding ->
                    Surface(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    ) {
                        TabsScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}