package com.hasta.app.domain.features

/**
 * Constants from the locked features-v1 contract.
 *
 * These values must match `contracts/feature_contract.json` and
 * `docs/spec/features-v1.md`. Changing them requires a new contract version.
 */
object FeatureContract {
    const val DIMENSION = 256

    const val PALM_SCALE_EPSILON = 1e-6
    const val VECTOR_EPSILON = 1e-9
    const val GAP_THRESHOLD_MS = 600L

    const val ATOL = 1e-6
    const val RTOL = 1e-5

    const val HEADER_OFFSET = 0
    const val VALIDITY_OFFSET = 16
    const val LEFT_LOCAL_OFFSET = 58
    const val LEFT_GLOBAL_OFFSET = 139
    const val LEFT_TEMPORAL_OFFSET = 146
    const val RIGHT_LOCAL_OFFSET = 154
    const val RIGHT_GLOBAL_OFFSET = 235
    const val RIGHT_TEMPORAL_OFFSET = 242
    const val INTER_HAND_OFFSET = 250

    const val LANDMARK_COUNT = 21
    const val VALIDITY_LENGTH = 42
}
