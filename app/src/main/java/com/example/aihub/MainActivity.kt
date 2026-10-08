package com.example.aihub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.aihub.core.Store
import com.example.aihub.screens.*
import com.example.aihub.ui.AIHubTheme
import com.example.aihub.ui.AppBackground
import com.example.aihub.ui.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            AIHubTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
                val back = { screen = Screen.HOME }
                AppBackground(showImage = screen != Screen.HOME) {
                    when (screen) {
                        Screen.HOME -> HomeScreen { screen = it }
                        Screen.CHAT -> ChatScreen(advanced = false, onBack = back)
                        Screen.ADV_CHAT -> ChatScreen(advanced = true, onBack = back)
                        Screen.VOICE_CHAT -> VoiceChatScreen(back)
                        Screen.VOICE_CHANGER -> VoiceChangerScreen(back)
                        Screen.IMAGE_EDIT -> ImageEditScreen(back)
                        Screen.ANIMATE -> AnimateScreen(back)
                        Screen.TRANSLATE -> TranslateScreen(back)
                        Screen.SONG -> SongScreen(back)
                        Screen.WEBSITE -> WebsiteScreen(back)
                        Screen.DRAW -> DrawScreen(back)
                        Screen.CHAR3D -> Char3DScreen(back)
                        Screen.TTS -> TtsScreen(back)
                        Screen.SPEECH_VIDEO -> SpeechVideoScreen(back)
                        Screen.WRITER -> WriterScreen(back)
                        Screen.SETTINGS -> SettingsScreen(back)
                        Screen.ABOUT -> AboutScreen(back)
                        Screen.CONTACT -> ContactScreen(back)
                    }
                }
            }
        }
    }
}
