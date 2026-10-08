package com.example.aihub.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

fun hasMic(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

fun decodeBitmap(ctx: Context, uri: Uri, maxDim: Int = 1280): Bitmap? = try {
    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
    ImageDecoder.decodeBitmap(src) { dec, info, _ ->
        dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val m = max(info.size.width, info.size.height)
        if (m > maxDim) {
            val s = maxDim.toFloat() / m
            dec.setTargetSize(
                (info.size.width * s).toInt().coerceAtLeast(1),
                (info.size.height * s).toInt().coerceAtLeast(1),
            )
        }
    }
} catch (e: Exception) {
    null
}

fun Bitmap.toJpegBytes(quality: Int = 85): ByteArray {
    val o = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, o)
    return o.toByteArray()
}

fun saveBitmapToGallery(ctx: Context, bmp: Bitmap, name: String = "AIHub_${System.currentTimeMillis()}"): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AIHub")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
    ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    true
} catch (e: Exception) {
    false
}

fun saveBytesToDownloads(ctx: Context, fileName: String, mime: String, bytes: ByteArray): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, mime)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AIHub")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
    true
} catch (e: Exception) {
    false
}

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(ctx, "برنامه‌ای برای باز کردن لینک پیدا نشد")
    }
}

fun sendEmail(ctx: Context, to: String, subject: String, body: String = "") {
    try {
        val i = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
    } catch (e: Exception) {
        toast(ctx, "برنامه‌ی ایمیل پیدا نشد")
    }
}

/** حذف علامت‌های مارک‌داون برای خواندن با صدا */
fun plainForSpeech(s: String): String =
    s.replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("[*#`_>~]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
