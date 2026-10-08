package com.example.aihub.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.core.*
import com.example.aihub.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ---------------------------------------------------------------- ساخت آهنگ

private class Song(
    val title: String, val lyrics: String, val chords: String, val tempo: Int, val melody: List<Pair<Int, Double>>,
)

private fun parseSong(raw: String): Song {
    val o = JSONObject(raw.stripCodeFence())
    val arr = o.optJSONArray("melody")
    val notes = ArrayList<Pair<Int, Double>>()
    if (arr != null) {
        for (i in 0 until arr.length()) {
            val n = arr.optJSONArray(i) ?: continue
            notes.add(n.optInt(0, 0) to n.optDouble(1, 1.0).coerceIn(0.125, 4.0))
        }
    }
    return Song(
        o.optString("title", "بدون عنوان"), o.optString("lyrics"), o.optString("chords"),
        o.optInt("tempo", 100), notes,
    )
}

@Composable
fun SongScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    val genres = listOf("پاپ", "عاشقانه", "رپ", "راک", "سنتی", "لالایی", "شاد و رقصی", "حماسی")
    val moods = listOf("شاد", "غمگین", "آرامش‌بخش", "پرانرژی", "نوستالژیک")
    var idea by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(genres[0]) }
    var mood by remember { mutableStateOf(moods[0]) }
    var lang by remember { mutableStateOf(Langs[0]) }
    var song by remember { mutableStateOf<Song?>(null) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    var playing by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }

    fun stopAll() { playJob?.cancel(); speaker.stop(); playing = false; reading = false }

    ToolScaffold("ساخت آهنگ", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppField(idea, { idea = it }, "درباره‌ی چی باشه؟ (ایده‌ی آهنگ)", Modifier.padding(horizontal = 16.dp), minLines = 2)
            SectionTitle("سبک"); ChipRow(genres, genre, { it }) { genre = it }
            SectionTitle("حس و حال"); ChipRow(moods, mood, { it }) { mood = it }
            SectionTitle("زبان ترانه"); ChipRow(Langs.take(6), lang, { it.label }) { lang = it }
            GradButton(
                "بساز", icon = Icons.Rounded.MusicNote, loading = task.loading, enabled = idea.isNotBlank(),
                colors = listOf(Color(0xFFFF5C8A), Color(0xFFB76CFF)),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = {
                    stopAll()
                    task.launch(scope) {
                        val raw = Ai.generate(
                            "Idea: $idea\nGenre: $genre\nMood: $mood\nLyrics language: ${lang.name}",
                            system = "You are a songwriter and composer. Return ONLY a JSON object with keys: " +
                                "\"title\" (string), \"lyrics\" (string, with [Verse]/[Chorus] sections, about 12-20 lines), " +
                                "\"chords\" (string, chord progression per section, e.g. Am - F - C - G), " +
                                "\"tempo\" (integer BPM 70-140), " +
                                "\"melody\" (array of 32-48 pairs [midiNote, beats] forming a catchy simple tune in a major or minor scale; " +
                                "midiNote between 57 and 81, 0 means rest; beats is one of 0.5, 1, 1.5, 2). " +
                                "The melody should repeat a motif and end on the tonic.",
                            json = true, temperature = 0.9, maxTokens = 4000,
                        )
                        song = parseSong(raw)
                    }
                },
            )
            ErrorText(task.error)
            song?.let { s ->
                Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎵 ${s.title}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("تمپو: ${s.tempo} BPM", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        if (s.chords.isNotBlank()) {
                            Text("آکوردها", fontWeight = FontWeight.Bold)
                            Text(s.chords, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Text("ترانه", fontWeight = FontWeight.Bold)
                        Text(s.lyrics, lineHeight = 26.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradButton(
                                if (playing) "توقف" else "پخش ملودی",
                                icon = if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                enabled = s.melody.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (playing) stopAll() else {
                                        stopAll()
                                        playing = true
                                        playJob = scope.launch {
                                            try {
                                                val pcm = withContext(Dispatchers.Default) { synthMelody(s.melody, s.tempo) }
                                                playPcm(pcm, 22050)
                                            } finally { playing = false }
                                        }
                                    }
                                },
                            )
                            GradButton(
                                if (reading) "توقف" else "خواندن ترانه",
                                icon = Icons.Rounded.RecordVoiceOver,
                                colors = listOf(Color(0xFF12C2A7), Color(0xFF2EC4F1)),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (reading) stopAll() else {
                                        stopAll()
                                        reading = true
                                        playJob = scope.launch {
                                            try {
                                                speaker.speak(s.lyrics.replace(Regex("\\[.*?]"), ""), 0.9f, 1f, lang.locale)
                                            } finally { reading = false }
                                        }
                                    }
                                },
                            )
                        }
                        val clip = LocalClipboardManager.current
                        OutlinedButton(onClick = {
                            clip.setText(AnnotatedString("${s.title}\n\n${s.chords}\n\n${s.lyrics}")); toast(ctx, "کپی شد")
                        }) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("کپی متن") }
                    }
                }
                Text(
                    "ملودی ساده با سینتی‌سایزر داخلی ساخته می‌شود؛ آهنگ کامل با خواننده نیست.",
                    Modifier.padding(horizontal = 20.dp), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- ساخت سایت / کاراکتر ۳بعدی (مشترک)

private const val WEB_SYSTEM =
    "You are an expert front-end developer and designer. Produce ONE complete, self-contained HTML5 file " +
        "(inline CSS and JS, no external files at all — use system fonts). " +
        "Make it beautiful, modern, fully responsive and mobile-first, with gradients, spacing, hover effects and small animations. " +
        "Use the language of the user's request for visible text (default Persian) and set dir=\"rtl\" lang=\"fa\" when Persian. " +
        "For images use CSS gradients or inline SVG only (no remote images). Keep the file compact: under 350 lines. " +
        "Output ONLY the raw HTML starting with <!DOCTYPE html>. No markdown, no explanations."

private const val THREE_SYSTEM =
    "You are an expert three.js developer and 3D character artist. Produce ONE complete self-contained HTML5 file that shows " +
        "an original 3D character built ONLY from three.js primitives (spheres, boxes, cylinders, cones, capsules via cylinders+spheres), " +
        "with nice colors, MeshStandardMaterial, shadows-free soft lighting (AmbientLight + 2 DirectionalLights), a gradient page background, " +
        "and a gentle idle animation (breathing, head bob, blinking or waving). " +
        "Load three.js ONLY with: <script src=\"https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js\"></script>. " +
        "Do NOT use OrbitControls or any other external script. Implement drag-to-rotate (pointer events) and pinch/wheel zoom manually. " +
        "The canvas must fill the whole screen (100vw x 100vh), handle resize, and use devicePixelRatio. Keep the code compact (under 250 lines). " +
        "Output ONLY the raw HTML starting with <!DOCTYPE html>. No markdown, no explanations."

@Composable
private fun HtmlBuilder(
    title: String, hint: String, system: String, styles: List<String>, buttonIcon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<Color>, fileName: String, onBack: () -> Unit, showCode: Boolean,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val clip = LocalClipboardManager.current
    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(styles[0]) }
    var tweak by remember { mutableStateOf("") }
    var html by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }

    ToolScaffold(title, onBack) {
        if (html.isEmpty()) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppField(prompt, { prompt = it }, hint, Modifier.padding(horizontal = 16.dp), minLines = 4)
                SectionTitle("سبک"); ChipRow(styles, style, { it }) { style = it }
                GradButton(
                    "بساز", icon = buttonIcon, loading = task.loading, enabled = prompt.isNotBlank(), colors = colors,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    onClick = {
                        task.launch(scope) {
                            val r = Ai.generate("Request: $prompt\nStyle: $style", system = system, temperature = 0.8, maxTokens = 16000)
                            html = r.stripCodeFence()
                            tab = 0
                        }
                    },
                )
                if (task.loading) Text("ساخت چند ده ثانیه طول می‌کشد…", Modifier.padding(horizontal = 20.dp), fontSize = 12.5.sp)
                ErrorText(task.error)
            }
        } else {
            if (showCode) ChipRow(listOf(0, 1), tab, { if (it == 0) "پیش‌نمایش" else "کد" }) { tab = it }
            Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(18.dp))) {
                if (tab == 0) {
                    HtmlPreview(html, Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                        Text(html, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
            ErrorText(task.error)
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    tweak, { tweak = it }, Modifier.weight(1f), placeholder = { Text("چی رو تغییر بدم؟") },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                )
                GradButton(
                    "اعمال", loading = task.loading, enabled = tweak.isNotBlank(), colors = colors,
                    onClick = {
                        task.launch(scope) {
                            val r = Ai.generate(
                                "Current HTML:\n$html\n\nApply this change and return the FULL updated HTML: $tweak",
                                system = system, temperature = 0.6, maxTokens = 16000,
                            )
                            html = r.stripCodeFence(); tweak = ""
                        }
                    },
                )
            }
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton({ clip.setText(AnnotatedString(html)); toast(ctx, "کد کپی شد") }, Modifier.weight(1f)) { Text("کپی کد") }
                OutlinedButton({
                    val ok = saveBytesToDownloads(ctx, "$fileName-${System.currentTimeMillis()}.html", "text/html", html.toByteArray())
                    toast(ctx, if (ok) "در Download/AIHub ذخیره شد" else "ذخیره نشد")
                }, Modifier.weight(1f)) { Text("ذخیره HTML") }
                OutlinedButton({ shareText(ctx, html) }, Modifier.weight(1f)) { Text("اشتراک") }
                OutlinedButton({ html = ""; task.cancel() }, Modifier.weight(0.8f)) { Text("جدید") }
            }
        }
    }
}

@Composable
fun WebsiteScreen(onBack: () -> Unit) = HtmlBuilder(
    "ساخت سایت", "سایتت چی باشه؟ مثلاً: صفحه‌ی معرفی کافه‌ی من با منو و تماس",
    WEB_SYSTEM, listOf("مدرن", "تیره و نئونی", "رنگارنگ", "مینیمال", "شرکتی"),
    Icons.Rounded.Language, listOf(Color(0xFF4F8CFF), Color(0xFF7C6CFF)), "site", onBack, showCode = true,
)

@Composable
fun Char3DScreen(onBack: () -> Unit) = HtmlBuilder(
    "کاراکتر سه‌بعدی", "کاراکترت چطوری باشه؟ مثلاً: ربات کوچولوی دوست‌داشتنی با آنتن و چشم‌های درشت",
    THREE_SYSTEM, listOf("کارتونی", "ربات", "فانتزی", "حیوان", "فضایی"),
    Icons.Rounded.ViewInAr, listOf(Color(0xFF8E54E9), Color(0xFF47B8E0)), "character3d", onBack, showCode = false,
)

// ---------------------------------------------------------------- ساخت نقاشی

@Composable
fun DrawScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val styles = listOf(
        "واقع‌گرایانه" to "photorealistic, ultra detailed, 8k",
        "انیمه" to "anime style, vibrant, studio quality",
        "آبرنگ" to "watercolor painting, soft textures",
        "رنگ روغن" to "oil painting, rich brush strokes",
        "پیکسل‌آرت" to "pixel art, 16-bit retro game style",
        "سه‌بعدی" to "3D render, cute, soft lighting, Pixar style",
        "لوگو" to "minimal vector logo, flat design, clean",
        "فانتزی" to "epic fantasy concept art, dramatic lighting",
    )
    val sizes = listOf("مربع" to (1024 to 1024), "افقی" to (1280 to 720), "عمودی" to (720 to 1280))
    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(styles[0]) }
    var size by remember { mutableStateOf(sizes[0]) }
    var result by remember { mutableStateOf<Bitmap?>(null) }

    ToolScaffold("ساخت نقاشی", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppField(prompt, { prompt = it }, "چی بکشم؟ (فارسی هم می‌تونی بنویسی)", Modifier.padding(horizontal = 16.dp), minLines = 3)
            SectionTitle("سبک"); ChipRow(styles, style, { it.first }) { style = it }
            SectionTitle("اندازه"); ChipRow(sizes, size, { it.first }) { size = it }
            GradButton(
                if (result == null) "بکش" else "یکی دیگه", icon = Icons.Rounded.Brush, loading = task.loading, enabled = prompt.isNotBlank(),
                colors = listOf(Color(0xFFFFB74D), Color(0xFFFF6F91)),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = {
                    task.launch(scope) {
                        val p = Poll.betterPrompt(prompt, style.second)
                        val bytes = Net.getBytes(Poll.imageUrl(p, size.second.first, size.second.second), tries = 4, waitMs = 8000)
                        val bmp = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                            ?: throw AiException("تصویر دریافتی قابل نمایش نبود. دوباره امتحان کن.")
                        result = bmp
                    }
                },
            )
            if (task.loading) {
                Text("در حال کشیدن… ممکن است تا ۳۰ ثانیه طول بکشد (سرویس رایگان است).", Modifier.padding(horizontal = 20.dp), fontSize = 12.5.sp)
            }
            ErrorText(task.error)
            result?.let { b ->
                BitmapImage(b, Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)))
                GradButton(
                    "ذخیره در گالری", icon = Icons.Rounded.Download,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    onClick = { toast(ctx, if (saveBitmapToGallery(ctx, b)) "در گالری (Pictures/AIHub) ذخیره شد" else "ذخیره نشد") },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
