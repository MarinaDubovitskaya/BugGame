package com.example.buggame

import android.content.res.Configuration
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация Room БД
        val db = DatabaseProvider.getDatabase(this)
        val playerRepo = PlayerRepository(db.playerDao())
        val scoreRepo = ScoreRepository(db.scoreDao())

        // Модуль Koin
        val appModule = module {
            // --- Network ---
            single {
                OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            }
            single {
                Retrofit.Builder()
                    .baseUrl("https://www.cbr.ru/") // обязательно https
                    .client(get())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
            }
            single<CurrencyApi> { get<Retrofit>().create(CurrencyApi::class.java) }
            single { CurrencyRepository(get()) }

            // --- Database & Repositories ---
            // Предоставляем ранее созданные экземпляры репозиториев
            single { playerRepo }
            single { scoreRepo }

            // --- ViewModels ---
            // GameViewModel теперь зависит от CurrencyRepository и ScoreRepository
            viewModel { GameViewModel(get(), get()) }
        }

        // Запуск Koin
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {
            BugGameTheme {
                Scaffold { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // TabsScreen теперь будет получать зависимости (ViewModel, Repositories)
                        // через Koin (org.koin.androidx.compose.getViewModel / get)
                        TabsScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Здесь можно добавить логику для обработки поворота, например, пауза игры
        // Но поскольку ViewModel сохраняет состояние, это может не быть нужно.
        // Если игра крашится, добавь try-catch или специфический код.
    }
}