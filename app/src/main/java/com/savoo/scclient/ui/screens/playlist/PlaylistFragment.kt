package com.savoo.scclient.ui.screens.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.data.repository.PlaylistsRepository
import com.savoo.scclient.databinding.FragmentPlaylistBinding
import com.savoo.scclient.player.PlayerController
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var playlistsRepository: PlaylistsRepository
    @Inject lateinit var trackRepository: com.savoo.scclient.data.repository.TrackRepository
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var favoritesRepository: com.savoo.scclient.data.repository.FavoritesRepository

    private lateinit var trackAdapter: TrackAdapter
    private var playlistId: Long = 0L
    private var playlistTracks: List<Track> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getLong("playlistId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                val index = playlistTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                playerController.playQueue(playlistTracks, index, tag = "playlist_$playlistId")
            }
        )
        binding.rvPlaylistTracks.adapter = trackAdapter

        binding.btnPlaylistPlay.setOnClickListener {
            if (playlistTracks.isNotEmpty()) {
                playerController.playQueue(playlistTracks, 0, tag = "playlist_$playlistId")
            }
        }

        binding.btnPlaylistShuffle.setOnClickListener {
            if (playlistTracks.isNotEmpty()) {
                playerController.playQueue(playlistTracks.shuffled(), 0, repeatAll = true, tag = "playlist_shuffle_$playlistId")
            }
        }

        loadPlaylist()
    }

    private fun loadPlaylist() {
        if (playlistId == 0L) return
        binding.playlistLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val playlist = trackRepository.getPlaylist(playlistId)
                binding.playlistTitle.text = playlist.title
                binding.playlistCreator.text = "By ${playlist.user.username} • ${playlist.trackCount} tracks"
                binding.playlistArtwork.load(playlist.artworkUrl?.replace("-large", "-t500x500")) {
                    crossfade(true)
                    placeholder(R.drawable.logo_git)
                }
                playlistTracks = playlist.tracks.orEmpty()
                trackAdapter.submitList(playlistTracks)
            }
            binding.playlistLoading.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
