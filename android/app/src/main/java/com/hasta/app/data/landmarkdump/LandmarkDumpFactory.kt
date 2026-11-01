package com.hasta.app.data.landmarkdump

import android.content.Context

/**
 * Creates the debug landmark dump without linking it into release builds.
 *
 * The release APK has no `DebugLandmarkDump` class, so [Class.forName] fails and
 * the app receives null. Mirrors [com.hasta.app.data.benchmark.BenchmarkFactory]:
 * debug capture code never ships in a release build.
 */
object LandmarkDumpFactory {

    private const val IMPLEMENTATION = "com.hasta.app.data.landmarkdump.DebugLandmarkDump"

    fun create(context: Context): LandmarkDump? = runCatching {
        val type = Class.forName(IMPLEMENTATION)
        type.getConstructor(Context::class.java)
            .newInstance(context.applicationContext) as LandmarkDump
    }.getOrNull()
}
