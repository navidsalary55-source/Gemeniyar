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
}

/** سرویس رایگان و بدون کلید Pollinations برای تصویر و صدا */
object Poll {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun imageUrl(prompt: String, w: Int, h: Int, seed: Int = Random.nextInt(1_000_000)): String =
        "https://image.pollinations.ai/prompt/${enc(prompt.take(700))}?width=$w&height=$h&seed=$seed&nologo=true&model=flux"

    fun speechUrl(text: String, voice: String): String =
        "https://text.pollinations.ai/${enc(text.take(500))}?model=openai-audio&voice=$voice"

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
