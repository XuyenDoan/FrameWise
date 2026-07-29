package com.framewise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.framewise.app.navigation.FrameWiseNavHost
import com.framewise.core.designsystem.theme.FrameWiseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FrameWiseTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FrameWiseNavHost()
                }
            }
        }
    }
}
