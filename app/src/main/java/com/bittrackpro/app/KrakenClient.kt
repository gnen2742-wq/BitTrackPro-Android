package com.bittrackpro.app

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KrakenClient {
    private const val BASE = "https://api.kraken.com"

    suspend fun ticker(pair: String): Double = withContext(Dispatchers.IO) {
        val url = URL("$BASE/0/public/Ticker?pair=${enc(pair)}")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val errors = json.getJSONArray("error")
        if (errors.length() > 0) error(errors.toString())
        val result = json.getJSONObject("result")
        val key = result.keys().next()
        result.getJSONObject(key).getJSONArray("c").getString(0).toDouble()
    }

    suspend fun balance(apiKey: String, apiSecret: String): JSONObject = privatePost(
        "/0/private/Balance", apiKey, apiSecret, emptyMap()
    )

    suspend fun openOrders(apiKey: String, apiSecret: String): JSONObject = privatePost(
        "/0/private/OpenOrders", apiKey, apiSecret, mapOf("trades" to "true")
    )

    suspend fun closedOrders(apiKey: String, apiSecret: String): JSONObject = privatePost(
        "/0/private/ClosedOrders", apiKey, apiSecret, mapOf("trades" to "true")
    )

    suspend fun cancelOrder(apiKey: String, apiSecret: String, txid: String): JSONObject = privatePost(
        "/0/private/CancelOrder", apiKey, apiSecret, mapOf("txid" to txid)
    )

    suspend fun addOrder(
        apiKey: String,
        apiSecret: String,
        side: String,
        orderType: String,
        volume: String,
        price: String? = null
    ): JSONObject {
        val params = linkedMapOf(
            "pair" to "XBTEUR",
            "type" to side,
            "ordertype" to orderType,
            "volume" to volume
        )
        if (!price.isNullOrBlank()) params["price"] = price
        return privatePost("/0/private/AddOrder", apiKey, apiSecret, params)
    }

    private suspend fun privatePost(
        path: String,
        apiKey: String,
        apiSecret: String,
        params: Map<String, String>
    ): JSONObject = withContext(Dispatchers.IO) {
        val nonce = System.currentTimeMillis().toString()
        val all = linkedMapOf("nonce" to nonce)
        all.putAll(params)
        val postData = all.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        val signature = sign(path, nonce, postData, apiSecret)

        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        conn.setRequestProperty("API-Key", apiKey)
        conn.setRequestProperty("API-Sign", signature)
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }

        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        val json = JSONObject(body)
        val errors = json.optJSONArray("error")
        if (errors != null && errors.length() > 0) error(errors.toString())
        json
    }

    private fun sign(path: String, nonce: String, postData: String, secret: String): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest((nonce + postData).toByteArray(Charsets.UTF_8))
        val message = path.toByteArray(Charsets.UTF_8) + hash
        val secretBytes = Base64.decode(secret, Base64.DEFAULT)
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA512"))
        return Base64.encodeToString(mac.doFinal(message), Base64.NO_WRAP)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
