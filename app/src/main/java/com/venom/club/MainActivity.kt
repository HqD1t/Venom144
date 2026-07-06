package com.venom.club

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.venom.club.navigation.VenomNavGraph
import com.venom.club.ui.theme.VenomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VenomTheme {
                VenomNavGraph(activity = this)
            }
        }
    }
}
