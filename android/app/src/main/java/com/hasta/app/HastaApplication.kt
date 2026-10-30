package com.hasta.app

import android.app.Application
import android.content.Context
import com.hasta.app.data.benchmark.BenchmarkFactory
import com.hasta.app.data.benchmark.BenchmarkRecorder

/**
 * Manual dependency construction. There is no DI framework in M0.
 *
 * The benchmark recorder is null in release builds; see BenchmarkFactory.
 */
class AppContainer(context: Context) {
    val benchmarkRecorder: BenchmarkRecorder? = BenchmarkFactory.create(context)
}

class HastaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
