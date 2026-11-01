package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.databinding.ItemQuickPickBinding

class QuickPickAdapter(
    private val onTrackClick: (Track) -> Unit,
) : ListAdapter<Track, QuickPickAdapter.ViewHolder>(QuickPickDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemQuickPickBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track) {
            binding.itemTitle.text = track.title
            binding.itemSubtitle.text = track.user.username

            binding.itemArt.load(track.artworkUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            binding.root.setOnClickListener { onTrackClick(track) }
        }
    }

    class QuickPickDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem == newItem
    }
}
