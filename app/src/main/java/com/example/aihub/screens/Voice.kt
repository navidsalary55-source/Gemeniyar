package com.example.aihub.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.core.*
import com.example.aihub.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class VState { IDLE, LISTENING, THINKING, SPEAKING }

private class VoiceCtl(val ctx: Context, val scope: CoroutineScope) {
    val speaker = Speaker(ctx)
    val stt = SpeechInput(ctx)
    val msgs = mutableStateListOf<ChatMsg>()
    var state by mutableStateOf(VState.IDLE)
    var info by mutableStateOf("برای شروع روی میکروفون بزن")
    var continuous by mutableStateOf(false)
    var lang by mutableStateOf(Langs[0])
    private var id = 0L
    private var job: Job? = null

    fun listen() {
        if (!stt.available()) {
            info = "سرویس تشخیص گفتار روی این گوشی نیست. برنامه‌ی Google را نصب کن."
            return
        }
        state = VState.LISTENING
        info = "در حال گوش دادن…"
        stt.start(lang.tag, onText = { handle(it) }, onFail = { state = VState.IDLE; info = it })
    }

    private fun handle(text: String) {
        msgs.add(ChatMsg(id++, true, text))
        state = VState.THINKING
        info = "در حال فکر کردن…"
        val history = msgs.filter { !it.isError }.toList()
        job = scope.launch {
            try {
                val reply = Ai.generateWith(
                    history,
                    system = "You are a friendly voice assistant. Answer in ${lang.name}. Speak naturally in short, clear " +
                        "sentences (max 4). Never use markdown, lists or emojis.",
                    temperature = 0.8,
                    maxTokens = 600,
                )
                msgs.add(ChatMsg(id++, false, reply))
                state = VState.SPEAKING
                info = "در حال صحبت…"
                speaker.speak(plainForSpeech(reply), locale = lang.locale)
                state = VState.IDLE
                info = "دوباره روی میکروفون بزن"
                if (continuous) listen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                msgs.add(ChatMsg(id++, false, e.message ?: "خطا", isError = true))
                state = VState.IDLE
                info = "دوباره امتحان کن"
            }
        }
    }

    fun stopAll() {
        job?.cancel(); stt.release(); speaker.stop(); state = VState.IDLE; info = "متوقف شد"
    }

    fun release() { job?.cancel(); stt.release(); speaker.shutdown() }
}

@Composable
fun VoiceChatScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ctl = remember { VoiceCtl(ctx, scope) }
    DisposableEffect(Unit) { onDispose { ctl.release() } }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) ctl.listen() else toast(ctx, "بدون اجازه‌ی میکروفون چت صوتی کار نمی‌کند")
    }
    fun onOrb() {
        if (ctl.state != VState.IDLE) { ctl.stopAll(); return }
        if (hasMic(ctx)) ctl.listen() else perm.launch(Manifest.permission.RECORD_AUDIO)
    }

    ToolScaffold("چت صوتی", onBack) {
        ChipRow(Langs.take(6), ctl.lang, { it.label }) { ctl.lang = it }
        MessageList(
            ctl.msgs, typing = false,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        if (ctl.stt.listening && ctl.stt.partial.isNotBlank()) {
            Text(
                ctl.stt.partial, Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Orb(ctl.state, ctl.stt.level, ::onOrb)
            Spacer(Modifier.height(10.dp))
            Text(ctl.info, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ctl.continuous, onCheckedChange = { ctl.continuous = it })
                Spacer(Modifier.width(8.dp))
                Text("گفتگوی پیوسته (بعد از جواب دوباره گوش بده)", fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun Orb(state: VState, level: Float, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "orb")
    val pulse by inf.animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
    )
    val lv by animateFloatAsState(level, label = "lv")
    val scale = when (state) {
        VState.LISTENING -> 1f + lv * 0.28f
        VState.THINKING, VState.SPEAKING -> pulse
        VState.IDLE -> 1f
    }
    val colors = when (state) {
        VState.LISTENING -> listOf(Color(0xFFFF6FB1), Color(0xFFFF9A62))
        VState.THINKING -> listOf(Color(0xFFFFB74D), Color(0xFFFF8A5C))
        VState.SPEAKING -> listOf(Color(0xFF12C2A7), Color(0xFF2EC4F1))
        VState.IDLE -> Brand
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Box(
            Modifier.size(150.dp).graphicsLayer { scaleX = scale * 1.08f; scaleY = scale * 1.08f }
                .background(colors[0].copy(alpha = 0.18f), CircleShape)
        )
        Box(
            Modifier.size(104.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .background(grad(colors), CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (state == VState.IDLE) Icons.Rounded.Mic else Icons.Rounded.Stop,
                null, tint = Color.White, modifier = Modifier.size(44.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- تغییر صدا

private class VcCtl(val scope: CoroutineScope) {
    val rec = PcmRecorder()
    var recording by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var data by mutableStateOf<ShortArray?>(null)
    var fx by mutableStateOf(VoiceFx.CHIPMUNK)
    var secs by mutableStateOf(0)
    var error by mutableStateOf<String?>(null)
    private var job: Job? = null

    fun start() {
        if (recording) return
        stopPlay()
        error = null; data = null; secs = 0; recording = true
        job = scope.launch {
            val timer = launch { while (true) { delay(1000); secs++ } }
            try {
                data = rec.record(15000)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "خطا در ضبط"
            } finally {
                timer.cancel(); recording = false
            }
        }
    }

    fun stopRec() { rec.stop() }

    fun play() {
        val d = data ?: return
        stopPlay()
        playing = true
        job = scope.launch {
            try {
                val (s, r) = withContext(Dispatchers.Default) { applyFx(d, fx, rec.sampleRate) }
                playPcm(s, r)
            } finally {
                playing = false
            }
        }
    }

    fun stopPlay() {
        if (playing) { job?.cancel(); playing = false }
    }

    fun release() { rec.stop(); job?.cancel() }
}

@Composable
fun VoiceChangerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ctl = remember { VcCtl(scope) }
    DisposableEffect(Unit) { onDispose { ctl.release() } }
    val c = MaterialTheme.colorScheme

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) ctl.start() else toast(ctx, "اجازه‌ی میکروفون لازم است")
    }

    ToolScaffold("تغییر صدا", onBack) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "یک جمله بگو (تا ۱۵ ثانیه)، بعد افکت رو انتخاب کن و بشنو!",
                fontSize = 14.sp, color = c.onBackground.copy(alpha = 0.7f),
            )
            Box(
                Modifier.size(120.dp)
                    .background(grad(if (ctl.recording) listOf(Color(0xFFFF5C8A), Color(0xFFFF9A62)) else listOf(Color(0xFF12C2A7), Color(0xFF2EC4F1))), CircleShape)
                    .clickable {
                        if (ctl.recording) ctl.stopRec()
                        else if (hasMic(ctx)) ctl.start() else perm.launch(Manifest.permission.RECORD_AUDIO)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (ctl.recording) Icons.Rounded.Stop else Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            Text(
                if (ctl.recording) "در حال ضبط… ${ctl.secs} ثانیه (برای پایان بزن)"
                else if (ctl.data != null) "ضبط آماده است ✔" else "برای شروع ضبط بزن",
                fontWeight = FontWeight.Bold,
            )
            ErrorText(ctl.error)
            if (ctl.data != null && !ctl.recording) {
                SectionTitle("افکت صدا")
                val rows = VoiceFx.values().toList().chunked(4)
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { fx ->
                            val sel = ctl.fx == fx
                            Surface(
                                onClick = { ctl.fx = fx },
                                shape = RoundedCornerShape(50),
                                color = if (sel) c.primary else c.surfaceVariant,
                            ) {
                                Text(
                                    fx.label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    color = if (sel) Color.White else c.onSurfaceVariant, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                GradButton(
                    text = if (ctl.playing) "توقف" else "پخش با افکت",
                    icon = if (ctl.playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    onClick = { if (ctl.playing) ctl.stopPlay() else ctl.play() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
