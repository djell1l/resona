package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.FavoritePlaylist
import com.savoo.scclient.databinding.ItemHomePlaylistBinding

class HomePlaylistAdapter(
    private val onPlaylistClick: (Long) -> Unit,
) : ListAdapter<FavoritePlaylist, HomePlaylistAdapter.ViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomePlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomePlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: FavoritePlaylist) {
            binding.playlistTitle.text = playlist.title
            binding.playlistSubtitle.text = if (playlist.username.isNotBlank()) playlist.username else "Playlist"

            binding.playlistArt.load(playlist.artworkUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            binding.root.setOnClickListener { onPlaylistClick(playlist.playlistId) }
        }
    }

    class PlaylistDiffCallback : DiffUtil.ItemCallback<FavoritePlaylist>() {
        override fun areItemsTheSame(oldItem: FavoritePlaylist, newItem: FavoritePlaylist): Boolean =
            oldItem.playlistId == newItem.playlistId

        override fun areContentsTheSame(oldItem: FavoritePlaylist, newItem: FavoritePlaylist): Boolean =
            oldItem == newItem
    }
}
