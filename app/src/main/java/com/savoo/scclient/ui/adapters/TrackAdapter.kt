package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.savoo.scclient.R
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.databinding.ItemTrackBinding
import java.util.Locale

class TrackAdapter(
    private val onTrackClick: (Track) -> Unit,
    private val onLikeClick: ((Track) -> Unit)? = null,
    private val onMoreClick: ((Track, View) -> Unit)? = null,
) : ListAdapter<Track, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    var currentPlayingTrackId: Long? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var favoriteTrackIds: Set<Long> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(private val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track) {
            binding.trackTitle.text = track.title
            val durationStr = formatDuration(track.durationMs)
            binding.trackArtist.text = "${track.user.username} • $durationStr"

            // Active playing highlight
            val isPlaying = track.id == currentPlayingTrackId
            if (isPlaying) {
                binding.trackTitle.setTextColor(binding.root.context.getColor(R.color.sesl_primary_color_light))
            } else {
                binding.trackTitle.setTextColor(binding.root.context.getColor(android.R.color.tab_indicator_text))
            }

            // Artwork
            binding.trackArtwork.load(track.artworkUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            // Favorite state
            val isFavorite = favoriteTrackIds.contains(track.id)
            binding.btnTrackLike.setImageResource(
                if (isFavorite) R.drawable.ic_oui_heart else R.drawable.ic_oui_heart_outline
            )

            binding.root.setOnClickListener { onTrackClick(track) }
            binding.btnTrackLike.setOnClickListener { onLikeClick?.invoke(track) }
            binding.btnTrackMore.setOnClickListener { v -> onMoreClick?.invoke(track, v) }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem == newItem
    }
}
