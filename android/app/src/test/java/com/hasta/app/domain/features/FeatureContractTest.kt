package com.hasta.app.domain.features

import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureContractTest {

    @Test
    fun `dimension and group offsets match the locked contract`() {
        assertEquals(256, FeatureContract.DIMENSION)
        assertEquals(0, FeatureContract.HEADER_OFFSET)
        assertEquals(16, FeatureContract.VALIDITY_OFFSET)
        assertEquals(58, FeatureContract.LEFT_LOCAL_OFFSET)
        assertEquals(139, FeatureContract.LEFT_GLOBAL_OFFSET)
        assertEquals(146, FeatureContract.LEFT_TEMPORAL_OFFSET)
        assertEquals(154, FeatureContract.RIGHT_LOCAL_OFFSET)
        assertEquals(235, FeatureContract.RIGHT_GLOBAL_OFFSET)
        assertEquals(242, FeatureContract.RIGHT_TEMPORAL_OFFSET)
        assertEquals(250, FeatureContract.INTER_HAND_OFFSET)

        assertEquals(
            FeatureContract.DIMENSION,
            FeatureContract.INTER_HAND_OFFSET + 6,
        )
        assertEquals(
            FeatureContract.VALIDITY_LENGTH,
            FeatureContract.LANDMARK_COUNT * 2,
        )
    }

    @Test
    fun `tolerances and constants are the documented values`() {
        assertEquals(1e-6, FeatureContract.PALM_SCALE_EPSILON, 0.0)
        assertEquals(1e-9, FeatureContract.VECTOR_EPSILON, 0.0)
        assertEquals(600L, FeatureContract.GAP_THRESHOLD_MS)
        assertEquals(1e-6, FeatureContract.ATOL, 0.0)
        assertEquals(1e-5, FeatureContract.RTOL, 0.0)
    }
}
