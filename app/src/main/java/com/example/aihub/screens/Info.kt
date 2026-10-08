package com.example.aihub.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.R
import com.example.aihub.core.*
import com.example.aihub.ui.*

// ---------------------------------------------------------------- تنظیمات

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val task = remember { TaskState() }
    val listTask = remember { TaskState() }
    val c = MaterialTheme.colorScheme

    var prov by remember { mutableStateOf(Store.provider) }
    var key by remember(prov) { mutableStateOf(Store.keyFor(prov)) }
    var model by remember(prov) { mutableStateOf(Store.modelFor(prov)) }
    var models by remember(prov) { mutableStateOf<List<String>>(emptyList()) }
    var show by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }

    ToolScaffold("تنظیمات", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("سرویس هوش مصنوعی")
            ChipRow(Provider.values().toList(), prov, { it.label }) { prov = it }
            Surface(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp), color = c.surfaceVariant,
            ) {
                Text(prov.note, Modifier.padding(14.dp), fontSize = 13.sp, lineHeight = 20.sp)
            }

            if (prov.needsKey) {
                prov.keyUrl?.let { url ->
                    TextButton(onClick = { openUrl(ctx, url) }, Modifier.padding(horizontal = 8.dp)) {
                        Icon(Icons.Rounded.VpnKey, null); Spacer(Modifier.width(6.dp)); Text("دریافت کلید رایگان ${prov.label}")
                    }
                }
                AppField(
                    key, { key = it }, "API Key", Modifier.padding(horizontal = 16.dp),
                    ltr = true, password = !show, maxLines = 1,
                    trailing = {
                        IconButton(onClick = { show = !show }) {
                            Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "نمایش")
                        }
                    },
                )
            }

            AppField(model, { model = it }, "نام مدل", Modifier.padding(horizontal = 16.dp), ltr = true, maxLines = 1)
            OutlinedButton(
                onClick = {
                    Store.save(prov, key.trim(), model.trim())
                    listTask.launch(scope) { models = Ai.listModels() }
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            ) {
                if (listTask.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("فهرست مدل‌های این سرویس")
            }
            ErrorText(listTask.error)
            if (models.isNotEmpty()) {
                Surface(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(max = 240.dp),
                    shape = RoundedCornerShape(18.dp), color = c.surfaceVariant,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        models.take(80).forEach { m ->
                            Text(
                                m,
                                Modifier.fillMaxWidth().clickable { model = m }.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 13.sp,
                                color = if (m == model) c.primary else c.onSurfaceVariant,
                                fontWeight = if (m == model) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            GradButton(
                "ذخیره و فعال‌سازی", icon = Icons.Rounded.Check,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                onClick = { Store.save(prov, key.trim(), model.trim()); toast(ctx, "ذخیره شد") },
            )
            OutlinedButton(
                onClick = {
                    Store.save(prov, key.trim(), model.trim())
                    task.launch(scope) { testResult = Ai.generate("یک جمله‌ی کوتاه و دوستانه‌ی فارسی بگو.", maxTokens = 100) }
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            ) {
                if (task.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("تست اتصال")
            }
            ErrorText(task.error)
            if (testResult.isNotBlank() && task.error == null) {
                Text("✅ $testResult", Modifier.padding(horizontal = 20.dp), fontSize = 13.5.sp)
            }

            SectionTitle("ظاهر برنامه")
            ChipRow(listOf(0, 1, 2), Store.themeMode, { listOf("خودکار", "روشن", "تاریک")[it] }) { Store.saveTheme(it) }
            Text(
                "نکته: اگر سرویسی وصل نشد، VPN را روشن کن یا سرویس دیگری انتخاب کن. سهمیه‌ی رایگان هر سرویس محدود است.",
                Modifier.padding(horizontal = 20.dp), fontSize = 12.sp, color = c.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- درباره ما

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    ToolScaffold("درباره ما", onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ResImage("logo", Modifier.size(104.dp).clip(CircleShape))
            Text("هوش‌یار", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("نسخه ${Config.APP_VERSION}", fontSize = 12.5.sp, color = c.primary)
            Text(
                "هوش‌یار یک جعبه‌ابزار هوش مصنوعی است: چت، چت صوتی، ترجمه، ساخت تصویر و سایت، تغییر صدا، ساخت آهنگ و کاراکتر سه‌بعدی و ابزارهای دیگر در یک اپلیکیشن.",
                textAlign = TextAlign.Center, lineHeight = 26.sp,
            )
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = c.surfaceVariant) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("فناوری‌ها", fontWeight = FontWeight.Bold)
                    Text("• سرویس‌های رایگان متن: Groq، Cerebras، OpenRouter، Mistral، Gemini و Pollinations", fontSize = 13.5.sp)
                    Text("• Pollinations برای ساخت تصویر و صدای آنلاین", fontSize = 13.5.sp)
                    Text("• تشخیص گفتار و متن‌به‌صدای خود اندروید", fontSize = 13.5.sp)
                    Text("• Jetpack Compose و Material 3", fontSize = 13.5.sp)
                }
            }
            Text(
                "پاسخ‌های هوش مصنوعی ممکن است اشتباه باشند؛ برای موضوعات مهم (پزشکی، حقوقی، مالی) حتماً بررسی کن.",
                textAlign = TextAlign.Center, fontSize = 12.sp, color = c.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- تماس با ما

@Composable
private fun ContactItem(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = c.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GradCircleIcon(icon, size = 42)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(sub, fontSize = 12.5.sp, color = c.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun ContactScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }

    ToolScaffold("تماس با ما", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ContactItem(Icons.Rounded.Email, "ایمیل", Config.CONTACT_EMAIL) { sendEmail(ctx, Config.CONTACT_EMAIL, "پیام از اپ هوش‌یار") }
            ContactItem(Icons.Rounded.Send, "تلگرام", Config.CONTACT_TELEGRAM) { openUrl(ctx, Config.CONTACT_TELEGRAM) }
            ContactItem(Icons.Rounded.Public, "وب‌سایت", Config.CONTACT_WEBSITE) { openUrl(ctx, Config.CONTACT_WEBSITE) }
            Spacer(Modifier.height(4.dp))
            SectionTitle("ارسال نظر یا پیشنهاد")
            AppField(msg, { msg = it }, "پیامت رو بنویس", minLines = 4)
            GradButton(
                "ارسال با ایمیل", icon = Icons.Rounded.Send, enabled = msg.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { sendEmail(ctx, Config.CONTACT_EMAIL, "بازخورد اپ هوش‌یار", msg) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
