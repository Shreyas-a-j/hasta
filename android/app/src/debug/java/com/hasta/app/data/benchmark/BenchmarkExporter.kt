package com.hasta.app.data.benchmark

import android.content.Context
import java.io.File

data class BenchmarkFiles(
    val jsonFile: File,
    val csvFile: File,
)

/**
 * Writes a run into the app-private files directory so the documented `run-as`
 * command can read it without a storage permission or a shared-storage path.
 */
class BenchmarkExporter(private val context: Context) {

    fun export(baseName: String, json: String, csv: String): BenchmarkFiles {
        val directory = File(context.filesDir, DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            error("Could not create benchmark directory: ${directory.absolutePath}")
        }

        var unique = baseName
        var suffix = 0
        while (
            File(directory, "$unique.json").exists() ||
            File(directory, "$unique.csv").exists()
        ) {
            suffix++
            unique = "$baseName-$suffix"
        }

        val jsonFile = File(directory, "$unique.json")
        val csvFile = File(directory, "$unique.csv")
        jsonFile.writeText(json, Charsets.UTF_8)
        csvFile.writeText(csv, Charsets.UTF_8)
        return BenchmarkFiles(jsonFile = jsonFile, csvFile = csvFile)
    }

    private companion object {
        const val DIRECTORY = "benchmark"
    }
}
