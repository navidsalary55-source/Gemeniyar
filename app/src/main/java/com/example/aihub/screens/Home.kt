package com.example.aihub.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aihub.core.Config
import com.example.aihub.core.Store
import com.example.aihub.ui.BannerBox
import com.example.aihub.ui.ResImage
import com.example.aihub.ui.Screen
import com.example.aihub.ui.rememberAsset
import kotlinx.coroutines.launch

private val origins = listOf(
    TransformOrigin(0.15f, 0.2f), TransformOrigin(0.5f, 0.5f), TransformOrigin(0.85f, 0.8f),
    TransformOrigin(0.85f, 0.2f), TransformOrigin(0.15f, 0.8f), TransformOrigin(0.5f, 0.15f),
)

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val c = MaterialTheme.colorScheme
    val robot = rememberAsset("page_bg")
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    ResImage("hero_bg", Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0x990B1020), Color(0xE60B1020)))
                        )
                    )
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResImage("logo", Modifier.size(56.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Config.APP_NAME, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text("سازنده: ${Config.OWNER}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Screen.drawerItems.forEach { s ->
                    NavigationDrawerItem(
                        label = { Text(s.title, fontWeight = FontWeight.Medium) },
                        icon = { Icon(s.icon, null) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; onOpen(s) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            BannerBox(height = 168.dp) { top ->
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopStart).padding(top = top, start = 8.dp),
                ) { Icon(Icons.Rounded.Menu, "منو", tint = Color.White) }
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
                    Column(Modifier.weight(1f)) {
                        Text(Config.APP_NAME, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
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
                        if (robot != null) {
                            Image(
                                robot, null,
                                Modifier.fillMaxSize().graphicsLayer { scaleX = 1.7f; scaleY = 1.7f; transformOrigin = o },
                                contentScale = ContentScale.Crop,
                            )
                        }
                        // لایه‌ی رنگی شفاف‌تر تا عکس ربات واضح‌تر دیده شود
                        val a1 = if (robot != null) 0.28f else 1f
                        val a2 = if (robot != null) 0.18f else 1f
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(listOf(s.c1.copy(alpha = a1), s.c2.copy(alpha = a2)))
                            )
                        )
                        // سایه‌ی ملایم پایین کارت برای خوانایی متن
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color(0x14000000), Color(0x99000000)))
                            )
                        )
                        Column(
                            Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier.size(44.dp).background(Color.White.copy(alpha = 0.26f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                            Column {
                                Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xCC000000), blurRadius = 8f)))
                                Text(
                                    s.sub, color = Color.White.copy(alpha = 0.92f), fontSize = 11.5.sp,
                                    lineHeight = 15.sp, maxLines = 2, style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xCC000000), blurRadius = 8f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
