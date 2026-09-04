package com.example.video_basedunique_personcollage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.video_basedunique_personcollage.ui.MainViewModel
import com.example.video_basedunique_personcollage.ui.screens.HomeScreen
import com.example.video_basedunique_personcollage.ui.theme.VideobasedUniquepersonCollageTheme
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideobasedUniquepersonCollageTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val viewModel: MainViewModel = viewModel(
                            factory = MainViewModel.provideFactory(this@MainActivity)
                        )
                        HomeScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
