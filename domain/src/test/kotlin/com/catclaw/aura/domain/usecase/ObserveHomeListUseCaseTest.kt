package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.ActiveWorkflow
import com.catclaw.aura.domain.model.HomeListEntry
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.model.WorkflowPhase
import com.catclaw.aura.domain.repository.MomentCardRepository
import com.catclaw.aura.domain.repository.MomentWorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHomeListUseCaseTest {

    @Test
    fun dedupesCardWhenStillActive() = runTest {
        val card = sampleCard("wf-1")
        val active = ActiveWorkflow("wf-1", WorkflowPhase.GENERATING_DESCRIPTION, 100L)
        val useCase = ObserveHomeListUseCase(
            FakeCardRepo(flowOf(listOf(card))),
            FakeWorkflowRepo(flowOf(listOf(active))),
        )
        val items = useCase().first()
        assertEquals(1, items.size)
        assertTrue(items[0] is HomeListEntry.InProgress)
    }

    @Test
    fun showsCompletedWhenNotActive() = runTest {
        val card = sampleCard("wf-1")
        val useCase = ObserveHomeListUseCase(
            FakeCardRepo(flowOf(listOf(card))),
            FakeWorkflowRepo(flowOf(emptyList())),
        )
        val items = useCase().first()
        assertEquals(1, items.size)
        assertTrue(items[0] is HomeListEntry.Completed)
    }

    private fun sampleCard(id: String) = MomentCard(
        id = id,
        createdAtEpochMs = 100L,
        posterPath = null,
        videoPath = null,
        audioPath = null,
        videoDurationMs = 0,
        audioDurationMs = 0,
        musicTitle = null,
        musicArtist = null,
        musicAlbum = null,
        musicActive = false,
        musicStatusMessage = "",
        musicPackageName = null,
        latitude = null,
        longitude = null,
        locationAccuracyMeters = null,
        locationProvider = null,
        locationPlaceName = null,
        sceneDescription = null,
        sceneDescriptionError = null,
        captureErrorSummary = null,
    )

    private class FakeCardRepo(private val flow: Flow<List<MomentCard>>) : MomentCardRepository {
        override fun observeCards(): Flow<List<MomentCard>> = flow
        override suspend fun getCard(id: String): MomentCard? = null
        override suspend fun save(card: MomentCard) = Unit
        override suspend fun delete(cardId: String) = Unit
    }

    private class FakeWorkflowRepo(private val flow: Flow<List<ActiveWorkflow>>) : MomentWorkflowRepository {
        override fun observeActiveWorkflows(): Flow<List<ActiveWorkflow>> = flow
        override fun enqueue(snapshot: com.catclaw.aura.domain.model.MomentCaptureSnapshot) = Unit
        override fun activeCount(): Int = 0
    }
}
