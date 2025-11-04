package com.example.buggame.data

import retrofit2.http.GET

interface CurrencyApi {
    // Получаем «сирой» XML как строку — затем парсим
    @GET("scripts/XML_daily.asp")
    suspend fun getDailyXml(): String

    // Запрос для металлов (на случай, если золото в отдельном эндпоинте)
    @GET("scripts/xml_metall.asp")
    suspend fun getMetalsXml(): String
}
