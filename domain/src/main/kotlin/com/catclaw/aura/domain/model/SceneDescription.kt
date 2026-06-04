package com.catclaw.aura.domain.model

data class SceneDescription(
    val text: String? = null,
    val model: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = !text.isNullOrBlank() && errorMessage == null
}
