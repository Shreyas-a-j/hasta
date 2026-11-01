package com.hasta.app.data.landmarkdump

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DebugLandmarkDumpTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun frame(timestampMs: Long, hands: Int = 1): HandFrame {
        val observations = List(hands) { slot ->
            HandObservation(
                handedness = if (slot == 0) Handedness.RIGHT else Handedness.LEFT,
                handednessScore = 0.9f,
                landmarks = List(HandObservation.LANDMARK_COUNT) {
                    Landmark(x = 0.1f, y = 0.2f, z = 0.0f)
                },
                worldLandmarks = null,
            )
        }
        return HandFrame.create(
            timestampMs = timestampMs,
            imageWidth = 640,
            imageHeight = 480,
            rotationDegrees = 90,
            isFrontCamera = true,
            hands = observations,
        ) ?: error("sample frame must be valid")
    }

    private lateinit var dir: java.io.File

    @Before
    fun setUp() {
        dir = folder.newFolder("landmark_dumps")
    }

    @Test
    fun endSessionWritesOneLinePerFrame() {
        val dump = DebugLandmarkDump(dir, maxFrames = 100, maxFiles = 5)
        dump.onFrame(frame(1))
        dump.onFrame(frame(2))
        dump.onFrame(frame(3))
        dump.endSession()

        val files = dir.listFiles()!!.filter { it.name.endsWith(".jsonl") }
        assertEquals(1, files.size)
        val lines = files[0].readLines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("\"timestampMs\":1"))
        assertTrue(lines[2].contains("\"timestampMs\":3"))
    }

    @Test
    fun bufferStopsGrowingAtTheFrameCap() {
        val dump = DebugLandmarkDump(dir, maxFrames = 5, maxFiles = 5)
        repeat(20) { dump.onFrame(frame(it.toLong())) }
        dump.endSession()
        val lines = dir.listFiles()!!.single().readLines()
        assertEquals(5, lines.size)
    }

    @Test
    fun emptySessionWritesNoFile() {
        val dump = DebugLandmarkDump(dir, maxFrames = 10, maxFiles = 5)
        dump.endSession()
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun oldFilesArePrunedBeyondTheRetentionLimit() {
        repeat(7) { index ->
            val dump = DebugLandmarkDump(dir, maxFrames = 10, maxFiles = 3)
            dump.onFrame(frame(index.toLong()))
            dump.endSession()
        }
        val files = dir.listFiles()!!.filter { it.name.endsWith(".jsonl") }
        assertEquals(3, files.size)
    }

    @Test
    fun framesAfterCloseAreIgnored() {
        val dump = DebugLandmarkDump(dir, maxFrames = 10, maxFiles = 5)
        dump.onFrame(frame(1))
        dump.endSession()
        dump.onFrame(frame(2))
        dump.endSession()
        val files = dir.listFiles()!!.filter { it.name.endsWith(".jsonl") }
        assertEquals(1, files.size)
        assertEquals(1, files[0].readLines().size)
    }
}
