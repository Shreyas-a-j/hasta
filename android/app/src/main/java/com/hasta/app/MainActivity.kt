package com.hasta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CameraScreen(benchmark = benchmark)
                }
            }
        }
    }
}
