package com.savoo.scclient.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.User
import com.savoo.scclient.databinding.ItemArtistBinding

class ArtistAdapter(
    private val onArtistClick: (User) -> Unit,
) : ListAdapter<User, ArtistAdapter.ViewHolder>(ArtistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemArtistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: User) {
            binding.artistName.text = artist.username
            val followers = artist.followersCount ?: 0
            binding.artistFollowers.text = if (followers > 0) "$followers followers" else "Artist"

            binding.artistAvatar.load(artist.avatarUrl?.replace("-large", "-t300x300")) {
                crossfade(true)
                placeholder(R.drawable.logo_git)
                error(R.drawable.logo_git)
            }

            binding.root.setOnClickListener { onArtistClick(artist) }
        }
    }

    class ArtistDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}
