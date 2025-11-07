package com.example.buggame.data

import retrofit2.http.GET

interface CurrencyApi {
    @GET("scripts/XML_daily.asp")
    suspend fun getDailyXml(): String
    @GET("scripts/xml_metall.asp")
    suspend fun getMetalsXml(): String
}
