package com.catclaw.aura.presentation.moment

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.catclaw.aura.R
import com.catclaw.aura.databinding.ItemMomentCardBinding
import com.catclaw.aura.domain.model.HomeListEntry
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.model.WorkflowPhase
import java.io.File
import java.text.DateFormat
import java.util.Date

class MomentCardListAdapter(
    private val onItemClick: (HomeListEntry) -> Unit,
    private val onItemLongClick: (HomeListEntry) -> Unit,
) : ListAdapter<HomeListEntry, MomentCardListAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMomentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMomentCardBinding,
        private val onItemClick: (HomeListEntry) -> Unit,
        private val onItemLongClick: (HomeListEntry) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeListEntry) {
            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
            binding.progressGenerating.isVisible = item is HomeListEntry.InProgress
            when (item) {
                is HomeListEntry.InProgress -> bindInProgress(item)
                is HomeListEntry.Completed -> bindCompleted(item)
            }
        }

        private fun bindInProgress(item: HomeListEntry.InProgress) {
            val workflow = item.workflow
            binding.textTime.text = formatTime(workflow.createdAtEpochMs)
            binding.textTitle.text = binding.root.context.getString(
                R.string.moment_list_generating_title,
                workflow.workflowId.take(8),
            )
            binding.textSubtitle.text = workflow.phase.label(binding.root.context)
            binding.textError.isVisible = workflow.errorMessage != null
            binding.textError.text = workflow.errorMessage
            loadPoster(workflow.posterPreviewPath)
        }

        private fun bindCompleted(item: HomeListEntry.Completed) {
            val card = item.card
            binding.textTime.text = formatTime(card.createdAtEpochMs)
            binding.textTitle.text = card.sceneDescription?.take(48)
                ?: binding.root.context.getString(R.string.moment_list_no_description)
            binding.textSubtitle.text = buildMetaLine(card)
            binding.textError.isVisible = !card.sceneDescriptionError.isNullOrBlank()
            binding.textError.text = card.sceneDescriptionError
            applyThemeColor(card.themeColor)
            loadPoster(card.posterPath)
        }

        private fun applyThemeColor(themeColor: String?) {
            val parsed = themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            if (parsed != null) {
                binding.imagePoster.setBackgroundColor(parsed)
            } else {
                binding.imagePoster.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        private fun loadPoster(path: String?) {
            if (path != null) {
                val uri = if (path.contains("://")) Uri.parse(path) else Uri.fromFile(File(path))
                binding.imagePoster.setImageURI(uri)
            } else {
                binding.imagePoster.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        private fun buildMetaLine(card: MomentCard): String {
            val parts = mutableListOf<String>()
            if (!card.musicTitle.isNullOrBlank() || !card.musicArtist.isNullOrBlank()) {
                parts.add(
                    listOfNotNull(card.musicTitle, card.musicArtist)
                        .joinToString(" — "),
                )
            }
            when {
                !card.locationPlaceName.isNullOrBlank() ->
                    parts.add(
                        binding.root.context.getString(
                            R.string.moment_list_location_place,
                            card.locationPlaceName,
                        ),
                    )
                card.latitude != null && card.longitude != null ->
                    parts.add(
                        binding.root.context.getString(
                            R.string.moment_list_location_short,
                            card.latitude,
                            card.longitude,
                        ),
                    )
            }
            return parts.joinToString(" · ").ifBlank {
                binding.root.context.getString(R.string.moment_list_meta_fallback)
            }
        }

        private fun formatTime(epochMs: Long): String =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(epochMs))
    }

    private object Diff : DiffUtil.ItemCallback<HomeListEntry>() {
        override fun areItemsTheSame(old: HomeListEntry, new: HomeListEntry): Boolean =
            old.id == new.id && old::class == new::class

        override fun areContentsTheSame(old: HomeListEntry, new: HomeListEntry): Boolean =
            old == new
    }
}
