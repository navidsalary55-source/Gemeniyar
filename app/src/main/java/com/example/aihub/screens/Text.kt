package com.example.aihub.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.core.*
import com.example.aihub.ui.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- مترجم

@Composable
fun TranslateScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val speaker = remember { Speaker(ctx) }
    val stt = remember { SpeechInput(ctx) }
    DisposableEffect(Unit) { onDispose { stt.release(); speaker.shutdown() } }

    var from by remember { mutableStateOf(Langs[0]) }
    var to by remember { mutableStateOf(Langs[1]) }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var speakJob by remember { mutableStateOf<Job?>(null) }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) stt.start(from.tag, { input = it }, { toast(ctx, it) }) else toast(ctx, "اجازه‌ی میکروفون لازم است")
    }

    ToolScaffold("مترجم هوشمند", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("از")
            ChipRow(Langs, from, { it.label }) { from = it }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { val t = from; from = to; to = t; input = output.also { output = input } }) {
                    Icon(Icons.Rounded.SwapVert, "جابه‌جایی زبان‌ها", tint = MaterialTheme.colorScheme.primary)
                }
            }
            SectionTitle("به")
            ChipRow(Langs, to, { it.label }) { to = it }
            AppField(
                input, { input = it }, "متن مبدأ", Modifier.padding(horizontal = 16.dp), minLines = 4, maxLines = 10,
                trailing = {
                    IconButton(onClick = {
                        if (stt.listening) stt.finish()
                        else if (hasMic(ctx)) stt.start(from.tag, { input = it }, { toast(ctx, it) })
                        else perm.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Icon(if (stt.listening) Icons.Rounded.Stop else Icons.Rounded.Mic, "میکروفون") }
                },
            )
            GradButton(
                "ترجمه", icon = Icons.Rounded.Translate, loading = task.loading, enabled = input.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = {
                    task.launch(scope) {
                        output = Ai.generate(
                            input,
                            system = "You are a professional translator. Translate the user's text from ${from.name} to ${to.name}. " +
                                "Keep meaning, tone and formatting. Output ONLY the translation, with no notes.",
                            temperature = 0.2,
                        )
                    }
                },
            )
            ErrorText(task.error)
            if (output.isNotBlank()) {
                ResultCard(
                    output, Modifier.padding(horizontal = 16.dp),
                    onSpeak = {
                        speakJob?.cancel()
                        speakJob = scope.launch { speaker.speak(output, locale = to.locale) }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- متن به صدا

@Composable
fun TtsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    var text by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(Langs[0]) }
    var online by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf("nova") }
    var rate by remember { mutableStateOf(1f) }
    var pitch by remember { mutableStateOf(1f) }
    var playing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var lastAudio by remember { mutableStateOf<ByteArray?>(null) }
    val voices = listOf("nova", "alloy", "echo", "fable", "onyx", "shimmer")

    fun stop() { job?.cancel(); speaker.stop(); task.cancel(); playing = false }

    ToolScaffold("متن به صدا", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppField(text, { text = it }, "متنی که می‌خوای خونده بشه", Modifier.padding(horizontal = 16.dp), minLines = 5, maxLines = 12)
            SectionTitle("زبان")
            ChipRow(Langs, lang, { it.label }) { lang = it }
            SectionTitle("نوع صدا")
            ChipRow(listOf(false, true), online, { if (it) "صدای هوش مصنوعی (آنلاین)" else "صدای گوشی (آفلاین)" }) { online = it }
            if (online) {
                ChipRow(voices, voice, { it }) { voice = it }
                Text(
                    "سرویس رایگان آنلاین حداکثر ۵۰۰ حرف اول متن را می‌خواند و ممکن است گاهی شلوغ باشد.",
                    Modifier.padding(horizontal = 16.dp), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
            } else {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("سرعت: ${"%.1f".format(rate)}", fontSize = 13.sp)
                    Slider(rate, { rate = it }, valueRange = 0.5f..2f)
                    Text("زیر و بمی: ${"%.1f".format(pitch)}", fontSize = 13.sp)
                    Slider(pitch, { pitch = it }, valueRange = 0.5f..2f)
                    if (speaker.isReady() && !speaker.hasVoice(lang.locale)) {
                        Text(
                            "صدای ${lang.label} روی گوشی نصب نیست. از تنظیمات گوشی › Text-to-speech صدا را دانلود کن یا حالت آنلاین را بزن.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            GradButton(
                if (playing) "توقف" else "پخش",
                icon = if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                loading = task.loading, enabled = text.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = {
                    if (playing) {
                        stop()
                    } else {
                        playing = true
                        if (online) {
                            task.launch(scope) {
                                try {
                                    val bytes = Net.getBytes(Poll.speechUrl(text, voice), tries = 3)
                                    lastAudio = bytes
                                    playAudioBytes(ctx, bytes)
                                } finally { playing = false }
                            }
                        } else {
                            job = scope.launch {
                                try { speaker.speak(text, rate, pitch, lang.locale) } finally { playing = false }
                            }
                        }
                    }
                },
            )
            if (online && lastAudio != null) {
                GradButton(
                    "ذخیره‌ی فایل صوتی", icon = Icons.Rounded.Download,
                    colors = listOf(androidx.compose.ui.graphics.Color(0xFF12C2A7), androidx.compose.ui.graphics.Color(0xFF2EC4F1)),
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    onClick = {
                        val ok = saveBytesToDownloads(ctx, "AIHub_voice_${System.currentTimeMillis()}.mp3", "audio/mpeg", lastAudio!!)
                        toast(ctx, if (ok) "در Download/AIHub ذخیره شد" else "ذخیره نشد")
                    },
                )
            }
            ErrorText(task.error)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- دستیار نوشتن

private val writerTasks = listOf(
    "خلاصه‌سازی" to "Summarize the text clearly in a few bullet points.",
    "بازنویسی رسمی" to "Rewrite the text in a polished, formal tone.",
    "بازنویسی دوستانه" to "Rewrite the text in a warm, friendly, casual tone.",
    "اصلاح نگارش" to "Fix spelling, grammar and punctuation. Keep the meaning. Output only the corrected text.",
    "نوشتن ایمیل" to "Turn the text/idea into a complete professional email with a subject line.",
    "کپشن اینستاگرام" to "Write 3 catchy social media captions with fitting emojis and hashtags for this topic.",
    "شعر" to "Write a short, beautiful poem about this topic.",
    "ایده‌ی محتوا" to "Give 8 creative content ideas about this topic, each with one line of explanation.",
    "ساده‌سازی" to "Explain the text in very simple words, as if to a 12-year-old.",
)

@Composable
fun WriterScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    var sel by remember { mutableStateOf(writerTasks[0]) }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    ToolScaffold("دستیار نوشتن", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChipRow(writerTasks, sel, { it.first }) { sel = it }
            AppField(input, { input = it }, "متن یا موضوع", Modifier.padding(horizontal = 16.dp), minLines = 5, maxLines = 12)
            GradButton(
                "انجام بده", icon = Icons.Rounded.AutoAwesome, loading = task.loading, enabled = input.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = {
                    task.launch(scope) {
                        output = Ai.generate(
                            input,
                            system = "${sel.second} Reply in the same language as the user's text unless asked otherwise (default Persian).",
                            temperature = 0.8,
                        )
                    }
                },
            )
            ErrorText(task.error)
            if (output.isNotBlank()) ResultCard(output, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
        }
    }
}
