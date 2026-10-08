package com.example.aihub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.core.Store
import com.example.aihub.ui.BannerBox
import com.example.aihub.ui.ResImage
import com.example.aihub.ui.Screen
import com.example.aihub.ui.rememberAsset

private val origins = listOf(
    TransformOrigin(0.15f, 0.2f), TransformOrigin(0.5f, 0.5f), TransformOrigin(0.85f, 0.8f),
    TransformOrigin(0.85f, 0.2f), TransformOrigin(0.15f, 0.8f), TransformOrigin(0.5f, 0.15f),
)

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val c = MaterialTheme.colorScheme
    val robot = rememberAsset("page_bg")
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        BannerBox(height = 168.dp) { top ->
            IconButton(
                onClick = { onOpen(Screen.SETTINGS) },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = top, end = 8.dp),
            ) { Icon(Icons.Rounded.Settings, "تنظیمات", tint = Color.White) }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResImage("logo", Modifier.size(64.dp).clip(CircleShape))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("هوش‌یار", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    Text(
                        "همه‌ی ابزارهای هوش مصنوعی در یک اپ",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp,
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (Store.needsKey) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        onClick = { onOpen(Screen.SETTINGS) },
                        shape = RoundedCornerShape(18.dp),
                        color = c.primary.copy(alpha = 0.14f),
                    ) {
                        Text(
                            "برای شروع، یک کلید رایگان (مثلاً Groq) در تنظیمات وارد کن ←",
                            Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.5.sp,
                        )
                    }
                }
            }
            itemsIndexed(Screen.tools) { i, s ->
                val o = origins[i % origins.size]
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onOpen(s) }
                ) {
                    // عکس ربات در پس‌زمینه‌ی همه‌ی کارت‌ها (هر کارت یک برش متفاوت)
                    if (robot != null) {
                        Image(
                            robot, null,
                            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.7f; scaleY = 1.7f; transformOrigin = o },
                            contentScale = ContentScale.Crop,
                        )
                    }
                    val a1 = if (robot != null) 0.86f else 1f
                    val a2 = if (robot != null) 0.78f else 1f
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(s.c1.copy(alpha = a1), s.c2.copy(alpha = a2)))
                        )
                    )
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            Modifier.size(44.dp).background(Color.White.copy(alpha = 0.24f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Column {
                            Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            Text(
                                s.sub, color = Color.White.copy(alpha = 0.88f), fontSize = 11.5.sp,
                                lineHeight = 15.sp, maxLines = 2,
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Screen.ABOUT, Screen.CONTACT).forEach { s ->
                        Surface(
                            onClick = { onOpen(s) },
                            shape = RoundedCornerShape(18.dp),
                            color = c.surfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(s.icon, null, tint = c.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(s.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
