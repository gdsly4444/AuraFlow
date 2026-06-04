package com.catclaw.aura.domain.repository

import com.catclaw.aura.domain.model.AmbientMoment

/**
 * Platform-agnostic capture entry point. Implementation binds camera UI in the data layer
 * before [capture] is invoked from presentation.
 */
interface AmbientCapturePort {
    suspend fun capture(): AmbientMoment
}
