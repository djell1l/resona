package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.databinding.ItemHomeTrackBinding

class HomeTrackAdapter(
    private val onTrackClick: (Track) -> Unit,
) : ListAdapter<Track, HomeTrackAdapter.ViewHolder>(TrackDiffCallback()) {

    var currentPlayingTrackId: Long? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomeTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track) {
            binding.trackTitle.text = track.title
            binding.trackArtist.text = track.user.username

            val isPlaying = track.id == currentPlayingTrackId
            binding.trackPlayingOverlay.visibility = if (isPlaying) View.VISIBLE else View.GONE
            binding.trackPlayingIcon.visibility = if (isPlaying) View.VISIBLE else View.GONE

            binding.trackArt.load(track.artworkUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            binding.root.setOnClickListener { onTrackClick(track) }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem == newItem
    }
}
