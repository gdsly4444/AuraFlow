package com.catclaw.aura.data.scenedescription

import android.app.Application
import com.catclaw.aura.BuildConfig
import com.catclaw.aura.data.ambient.model.AmbientMoment
import com.catclaw.aura.data.scenedescription.config.SceneDescriptionConfig
import com.catclaw.aura.data.scenedescription.model.SceneDescription
import com.catclaw.aura.data.scenedescription.organization.AmbientMomentPayloadMapper
import com.catclaw.aura.data.scenedescription.organization.SceneMediaEncoder
import com.catclaw.aura.data.scenedescription.prompt.SceneDescriptionPromptBuilder
import com.catclaw.aura.data.scenedescription.remote.DashScopeSceneDescriptionRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SceneDescriptionRepository(
    application: Application,
    private val config: SceneDescriptionConfig = SceneDescriptionConfig(),
) {
    private val encoder = SceneMediaEncoder(application.contentResolver, config)
    private val promptBuilder = SceneDescriptionPromptBuilder(config)
    private val remote = DashScopeSceneDescriptionRemote()

    suspend fun generateFrom(moment: AmbientMoment): SceneDescription = withContext(Dispatchers.IO) {
        if (BuildConfig.DASHSCOPE_API_KEY.isBlank()) {
            return@withContext SceneDescription(
                errorMessage = "未配置 DASHSCOPE_API_KEY，请在 local.properties 中设置",
            )
        }
        val payload = AmbientMomentPayloadMapper.map(moment)
        val media = encoder.encode(payload)
        val body = promptBuilder.buildRequestJson(payload, media)
        remote.generate(body, BuildConfig.DASHSCOPE_API_KEY)
    }
}
