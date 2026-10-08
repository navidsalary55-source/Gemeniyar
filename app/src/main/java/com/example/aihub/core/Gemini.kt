package com.example.aihub.core
// (نام فایل برای سازگاری با آپلود روی نسخه‌ی قبلی همان Gemini.kt مانده؛ داخلش کلاینت چندسرویسی Ai است)

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

class AiException(message: String) : Exception(message)

data class ChatMsg(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val isError: Boolean = false,
    val image: ByteArray? = null,
)

fun String.stripCodeFence(): String {
    var s = trim()
    if (s.startsWith("```")) {
        s = s.substringAfter('\n', s)
        s = s.substringBeforeLast("```")
    }
    return s.trim()
}

/** رشته‌ی کلید؛ اگر مقدار null یا نبود، رشته‌ی خالی (org.json برای null می‌نویسد "null"!) */
private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key)

/** کلاینت یکپارچه برای همه‌ی سرویس‌دهنده‌های سازگار با OpenAI (Groq, Cerebras, OpenRouter, Mistral, Gemini, Pollinations) */
object Ai {
    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json".toMediaType()
    private val THINK = Regex("(?s)<think>.*?</think>")

    private fun content(m: ChatMsg): Any {
        val img = m.image ?: return m.text
        val arr = JSONArray()
        arr.put(JSONObject().put("type", "text").put("text", m.text.ifEmpty { "Describe this image." }))
        arr.put(
            JSONObject().put("type", "image_url").put(
                "image_url",
                JSONObject().put("url", "data:image/jpeg;base64," + Base64.encodeToString(img, Base64.NO_WRAP))
            )
        )
        return arr
    }

    private fun body(
        history: List<ChatMsg>, system: String?, temperature: Double, json: Boolean, maxTokens: Int?, stream: Boolean,
    ): String {
        val pr = Store.provider
        val hasImage = history.any { it.image != null }
        val vm = pr.visionModel
        val model = if (hasImage && vm != null && Store.model == pr.defaultModel) vm else Store.model
        val msgs = JSONArray()
        var sys = system.orEmpty()
        if (json) sys += "\nReturn ONLY one valid JSON object. No markdown fences, no commentary."
        if (sys.isNotBlank()) msgs.put(JSONObject().put("role", "system").put("content", sys.trim()))
        history.forEach { m ->
            msgs.put(JSONObject().put("role", if (m.isUser) "user" else "assistant").put("content", content(m)))
        }
        val o = JSONObject().put("model", model).put("messages", msgs)
            .put("temperature", temperature).put("stream", stream)
        var mt = maxTokens
        if (pr == Provider.GROQ && mt != null) mt = min(mt, 6000)
        if (mt != null) o.put("max_tokens", mt)
        return o.toString()
    }

    private fun request(path: String, body: String?): Request {
        val pr = Store.provider
        if (pr.needsKey && Store.apiKey.isBlank()) {
            throw AiException("اول کلید API را در «تنظیمات» وارد کن (یا سرویس Pollinations را بدون کلید انتخاب کن).")
        }
        val b = Request.Builder().url(pr.base + path)
        if (Store.apiKey.isNotBlank()) b.addHeader("Authorization", "Bearer ${Store.apiKey}")
        if (body != null) b.post(body.toRequestBody(JSON_TYPE)) else b.get()
        return b.build()
    }

    /** پاسخ کامل (غیر Streaming) */
    suspend fun generateWith(
        history: List<ChatMsg>,
        system: String? = null,
        temperature: Double = 0.8,
        json: Boolean = false,
        maxTokens: Int? = null,
    ): String = withContext(Dispatchers.IO) {
        val req = request("/chat/completions", body(history, system, temperature, json, maxTokens, false))
        try {
            http.newCall(req).execute().use { r ->
                val txt = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw AiException(friendly(r.code, txt))
                val msg = JSONObject(txt).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                var out = (msg?.str("content") ?: "").replace(THINK, "").trim()
                if (json) {
                    val a = out.indexOf('{')
                    val z = out.lastIndexOf('}')
                    if (a != -1 && z > a) out = out.substring(a, z + 1)
                }
                if (out.isEmpty()) throw AiException("پاسخی دریافت نشد. دوباره امتحان کن.")
                out
            }
        } catch (e: IOException) {
            throw AiException("اتصال اینترنت برقرار نیست (اگر لازم است VPN را روشن کن).")
        }
    }

    suspend fun generate(
        prompt: String,
        system: String? = null,
        image: ByteArray? = null,
        temperature: Double = 0.8,
        json: Boolean = false,
        maxTokens: Int? = null,
    ): String = generateWith(listOf(ChatMsg(0, true, prompt, image = image)), system, temperature, json, maxTokens)

    /** پاسخ زنده (Streaming) */
    fun stream(history: List<ChatMsg>, system: String?, temperature: Double): Flow<String> = callbackFlow {
        val call = http.newCall(request("/chat/completions", body(history, system, temperature, false, null, true)))
        launch(Dispatchers.IO) {
            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) throw AiException(friendly(resp.code, resp.body?.string()))
                    val source = resp.body!!.source()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty()) continue
                        if (data == "[DONE]") break
                        val delta = try {
                            JSONObject(data).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                        } catch (_: Exception) {
                            null
                        }
                        val t = delta?.str("content").orEmpty()
                        if (t.isNotEmpty()) trySend(t)
                    }
                }
                close()
            } catch (e: IOException) {
                close(AiException("اتصال اینترنت برقرار نیست یا قطع شد."))
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose { call.cancel() }
    }

    /** فهرست مدل‌های در دسترس سرویس‌دهنده‌ی فعلی */
    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            http.newCall(request("/models", null)).execute().use { r ->
                val txt = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw AiException(friendly(r.code, txt))
                val arr = JSONObject(txt).optJSONArray("data") ?: JSONArray()
                val ids = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val id = arr.optJSONObject(i)?.str("id").orEmpty().removePrefix("models/")
                    if (id.isNotBlank()) ids.add(id)
                }
                ids.sorted()
            }
        } catch (e: IOException) {
            throw AiException("اتصال اینترنت برقرار نیست (اگر لازم است VPN را روشن کن).")
        }
    }

    private fun friendly(code: Int, body: String?): String {
        val apiMsg = try {
            val o = JSONObject(body ?: "")
            o.optJSONObject("error")?.str("message") ?: o.str("error")
        } catch (_: Exception) {
            ""
        }
        return when (code) {
            400 -> "درخواست نامعتبر بود. $apiMsg".trim()
            401 -> "کلید API معتبر نیست. در تنظیمات دوباره واردش کن."
            402, 403 -> "دسترسی رد شد. کلید یا محدودیت منطقه (VPN) را بررسی کن. $apiMsg".trim()
            404 -> "مدل پیدا نشد. در تنظیمات «فهرست مدل‌ها» را بزن و یکی را انتخاب کن."
            413 -> "درخواست برای این مدل خیلی بزرگ است. متن کوتاه‌تر یا مدل دیگری انتخاب کن."
            429 -> "سهمیه‌ی رایگان یا تعداد درخواست در دقیقه پر شده. کمی صبر کن یا در تنظیمات سرویس دیگری انتخاب کن."
            500, 502, 503 -> "سرور سرویس شلوغ است. چند لحظه بعد دوباره تلاش کن."
            else -> "خطا ($code) $apiMsg".trim()
        }
    }
}
