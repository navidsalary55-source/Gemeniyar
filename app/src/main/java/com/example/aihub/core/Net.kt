package com.example.aihub.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object Net {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** دانلود بایت‌ها؛ در صورت خطای موقت (محدودیت نرخ) چند بار دوباره تلاش می‌کند. */
    suspend fun getBytes(url: String, tries: Int = 4, waitMs: Long = 7000): ByteArray =
        withContext(Dispatchers.IO) {
            var last: Exception = IOException("خطای ناشناخته در دانلود")
            for (i in 0 until tries) {
                try {
                    http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                        if (r.isSuccessful) {
                            val b = r.body?.bytes()
                            if (b != null && b.isNotEmpty()) return@withContext b
                            last = IOException("پاسخ خالی")
                        } else {
                            last = IOException("HTTP ${r.code}")
                        }
                    }
                } catch (e: IOException) {
                    last = e
                }
                if (i < tries - 1) delay(waitMs)
            }
            throw AiException(
                "دریافت از سرویس رایگان ممکن نشد (${last.message}). ممکن است شلوغ باشد؛ چند لحظه بعد دوباره امتحان کن."
            )
        }

    /** دانلود از چند نشانی به ترتیب؛ فقط پاسخی که نوع محتوایش با prefix شروع شود (مثلاً image/ یا audio/) قبول است. */
    suspend fun fetchTyped(urls: List<String>, prefix: String, tries: Int = 2, waitMs: Long = 5000): ByteArray =
        withContext(Dispatchers.IO) {
            var last = "پاسخ نامعتبر"
            for (url in urls) {
                for (i in 0 until tries) {
                    try {
                        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                            val type = r.header("Content-Type").orEmpty()
                            if (r.isSuccessful && type.startsWith(prefix)) {
                                val b = r.body?.bytes()
                                if (b != null && b.size > 200) return@withContext b
                                last = "پاسخ خالی"
                            } else {
                                last = if (r.isSuccessful) "نوع پاسخ نادرست" else "HTTP ${r.code}"
                            }
                            Unit
                        }
                    } catch (e: IOException) {
                        last = e.message ?: "قطع اتصال"
                    }
                    if (last.startsWith("HTTP 4") && last != "HTTP 429") break
                    if (i < tries - 1) delay(waitMs)
                }
            }
            throw AiException(
                "سرویس رایگان جواب نداد ($last). اگر خطا تکرار شد، در «تنظیمات» کلید رایگان Pollinations را (طبق راهنما) وارد کن یا چند دقیقه بعد دوباره امتحان کن."
            )
        }
}

/** سرویس رایگان و بدون کلید Pollinations برای تصویر و صدا */
object Poll {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun imageUrls(prompt: String, w: Int, h: Int, seed: Int): List<String> {
        val q = enc(prompt.take(700))
        val out = ArrayList<String>()
        if (Store.pollKey.isNotBlank()) {
            out.add("https://gen.pollinations.ai/image/$q?width=$w&height=$h&seed=$seed&model=flux&nologo=true&key=${Store.pollKey}")
        }
        out.add("https://image.pollinations.ai/prompt/$q?width=$w&height=$h&seed=$seed&nologo=true&model=flux")
        return out
    }

    /** ساخت تصویر؛ با کلید (در صورت وجود) و بعد مسیر بدون کلید */
    suspend fun image(prompt: String, w: Int, h: Int, seed: Int = Random.nextInt(1_000_000)): ByteArray =
        Net.fetchTyped(imageUrls(prompt, w, h, seed), "image/", tries = 3, waitMs = 7000)

    /** صدای آنلاین (mp3) با صدای آماده‌ی هوش مصنوعی */
    suspend fun speech(text: String, voice: String): ByteArray {
        val q = enc(text.take(600))
        val out = ArrayList<String>()
        if (Store.pollKey.isNotBlank()) {
            out.add("https://gen.pollinations.ai/audio/$q?voice=$voice&model=openai-audio&key=${Store.pollKey}")
        }
        out.add("https://text.pollinations.ai/$q?model=openai-audio&voice=$voice")
        return Net.fetchTyped(out, "audio/", tries = 2, waitMs = 4000)
    }

    /** تبدیل توضیح فارسی به پرامپت انگلیسی دقیق (اگر کلید جمینای باشد) */
    suspend fun betterPrompt(userText: String, style: String): String {
        return try {
            Ai.generate(
                "User idea: $userText\nStyle: $style",
                system = "You write prompts for an AI image generator. Convert the user's idea (any language) into ONE detailed " +
                    "English prompt (max 60 words) including subject, composition, lighting and the requested style. " +
                    "Output only the prompt, nothing else.",
                temperature = 0.7,
            ).trim()
        } catch (e: Exception) {
            "$userText, $style style"
        }
    }
}
