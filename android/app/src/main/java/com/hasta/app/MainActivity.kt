package com.hasta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hasta.app.ui.CameraScreen
import com.hasta.app.ui.theme.HastaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Null in release builds; the benchmark implementation lives in the debug
        // source set only.
        val benchmark = (application as HastaApplication).container.benchmarkRecorder

        setContent {
            HastaTheme {
                CameraScreen(benchmark = benchmark)
            }
        }
    }
}
