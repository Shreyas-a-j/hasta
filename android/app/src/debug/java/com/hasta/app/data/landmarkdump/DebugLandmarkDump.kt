package com.hasta.app.data.landmarkdump

import android.content.Context
import com.hasta.app.domain.model.HandFrame
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug-only landmark dump for on-device real-landmark capture (G12).
 *
 * Constructed reflectively by [LandmarkDumpFactory], so no part of this class
 * ships in a release build. Frames are buffered in memory and written as one
 * JSON Lines file when the session ends; the buffer stops growing at [maxFrames]
 * and all but the newest [maxFiles] files are pruned, so a forgotten session
 * cannot fill device storage. One dump covers one pipeline session: after
 * [endSession] the instance ignores further frames.
 */
class DebugLandmarkDump @JvmOverloads constructor(
    private val dir: File,
    private val maxFrames: Int = 900,
    private val maxFiles: Int = 5,
) : LandmarkDump {

    constructor(context: Context) : this(File(context.filesDir, "landmark_dumps"))

    private val buffer = ArrayList<String>(64)
    private var closed = false

    @Synchronized
    override fun onFrame(frame: HandFrame) {
        if (closed || buffer.size >= maxFrames) return
        buffer += HandFrameJsonl.encode(frame)
    }

    @Synchronized
    override fun endSession() {
        if (closed) return
        closed = true
        if (buffer.isEmpty()) return
        dir.mkdirs()
        val file = File(
            dir,
            "landmarks-${System.currentTimeMillis()}-${fileCounter.incrementAndGet()}.jsonl",
        )
        file.writeText(buffer.joinToString(separator = "\n") + "\n")
        buffer.clear()
        prune()
    }

    private fun prune() {
        val files = dir.listFiles { entry -> entry.isFile && entry.name.endsWith(".jsonl") } ?: return
        files.sortedByDescending { it.lastModified() }
            .drop(maxFiles)
            .forEach { it.delete() }
    }

    private companion object {
        val fileCounter = AtomicLong()
    }
}
