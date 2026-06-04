package com.catclaw.aura.data.scenedescription.model

enum class EncodedMediaKind {
    POSTER,
    VIDEO,
    AUDIO,
}

data class EncodedSceneMedia(
    val kind: EncodedMediaKind,
    val mimeType: String,
    val dataUri: String,
    val skippedReason: String? = null,
) {
    /** DashScope OpenAI-compatible payload (see Alibaba Qwen-Omni base64 examples). */
    fun dashScopePayload(): String {
        val base64 = dataUri.substringAfter("base64,", dataUri)
        return when (kind) {
            EncodedMediaKind.POSTER -> "data:$mimeType;base64,$base64"
            EncodedMediaKind.VIDEO,
            EncodedMediaKind.AUDIO,
            -> "data:;base64,$base64"
        }
    }
}

/**
 * Only [selected] is attached to the API request (Omni allows one non-text modality).
 */
data class EncodedSceneMediaSet(
    val selected: EncodedSceneMedia? = null,
    val skippedNotes: List<String> = emptyList(),
)
