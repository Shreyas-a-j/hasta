package com.hasta.app.data.landmarkdump

import com.hasta.app.domain.model.HandFrame

/**
 * Receives validated tracker frames for debug-only landmark capture (G12).
 *
 * The release build has no implementation: [LandmarkDumpFactory] returns null
 * and every call site no-ops, so M0 behaviour is unchanged.
 */
interface LandmarkDump {
    fun onFrame(frame: HandFrame)
    fun endSession()
}
