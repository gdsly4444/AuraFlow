package com.catclaw.aura.data.scenedescription.model

/**
 * Result of a scene description generation attempt.
 */
data class SceneDescription(
    val text: String? = null,
    val model: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = !text.isNullOrBlank() && errorMessage == null
}
