package com.catclaw.aura.ui.moment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.catclaw.aura.R
import com.catclaw.aura.databinding.ItemMomentCardBinding
import com.catclaw.aura.service.moment.WorkflowPhase
import java.io.File
import java.text.DateFormat
import java.util.Date

class MomentCardListAdapter(
    private val onItemClick: (MomentListItem) -> Unit,
    private val onItemLongClick: (MomentListItem) -> Unit,
) : ListAdapter<MomentListItem, MomentCardListAdapter.ViewHolder>(Diff) {

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
        private val onItemClick: (MomentListItem) -> Unit,
        private val onItemLongClick: (MomentListItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MomentListItem) {
            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
            binding.progressGenerating.isVisible = item is MomentListItem.InProgress
            when (item) {
                is MomentListItem.InProgress -> bindInProgress(item)
                is MomentListItem.Completed -> bindCompleted(item)
            }
        }

        private fun bindInProgress(item: MomentListItem.InProgress) {
            val workflow = item.workflow
            binding.textTime.text = formatTime(workflow.createdAtEpochMs)
            binding.textTitle.text = binding.root.context.getString(
                R.string.moment_list_generating_title,
                workflow.workflowId.take(8),
            )
            binding.textSubtitle.text = phaseLabel(workflow.phase)
            binding.textError.isVisible = workflow.errorMessage != null
            binding.textError.text = workflow.errorMessage
            loadPoster(workflow.posterPreviewPath)
        }

        private fun bindCompleted(item: MomentListItem.Completed) {
            val card = item.card
            binding.textTime.text = formatTime(card.createdAtEpochMs)
            binding.textTitle.text = card.sceneDescription?.take(48)
                ?: binding.root.context.getString(R.string.moment_list_no_description)
            binding.textSubtitle.text = buildMetaLine(card)
            binding.textError.isVisible = !card.sceneDescriptionError.isNullOrBlank()
            binding.textError.text = card.sceneDescriptionError
            loadPoster(card.posterPath)
        }

        private fun loadPoster(path: String?) {
            if (path != null) {
                binding.imagePoster.setImageURI(android.net.Uri.fromFile(File(path)))
            } else {
                binding.imagePoster.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        private fun phaseLabel(phase: WorkflowPhase): String =
            phase.label(binding.root.context)

        private fun buildMetaLine(card: com.catclaw.aura.data.moment.model.MomentCard): String {
            val parts = mutableListOf<String>()
            if (!card.musicTitle.isNullOrBlank() || !card.musicArtist.isNullOrBlank()) {
                parts.add(
                    listOfNotNull(card.musicTitle, card.musicArtist)
                        .joinToString(" — "),
                )
            }
            if (card.latitude != null && card.longitude != null) {
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

    private object Diff : DiffUtil.ItemCallback<MomentListItem>() {
        override fun areItemsTheSame(old: MomentListItem, new: MomentListItem): Boolean =
            old.id == new.id && old::class == new::class

        override fun areContentsTheSame(old: MomentListItem, new: MomentListItem): Boolean =
            old == new
    }
}
