package com.hasta.app.perception.tracker

/**
 * Supplies strictly increasing timestamps to MediaPipe live-stream mode.
 *
 * Camera timestamps and wall clocks can repeat or move backwards after lifecycle
 * events. Live-stream mode rejects non-increasing timestamps, so the clock clamps
 * to the previous value plus one millisecond.
 */
class FrameClock {
    private var last = Long.MIN_VALUE

    @Synchronized
    fun next(nowMs: Long): Long {
        val next = if (nowMs > last) nowMs else last + 1
        last = next
        return next
    }
}
