package com.hasta.app.data.benchmark

import android.content.Context

/**
 * Creates the debug benchmark implementation without linking it into release builds.
 *
 * The release APK has no `DebugBenchmarkRecorder` class, so [Class.forName] fails and
 * the app receives null. That keeps all sampling and export code out of release while
 * still allowing ordinary compile-time types in shared UI code.
 */
object BenchmarkFactory {

    private const val IMPLEMENTATION = "com.hasta.app.data.benchmark.DebugBenchmarkRecorder"

    fun create(context: Context): BenchmarkRecorder? = runCatching {
        val type = Class.forName(IMPLEMENTATION)
        type.getConstructor(Context::class.java)
            .newInstance(context.applicationContext) as BenchmarkRecorder
    }.getOrNull()
}
