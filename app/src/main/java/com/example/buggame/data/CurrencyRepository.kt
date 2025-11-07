package com.example.buggame.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern
import kotlin.text.RegexOption
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class CurrencyRepository(private val api: CurrencyApi) {

    companion object {
        private const val TAG = "CurrencyRepository"
    }
    private fun fetchMetalsDirectly(): String? {
        try {
            val target = "https://www.cbr.ru/scripts/XML_metall.asp"
            var url = URL(target)
            var conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) BugGame/1.0")
                setRequestProperty("Accept", "text/xml, application/xml, */*;q=0.1")
                requestMethod = "GET"
            }

            var responseCode = conn.responseCode
            Log.d(TAG, "direct fetch: url=$url responseCode=$responseCode")
            if (responseCode in 200..299) {
                return conn.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            }

            if (responseCode in 300..399) {
                val location = conn.getHeaderField("Location")
                Log.w(TAG, "redirect to: $location")
                if (!location.isNullOrBlank()) {
                    var newLocation = location
                    if (newLocation.startsWith("http://")) {
                        newLocation = newLocation.replaceFirst("http://", "https://")
                        Log.d(TAG, "converted redirect to https: $newLocation")
                    }
                    url = URL(newLocation)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) BugGame/1.0")
                        setRequestProperty("Accept", "text/xml, application/xml, */*;q=0.1")
                    }
                    responseCode = conn.responseCode
                    Log.d(TAG, "retry fetch: url=$url responseCode=$responseCode")
                    if (responseCode in 200..299) {
                        return conn.inputStream.readBytes().toString(StandardCharsets.UTF_8)
                    } else {
                        Log.w(TAG, "retry failed code=$responseCode")
                    }
                }
            } else {
                val errBytes = try { conn.errorStream?.readBytes() } catch (_: Exception) { null }
                val snippet = errBytes?.let { String(it).take(300) } ?: "no body"
                Log.w(TAG, "direct fetch failed: code=$responseCode snippet=$snippet")
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchMetalsDirectly exception: ${e.message}")
        }
        return null
    }

    suspend fun fetchGoldRateRUB(): Double = withContext(Dispatchers.IO) {
        try {
            // 1) Попробуем dailyXml
            val dailyXml = try { api.getDailyXml() } catch (e: Exception) {
                Log.w(TAG, "getDailyXml failed: ${e.message}")
                ""
            }
            if (dailyXml.isNotBlank()) {
                Log.d(TAG, "dailyXml length=${dailyXml.length}")
                Log.d(TAG, "dailyXml snippet: ${dailyXml.take(800).replace("\n"," ")}")
                parseGoldFromDailyXml(dailyXml)?.let {
                    Log.d(TAG, "parseDaily -> $it")
                    return@withContext it
                }
            }


            val metalsXml = try { api.getMetalsXml() } catch (e: Exception) {
                Log.w(TAG, "getMetalsXml failed: ${e.message}")
                ""
            }
            if (metalsXml.isNotBlank()) {
                Log.d(TAG, "metalsXml length=${metalsXml.length}")
                Log.d(TAG, "metalsXml snippet: ${metalsXml.take(800).replace("\n"," ")}")
                parseGoldFromMetalsXml(metalsXml)?.let {
                    Log.d(TAG, "parseMetals -> $it")
                    return@withContext it
                }
            }


            val xmlDirect = fetchMetalsDirectly()
            if (!xmlDirect.isNullOrBlank()) {
                Log.d(TAG, "direct metalsXml length=${xmlDirect.length}")
                Log.d(TAG, "direct metalsXml snippet: ${xmlDirect.take(800).replace("\n"," ")}")
                parseGoldFromMetalsXml(xmlDirect)?.let {
                    Log.d(TAG, "parseMetals (direct) -> $it")
                    return@withContext it
                }
            }


            val external = try { fetchGoldByExternalAndConvertToRUB(dailyXml) } catch (e: Exception) { null }
            if (external != null && external > 0.0) {
                Log.d(TAG, "external fallback -> $external")
                return@withContext external
            }


            val combined = (dailyXml + "\n" + metalsXml + "\n" + (xmlDirect ?: "")).ifBlank { "" }
            val fallback = parseByNameRegex(combined)
            Log.d(TAG, "regex fallback parse -> $fallback")
            if (fallback != null && fallback >= 0.01) return@withContext fallback

            return@withContext 0.0
        } catch (e: Exception) {
            Log.w(TAG, "fetchGoldRateRUB failed: ${e.message}")
            return@withContext 0.0
        }
    }
    private fun parseGoldFromDailyXml(xml: String): Double? {
        if (xml.isBlank()) return null
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var event = parser.eventType
            var insideValute = false
            var currentCharCode: String? = null
            var currentValue: String? = null
            var currentNominal: String? = null
            var currentName: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        when (name.lowercase()) {
                            "valute" -> {
                                insideValute = true
                                currentCharCode = null
                                currentValue = null
                                currentNominal = null
                                currentName = null
                            }
                            "charcode" -> if (insideValute) currentCharCode = parser.nextText()
                            "value" -> if (insideValute) currentValue = parser.nextText()
                            "nominal" -> if (insideValute) currentNominal = parser.nextText()
                            "name" -> if (insideValute) currentName = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name?.lowercase() == "valute") {
                            val cc = currentCharCode?.lowercase()
                            val nm = currentName?.lowercase() ?: ""
                            if (cc == "xau" || nm.contains("золото") || nm.contains("gold")) {
                                val v = (currentValue ?: "").replace(',', '.')
                                val nominal = (currentNominal ?: "1").replace(',', '.').toDoubleOrNull() ?: 1.0
                                val valueDouble = v.toDoubleOrNull()
                                if (valueDouble != null) {
                                    return valueDouble / nominal
                                }
                            }
                            insideValute = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseDaily failed: ${e.message}")
        }
        return null
    }
    private fun parseGoldFromMetalsXml(xml: String): Double? {
        if (xml.isBlank()) return null
        try {
            val nameRegex = Regex("(?s)<Name[^>]*>\\s*([^<]+?)\\s*</Name>.*?<Value[^>]*>\\s*([^<]+?)\\s*</Value>", RegexOption.IGNORE_CASE)
            val match = nameRegex.find(xml)
            if (match != null) {
                val nameText = match.groupValues[1].lowercase()
                val valueText = match.groupValues[2].replace(',', '.').trim()
                if (nameText.contains("золото") || nameText.contains("gold")) {
                    return valueText.toDoubleOrNull()
                }
            }
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var insideRecord = false
            var currentName: String? = null
            var currentValue: String? = null
            var currentNominal: String? = null
            var currentPrice: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        when (name.lowercase()) {
                            "record", "metal" -> {
                                insideRecord = true
                                currentName = null; currentValue = null; currentNominal = null; currentPrice = null
                            }
                            "name" -> if (insideRecord) currentName = parser.nextText()
                            "value" -> if (insideRecord) currentValue = parser.nextText()
                            "nominal" -> if (insideRecord) currentNominal = parser.nextText()
                            "price" -> if (insideRecord) currentPrice = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endName = parser.name?.lowercase()
                        if ((endName == "record" || endName == "metal") && insideRecord) {
                            val nm = currentName?.lowercase() ?: ""
                            if (nm.contains("золото") || nm.contains("gold")) {
                                val vStr = (currentValue ?: currentPrice ?: "").replace(',', '.').trim()
                                val nominal = (currentNominal ?: "1").replace(',', '.').toDoubleOrNull() ?: 1.0
                                val v = vStr.toDoubleOrNull()
                                if (v != null) return v / nominal
                            }
                            insideRecord = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseMetals failed: ${e.message}")
        }
        return null
    }
    private fun parseByNameRegex(xml: String): Double? {
        if (xml.isBlank()) return null
        try {
            val lower = xml.lowercase()
            val idx = lower.indexOf("золото").takeIf { it >= 0 } ?: lower.indexOf("gold").takeIf { it >= 0 } ?: -1
            if (idx >= 0) {
                val snippet = xml.substring(idx, kotlin.math.min(xml.length, idx + 1000))
                val numRegex = Regex("""(\d{1,3}(?:[ ,]\d{3})*(?:[.,]\d+)|\d+[.,]\d+)""")
                val m = numRegex.find(snippet)
                if (m != null) {
                    val raw = m.groupValues[1].replace(" ", "").replace(",", ".")
                    val d = raw.toDoubleOrNull()
                    if (d != null && d >= 10.0) return d
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseByNameRegex failed: ${e.message}")
        }
        return null
    }

    private fun fetchGoldByExternalAndConvertToRUB(dailyXml: String): Double? {
        try {
            var usdToRub: Double? = null
            if (dailyXml.isNotBlank()) {
                val lower = dailyXml.lowercase()
                val idx = lower.indexOf("<charcode>usd</charcode>")
                if (idx >= 0) {
                    val valueIdx = lower.indexOf("<value", idx)
                    if (valueIdx >= 0) {
                        val start = lower.indexOf('>', valueIdx) + 1
                        val end = lower.indexOf('<', start)
                        if (start > 0 && end > start) {
                            val raw = dailyXml.substring(start, end).trim().replace(',', '.')
                            usdToRub = raw.toDoubleOrNull()
                        }
                    }
                }
            }

            val apiUrl = "https://data-asg.goldprice.org/dbXRates/USD"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) BugGame/1.0")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.readBytes().toString(StandardCharsets.UTF_8)
                try {
                    val root = JSONObject(text)
                    var goldUsdPerOunce: Double? = null

                    if (root.has("items")) {
                        val items = root.getJSONArray("items")
                        if (items.length() > 0) {
                            val it0 = items.getJSONObject(0)
                            if (it0.has("xauPrice")) {
                                goldUsdPerOunce = it0.getDouble("xauPrice")
                            } else if (it0.has("xauPriceRaw")) {
                                goldUsdPerOunce = it0.getDouble("xauPriceRaw")
                            } else if (it0.has("price")) {
                                goldUsdPerOunce = it0.getDouble("price")
                            }
                        }
                    }
                    if (goldUsdPerOunce == null) {
                        if (root.has("xauPrice")) goldUsdPerOunce = root.getDouble("xauPrice")
                        else if (root.has("price")) goldUsdPerOunce = root.getDouble("price")
                    }

                    if (goldUsdPerOunce != null) {
                        val ozToGram = 31.1034768
                        val goldUsdPerGram = goldUsdPerOunce / ozToGram

                        if (usdToRub != null) {
                            val goldRubPerGram = goldUsdPerGram * usdToRub
                            Log.d("CurrencyRepository", "external gold (USD/oz)=$goldUsdPerOunce => RUB/g=$goldRubPerGram using USD->RUB=$usdToRub")
                            return goldRubPerGram
                        } else {
                            Log.w("CurrencyRepository", "USD->RUB not found in dailyXml; cannot convert external gold USD -> RUB")
                            return null
                        }
                    }
                } catch (je: Exception) {
                    Log.w("CurrencyRepository", "external parse json failed: ${je.message}")
                }
            } else {
                val err = conn.errorStream?.readBytes()?.toString(StandardCharsets.UTF_8)
                Log.w("CurrencyRepository", "external API responded code=$code snippet=${err?.take(200)}")
            }
        } catch (e: Exception) {
            Log.w("CurrencyRepository", "fetchGoldByExternalAndConvertToRUB failed: ${e.message}")
        }
        return null
    }

}
