package com.example.buggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.buggame.data.CurrencyApi
import com.example.buggame.data.CurrencyRepository
import com.example.buggame.data.DatabaseProvider
import com.example.buggame.data.PlayerRepository
import com.example.buggame.data.ScoreRepository
import com.example.buggame.ui.GameViewModel
import com.example.buggame.ui.TabsScreen
import com.example.buggame.ui.theme.BugGameTheme
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module


lateinit var playerRepository: PlayerRepository
lateinit var scoreRepository: ScoreRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация Room БД и репозиториев (как было)
        val db = DatabaseProvider.getDatabase(this)
        playerRepository = PlayerRepository(db.playerDao())
        scoreRepository = ScoreRepository(db.scoreDao())

        val retrofitModule = module {
            single {
                OkHttpClient.Builder()
                    // при необходимости добавить таймауты/логгер
                    .build()
            }
            single {
                Retrofit.Builder()
                    .baseUrl("https://www.cbr.ru/") // обязательно https
                    .client(get())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
            }
            single { get<Retrofit>().create(com.example.buggame.data.CurrencyApi::class.java) }
            single { com.example.buggame.data.CurrencyRepository(get()) }
            viewModel { com.example.buggame.ui.GameViewModel(get()) }
            // сохраняем playerRepository и scoreRepository если нужны
            single { playerRepository }
            single { scoreRepository }
        }

        startKoin {
            androidContext(this@MainActivity)
            modules(retrofitModule)
        }

        setContent {
            BugGameTheme {
                Scaffold { innerPadding ->
                    Surface(
                        modifier = Modifier
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
