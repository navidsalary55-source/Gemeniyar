package com.example.aihub.ui

import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aihub.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** راهنمای یک بخش: متن مرحله‌به‌مرحله + ویدیوی آموزشی (assets/videos/<id>.mp4.b64) */
data class Guide(
    val id: String,
    val title: String,
    val emoji: String,
    val intro: String,
    val steps: List<String>,
    val tips: List<String>,
    val c1: Long,
    val c2: Long,
)

object Guides {
    val all: List<Guide> = listOf(
    Guide(
        id = "keys", title = "گرفتن کلید رایگان", emoji = "🔑",
        intro = "همه‌ی بخش‌ها با یک کلید رایگان کار می‌کنند. چند دقیقه وقت لازم است و فقط یک‌بار انجام می‌شود.",
        steps = listOf("سایت console.groq.com/keys را باز کن و با حساب گوگل وارد شو", "روی Create API Key بزن و کلید را کپی کن", "در برنامه: منو ← تنظیمات ← سرویس Groq ← کلید را بچسبان ← ذخیره", "اختیاری: برای تصویر و صدای پایدارتر، از enter.pollinations.ai کلید بگیر و در همان صفحه بگذار"),
        tips = listOf("کلید Groq تشخیص گفتار (Whisper) چت صوتی را هم فعال می‌کند", "کلید چند سرویس بگذار تا اگر یکی تمام شد، خودکار سرویس بعدی جواب بدهد", "کلیدت را برای کسی نفرست"),
        c1 = 0xFF7C6CFF, c2 = 0xFF3FA2F7,
    ),
    Guide(
        id = "chat", title = "چت ساده", emoji = "💬",
        intro = "گفتگوی سریع و روان با هوش مصنوعی؛ هر سوالی داشتی بپرس.",
        steps = listOf("پیامت را در کادر پایین بنویس", "دکمه‌ی ارسال را بزن؛ جواب زنده نوشته می‌شود", "روی جواب بزن تا کپی یا خوانده شود", "برای شروع گفتگوی جدید، پاک‌کردن را بزن"),
        tips = listOf("سوال را کامل و مشخص بنویس", "اگر خطا آمد، کلید را در تنظیمات بررسی کن"),
        c1 = 0xFF7C6CFF, c2 = 0xFF3FA2F7,
    ),
    Guide(
        id = "voice_chat", title = "چت صوتی", emoji = "🎙️",
        intro = "با ربات حرف بزن و جواب را با صدای طبیعی بشنو. هر پیام را می‌توانی دوباره پخش کنی.",
        steps = listOf("زبان و صدای ربات را بالای صفحه انتخاب کن", "روی میکروفون بزن و سوالت را بگو", "بعد از چند لحظه سکوت، پیام خودکار ارسال می‌شود", "ربات با صدا جواب می‌دهد؛ دکمه‌ی پخش کنار هر پیام برای شنیدن دوباره است", "«گفتگوی پیوسته» را روشن کن تا بعد از هر جواب دوباره گوش بدهد"),
        tips = listOf("برای تشخیص دقیق‌تر، کلید Groq را در تنظیمات بگذار", "اگر صدای آنلاین نیامد، خودکار با صدای گوشی می‌خواند", "در محیط ساکت واضح‌تر حرف بزن"),
        c1 = 0xFFFF6FB1, c2 = 0xFFFF9A62,
    ),
    Guide(
        id = "adv_chat", title = "چت پیشرفته", emoji = "✨",
        intro = "چت با شخصیت‌های مختلف (معلم، برنامه‌نویس، نویسنده…) و امکان فرستادن عکس.",
        steps = listOf("یک شخصیت از بالا انتخاب کن", "اگر خواستی با دکمه‌ی عکس، تصویر بفرست", "سوالت را بنویس و ارسال کن", "شخصیت را عوض کن تا سبک جواب‌ها تغییر کند"),
        tips = listOf("برای فهمیدن عکس، سرویس Groq یا Gemini بهتر است", "شخصیت «معلم صبور» برای یادگیری عالی است"),
        c1 = 0xFF6C5CE7, c2 = 0xFFB76CFF,
    ),
    Guide(
        id = "voice_changer", title = "تغییر صدا", emoji = "🎚️",
        intro = "دو حالت داری: افکت روی صدای خودت، یا خواندن جمله‌ات با یک صدای آماده‌ی دیگر.",
        steps = listOf("حالت «افکت»: ضبط کن، افکت را انتخاب کن و پخش بزن", "حالت «صدای دیگر»: جمله را بگو یا بنویس", "یک صدا (زن، مرد، بم، قصه‌گو…) انتخاب کن", "«ساخت با این صدا» را بزن، بشنو و ذخیره کن"),
        tips = listOf("تبدیل خودکار گفتار به متن به کلید Groq نیاز دارد؛ یا متن را بنویس", "کلون صدای افراد واقعی پشتیبانی نمی‌شود"),
        c1 = 0xFF12C2A7, c2 = 0xFF2EC4F1,
    ),
    Guide(
        id = "image_edit", title = "ویرایش تصویر", emoji = "🖼️",
        intro = "عکس را انتخاب کن و نور، رنگ و فیلتر را با لغزنده تنظیم کن.",
        steps = listOf("روی انتخاب عکس بزن", "فیلتر یا لغزنده‌های نور و رنگ را تنظیم کن", "نتیجه را زنده ببین", "ذخیره را بزن تا در گالری بماند"),
        tips = listOf("عکس با کیفیت بهتر نتیجه‌ی بهتری می‌دهد"),
        c1 = 0xFFFF8A5C, c2 = 0xFFFF5C8A,
    ),
    Guide(
        id = "animate", title = "متحرک‌سازی تصویر", emoji = "🎞️",
        intro = "به عکس ثابتت حرکت بده؛ زوم، حرکت و جلوه‌های زنده.",
        steps = listOf("عکس را انتخاب کن", "یک حرکت (زوم سینمایی، شناور، تاب…) را برگزین", "افکت برف، ستاره یا حباب را اضافه کن و سرعت را تنظیم کن", "برای ذخیره‌ی ویدیو از «ضبط صفحه»ی گوشی استفاده کن"),
        tips = listOf("عکس‌هایی با سوژه‌ی واضح بهتر می‌شوند"),
        c1 = 0xFF22C1C3, c2 = 0xFF5BC97B,
    ),
    Guide(
        id = "translate", title = "مترجم هوشمند", emoji = "🌐",
        intro = "ترجمه‌ی دقیق بین ۱۲ زبان، با امکان شنیدن متن ترجمه‌شده.",
        steps = listOf("زبان مبدأ و مقصد را انتخاب کن", "متن را بنویس یا بچسبان", "ترجمه را بزن", "با دکمه‌ی بلندگو تلفظ را بشنو"),
        tips = listOf("متن‌های کوتاه‌تر دقیق‌تر ترجمه می‌شوند", "می‌توانی ترجمه را کپی کنی"),
        c1 = 0xFF3FA2F7, c2 = 0xFF2ED3C6,
    ),
    Guide(
        id = "song", title = "ساخت آهنگ", emoji = "🎵",
        intro = "موضوع و حال‌وهوا را بگو؛ متن، آکورد و ملودی ساخته و پخش می‌شود.",
        steps = listOf("ایده‌ی آهنگ را بنویس", "سبک، حس‌وحال و زبان ترانه را انتخاب کن", "دکمه‌ی ساخت را بزن", "ملودی را پخش کن و متن را کپی کن"),
        tips = listOf("هر چه توضیح دقیق‌تر، آهنگ بهتر", "چند بار بساز و بهترین را نگه دار"),
        c1 = 0xFFFF5C8A, c2 = 0xFFB76CFF,
    ),
    Guide(
        id = "website", title = "ساخت سایت", emoji = "🌍",
        intro = "سایتت را توضیح بده تا صفحه‌ی کامل HTML ساخته و نمایش داده شود.",
        steps = listOf("توضیح سایت را بنویس (موضوع، رنگ، بخش‌ها)", "ساخت را بزن", "نتیجه را در پیش‌نمایش ببین", "کد را کپی یا ذخیره کن"),
        tips = listOf("برای سایت‌های بلند، کلید Cerebras عالی است", "اگر خطا داد، توضیح را کوتاه‌تر کن"),
        c1 = 0xFF4F8CFF, c2 = 0xFF7C6CFF,
    ),
    Guide(
        id = "draw", title = "ساخت نقاشی", emoji = "🎨",
        intro = "از روی متن تصویر بساز؛ فارسی هم بنویسی خودکار به پرامپت دقیق تبدیل می‌شود.",
        steps = listOf("چیزی که می‌خواهی را توصیف کن (فارسی هم می‌شود)", "سبک و اندازه را انتخاب کن", "ساخت را بزن (چند ثانیه صبر کن)", "تصویر را در گالری ذخیره کن"),
        tips = listOf("اگر شلوغ بود چند لحظه بعد دوباره بزن", "کلید Pollinations در تنظیمات پایداری را بیشتر می‌کند"),
        c1 = 0xFFFFB74D, c2 = 0xFFFF6F91,
    ),
    Guide(
        id = "char3d", title = "کاراکتر سه‌بعدی", emoji = "🧊",
        intro = "کاراکتر مورد نظرت را توصیف کن تا مدل سه‌بعدی بسازد و بچرخانی.",
        steps = listOf("شخصیت را توصیف کن", "ساخت را بزن", "با انگشت بچرخانش", "از نتیجه خروجی بگیر"),
        tips = listOf("توصیف ساده‌تر نتیجه‌ی تمیزتر می‌دهد"),
        c1 = 0xFF8E54E9, c2 = 0xFF47B8E0,
    ),
    Guide(
        id = "tts", title = "متن به صدا", emoji = "🔊",
        intro = "هر متنی را با صدای آنلاین طبیعی یا صدای گوشی بخوان و ذخیره کن.",
        steps = listOf("متن را بنویس", "حالت آنلاین یا گوشی و صدا را انتخاب کن", "پخش را بزن", "در حالت آنلاین فایل mp3 را ذخیره کن"),
        tips = listOf("صدای آنلاین طبیعی‌تر است", "سرعت و زیر و بمی در حالت گوشی قابل تنظیم است"),
        c1 = 0xFF00C9A7, c2 = 0xFF3FA2F7,
    ),
    Guide(
        id = "speech_video", title = "گفتار به ویدیو", emoji = "🎬",
        intro = "یک ایده بده؛ برنامه صحنه‌ها را می‌نویسد، تصویر می‌سازد و ویدیوی اسلایدی با گفتار می‌دهد.",
        steps = listOf("ایده یا متن را بنویس", "زبان روایت، تعداد صحنه و سبک تصویر را انتخاب کن", "ساخت را بزن؛ چند دقیقه طول می‌کشد", "ویدیو را ببین و ذخیره کن"),
        tips = listOf("بخش پرمصرف است؛ کلید Pollinations کمک می‌کند", "ایده‌ی کوتاه و روشن بهتر است"),
        c1 = 0xFFF857A6, c2 = 0xFFFF5858,
    ),
    Guide(
        id = "writer", title = "دستیار نوشتن", emoji = "✍️",
        intro = "خلاصه، ایمیل، کپشن و بازنویسی متن؛ همه در یک‌جا.",
        steps = listOf("نوع کار را انتخاب کن", "متن یا موضوع را وارد کن", "اجرا را بزن", "نتیجه را کپی یا به اشتراک بگذار"),
        tips = listOf("لحن مورد نظر را در متن بنویس"),
        c1 = 0xFF56AB2F, c2 = 0xFFA8E063,
    )
    )

    /** عنوان صفحه‌ی ابزار ← راهنمای مربوط */
    fun forTitle(title: String): Guide? {
        val id = if (title == "تنظیمات") "keys" else null
        return all.firstOrNull { it.title == title || it.id == id }
    }
}

private fun toFa(n: Int) = n.toString().map { "۰۱۲۳۴۵۶۷۸۹"[it - '0'] }.joinToString("")

/** پخش ویدیوی آموزشی از assets (base64) */
@Composable
fun GuideVideo(id: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var file by remember(id) { mutableStateOf<File?>(null) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        withContext(Dispatchers.IO) {
            try {
                val f = File(ctx.cacheDir, "guide_$id.mp4")
                if (!f.exists() || f.length() < 1000) {
                    val txt = ctx.assets.open("videos/$id.mp4.b64").bufferedReader().use { it.readText() }
                    f.writeBytes(Base64.decode(txt.trim(), Base64.DEFAULT))
                }
                file = f
            } catch (e: Exception) {
                failed = true
            }
        }
    }
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF0B0D1A)),
        contentAlignment = Alignment.Center,
    ) {
        val f = file
        when {
            failed -> Text("ویدیو در دسترس نیست", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            f == null -> CircularProgressIndicator(color = Color.White)
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    VideoView(c).apply {
                        val mc = MediaController(c)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        setVideoPath(f.absolutePath)
                        setOnPreparedListener { mp -> mp.isLooping = true; start() }
                    }
                },
                onRelease = { it.stopPlayback() },
            )
        }
    }
}

/** پنجره‌ی راهنما: ویدیو + مراحل + نکته‌ها */
@Composable
fun GuideDialog(g: Guide, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp), color = c.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(g.c1), Color(g.c2))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(g.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("راهنمای ${g.title}", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "بستن", tint = Color.White) }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(g.intro, fontSize = 14.5.sp, lineHeight = 24.sp, color = c.onBackground)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        GuideVideo(g.id, Modifier.height(440.dp).aspectRatio(9f / 16f))
                    }
                    Text("مرحله‌به‌مرحله", fontWeight = FontWeight.Bold, color = c.primary, fontSize = 15.sp)
                    g.steps.forEachIndexed { i, s ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(28.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(g.c1), Color(g.c2))), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text(toFa(i + 1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            Spacer(Modifier.width(10.dp))
                            Text(s, Modifier.weight(1f).padding(top = 2.dp), fontSize = 14.sp, lineHeight = 23.sp, color = c.onBackground)
                        }
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = c.surfaceVariant) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡 نکته‌ها", fontWeight = FontWeight.Bold, color = c.onSurfaceVariant)
                            g.tips.forEach { Text("• $it", fontSize = 13.sp, lineHeight = 21.sp, color = c.onSurfaceVariant) }
                        }
                    }
                    Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("متوجه شدم") }
                }
            }
        }
    }
}

/** صفحه‌ی راهنما و آموزش: فهرست همه‌ی بخش‌ها */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf<Guide?>(null) }
    val c = MaterialTheme.colorScheme
    ToolScaffold("راهنما و آموزش", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "برای هر بخش یک متن راهنما و یک ویدیوی آموزشی کوتاه داریم. اول «گرفتن کلید رایگان» را ببین.",
                fontSize = 13.5.sp, lineHeight = 22.sp, color = c.onBackground.copy(alpha = 0.8f), textAlign = TextAlign.Start,
            )
            Guides.all.forEach { g ->
                Surface(
                    onClick = { open = g }, shape = RoundedCornerShape(20.dp), color = c.surface.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(50.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(g.c1), Color(g.c2))), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(g.emoji, fontSize = 26.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.title, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = c.onSurface)
                            Text(g.intro, fontSize = 12.sp, lineHeight = 18.sp, color = c.onSurface.copy(alpha = 0.65f), maxLines = 2)
                        }
                        Icon(Icons.Rounded.PlayCircle, null, tint = c.primary, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    open?.let { GuideDialog(it) { open = null } }
}
