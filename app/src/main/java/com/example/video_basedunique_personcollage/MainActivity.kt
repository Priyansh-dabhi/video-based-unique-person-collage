package com.example.video_basedunique_personcollage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.video_basedunique_personcollage.ui.MainViewModel
import com.example.video_basedunique_personcollage.ui.screens.HomeScreen
import com.example.video_basedunique_personcollage.ui.theme.VideobasedUniquepersonCollageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideobasedUniquepersonCollageTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.provideFactory(this@MainActivity)
                )
                HomeScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
