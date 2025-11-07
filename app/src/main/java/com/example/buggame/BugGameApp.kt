package com.example.buggame

import android.app.Application
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class BugGameApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val retrofitModule = module {
            single {
                OkHttpClient.Builder().build()
            }
            single {
                Retrofit.Builder()
                    .baseUrl("https://www.cbr.ru/")
                    .client(get())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
            }
            single { get<Retrofit>().create(com.example.buggame.data.CurrencyApi::class.java) }
            single { com.example.buggame.data.CurrencyRepository(get()) }
            viewModel { com.example.buggame.ui.GameViewModel(get()) }

            // Репозитории, если нужно
            single { playerRepository }
            single { scoreRepository }
        }

        // Запускаем Koin один раз
        startKoin {
            androidContext(this@BugGameApp)
            modules(retrofitModule)
        }
    }
}
