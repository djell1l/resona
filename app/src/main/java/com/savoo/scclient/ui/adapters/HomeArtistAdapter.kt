package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.FavoriteArtist
import com.savoo.scclient.databinding.ItemHomeArtistBinding

class HomeArtistAdapter(
    private val onArtistClick: (Long) -> Unit,
) : ListAdapter<FavoriteArtist, HomeArtistAdapter.ViewHolder>(ArtistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomeArtistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: FavoriteArtist) {
            binding.artistName.text = artist.username

            binding.artistAvatar.load(artist.avatarUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            binding.root.setOnClickListener { onArtistClick(artist.artistId) }
        }
    }

    class ArtistDiffCallback : DiffUtil.ItemCallback<FavoriteArtist>() {
        override fun areItemsTheSame(oldItem: FavoriteArtist, newItem: FavoriteArtist): Boolean =
            oldItem.artistId == newItem.artistId

        override fun areContentsTheSame(oldItem: FavoriteArtist, newItem: FavoriteArtist): Boolean =
            oldItem == newItem
    }
}
