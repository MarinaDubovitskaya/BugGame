package com.example.buggame.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.buggame.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GoldRateWidgetProvider : AppWidgetProvider() {
    private val TAG = "GoldRateWidgetProv"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    private fun makeRetrofit(): Retrofit {
        val logger = HttpLoggingInterceptor().apply {
            // для разработки можно поставить BODY, на релизе — NONE или BASIC
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://www.cbr.ru/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, GoldRateWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(thisWidget)

        // 1. Создаем Retrofit (как у вас и было)
        val retrofit = makeRetrofit()
        val api = retrofit.create(com.example.buggame.data.CurrencyApi::class.java)

        // 2. СОЗДАЕМ РЕПОЗИТОРИЙ (вместо ручного парсинга)
        val repository = com.example.buggame.data.CurrencyRepository(api)

        CoroutineScope(Dispatchers.IO).launch {
            var rateText = "—" // Значение по умолчанию
            try {
                // 3. ВЫЗЫВАЕМ НАДЕЖНЫЙ МЕТОД ИЗ РЕПОЗИТОРИЯ
                val goldRate = repository.fetchGoldRateRUB()

                Log.d(TAG, "Fetched gold rate from repository: $goldRate")

                // 4. Проверяем, что курс получен
                if (goldRate > 0.0) {
                    rateText = try {
                        String.format("%.2f", goldRate)
                    } catch (ex: Throwable) {
                        Log.w(TAG, "format value failed: ${ex.message}")
                        goldRate.toString() // Фолбэк, если форматирование не удалось
                    }
                } else {
                    Log.w(TAG, "Repository returned zero or negative rate.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "updateAllWidgets failed: ${e.message}", e)
            }

            // 5. Обновляем UI виджетов (этот код у вас уже есть и он верный)
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)
                views.setTextViewText(R.id.widget_gold_value, if (rateText == "—") "—" else "$rateText ₽")

                val intent = Intent(context, com.example.buggame.MainActivity::class.java)
                val pi = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
