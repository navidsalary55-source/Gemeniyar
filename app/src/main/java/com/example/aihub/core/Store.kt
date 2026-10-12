package com.example.aihub.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Config {
    const val APP_VERSION = "1.6"
    const val APP_NAME = "هوش مصنوعی هوشیار"
    const val OWNER = "نوید سالاری"
    const val CONTACT_EMAIL = "navidsalary55@gmail.com"
    const val ID_TELEGRAM = "@na112233vi"
    const val ID_RUBIKA = "@na112233vi"
    const val ID_WHATSAPP = "@na11223344vad"
    const val URL_TELEGRAM = "https://t.me/na112233vi"
    const val URL_RUBIKA = "https://rubika.ir/na112233vi"
}

/** سرویس‌دهنده‌های هوش مصنوعی (همه با فرمت سازگار با OpenAI) */
enum class Provider(
    val label: String,
    val base: String,
    val defaultModel: String,
    val visionModel: String?,
    val keyUrl: String?,
    val needsKey: Boolean,
    val note: String,
) {
    GROQ(
        "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile",
        "meta-llama/llama-4-scout-17b-16e-instruct", "https://console.groq.com/keys", true,
        "خیلی سریع و رایگان؛ حدود ۱۰۰۰ درخواست در روز؛ از عکس هم پشتیبانی می‌کند. (پیشنهاد اصلی)",
    ),
    CEREBRAS(
        "Cerebras", "https://api.cerebras.ai/v1", "gpt-oss-120b", null, "https://cloud.cerebras.ai", true,
        "سهمیه‌ی روزانه‌ی زیاد (حدود ۱ میلیون توکن)؛ فقط متن. برای ساخت سایت و متن‌های بلند عالی است.",
    ),
    OPENROUTER(
        "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-oss-20b:free", null, "https://openrouter.ai/keys", true,
        "مدل‌های رایگان متنوع؛ حدود ۵۰ درخواست در روز.",
    ),
    MISTRAL(
        "Mistral", "https://api.mistral.ai/v1", "mistral-small-latest", null, "https://console.mistral.ai/api-keys", true,
        "پلن رایگان Experiment؛ فارسی خوب و پشتیبانی از عکس.",
    ),
    GEMINI(
        "Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash", null,
        "https://aistudio.google.com/apikey", true,
        "گوگل؛ بدون VPN ممکن است در بعضی مناطق کار نکند.",
    ),
    POLLINATIONS(
        "Pollinations", "https://text.pollinations.ai/openai", "openai", null, null, false,
        "بدون کلید و بدون ثبت‌نام؛ کندتر و محدود به سرعت. برای تست سریع خوب است.",
    );

    /** آیا این سرویس عکس را می‌فهمد */
    val supportsVision: Boolean get() = visionModel != null || this == MISTRAL || this == GEMINI
}

object Store {
    private lateinit var p: SharedPreferences

    var provider by mutableStateOf(Provider.GROQ)
        private set
    var apiKey by mutableStateOf("")
        private set
    var model by mutableStateOf(Provider.GROQ.defaultModel)
        private set
    /** 0 = سیستم، 1 = روشن، 2 = تاریک */
    var themeMode by mutableStateOf(0)
        private set

    val needsKey: Boolean get() = provider.needsKey && apiKey.isBlank()

    /** کلید اختیاری Pollinations (از enter.pollinations.ai) برای تصویر و صدای پایدارتر */
    var pollKey by mutableStateOf("")
        private set
    /** اگر سرویس انتخابی خطا داد، خودکار سرویس‌های دیگرِ دارای کلید را امتحان کن */
    var autoFallback by mutableStateOf(true)
        private set
    /** نام صدای ربات در چت صوتی (alloy, nova, shimmer, echo, onyx, fable) */
    var botVoice by mutableStateOf("nova")
        private set
    /** ربات با صدای آنلاین (طبیعی‌تر) جواب بدهد؟ در غیر این صورت صدای خود گوشی */
    var voiceOnline by mutableStateOf(true)
        private set
    /** پاسخ ربات در چت متنی هم بلند خوانده شود؟ */
    var autoSpeak by mutableStateOf(false)
        private set

    fun guideSeen(id: String): Boolean = p.getBoolean("guide_$id", false)
    fun markGuide(id: String) { p.edit().putBoolean("guide_$id", true).apply() }

    fun savePoll(key: String) {
        pollKey = key.trim()
        p.edit().putString("poll_key", pollKey).apply()
    }
    fun saveFlags(fallback: Boolean, online: Boolean, voice: String) {
        autoFallback = fallback; voiceOnline = online; botVoice = voice
        p.edit().putBoolean("fallback", fallback).putBoolean("v_online", online).putString("bot_voice", voice).apply()
    }
    fun saveAutoSpeak(v: Boolean) {
        autoSpeak = v
        p.edit().putBoolean("auto_speak", v).apply()
    }

    fun keyFor(pr: Provider): String = p.getString("key_${pr.name}", "") ?: ""
    fun modelFor(pr: Provider): String = p.getString("model_${pr.name}", pr.defaultModel) ?: pr.defaultModel

    fun init(ctx: Context) {
        p = ctx.applicationContext.getSharedPreferences("aihub", Context.MODE_PRIVATE)
        provider = try {
            Provider.valueOf(p.getString("provider", Provider.GROQ.name) ?: Provider.GROQ.name)
        } catch (e: Exception) {
            Provider.GROQ
        }
        apiKey = keyFor(provider)
        model = modelFor(provider)
        themeMode = p.getInt("theme", 0)
        pollKey = p.getString("poll_key", "") ?: ""
        autoFallback = p.getBoolean("fallback", true)
        voiceOnline = p.getBoolean("v_online", true)
        botVoice = p.getString("bot_voice", "nova") ?: "nova"
        autoSpeak = p.getBoolean("auto_speak", false)
    }

    fun save(pr: Provider, key: String, modelName: String) {
        val m = modelName.ifBlank { pr.defaultModel }
        p.edit().putString("provider", pr.name)
            .putString("key_${pr.name}", key)
            .putString("model_${pr.name}", m).apply()
        provider = pr
        apiKey = key
        model = m
    }

    fun saveTheme(mode: Int) {
        themeMode = mode
        p.edit().putInt("theme", mode).apply()
    }
}
