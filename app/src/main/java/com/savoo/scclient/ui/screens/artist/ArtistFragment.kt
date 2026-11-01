package com.savoo.scclient.ui.screens.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.data.repository.TrackRepository
import com.savoo.scclient.databinding.FragmentArtistBinding
import com.savoo.scclient.player.PlayerController
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArtistFragment : Fragment() {

    private var _binding: FragmentArtistBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var trackRepository: TrackRepository
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var favoritesRepository: com.savoo.scclient.data.repository.FavoritesRepository

    private lateinit var trackAdapter: TrackAdapter
    private var userId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong("userId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                playerController.play(track)
            },
            onLikeClick = { track ->
                viewLifecycleOwner.lifecycleScope.launch {
                    favoritesRepository.toggleTrackFavorite(track)
                }
            }
        )
        binding.rvArtistTracks.adapter = trackAdapter

        loadArtist()
    }

    private fun loadArtist() {
        if (userId == 0L) return
        binding.artistLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val user = trackRepository.getUser(userId)
                val tracks = trackRepository.getArtistTracks(userId)
                binding.artistName.text = user.username
                binding.artistFollowersCount.text = "${user.followersCount ?: 0} followers"
                binding.artistAvatar.load(user.avatarUrl?.replace("-large", "-t500x500")) {
                    crossfade(true)
                    placeholder(R.drawable.logo_git)
                }
                trackAdapter.submitList(tracks)
            }
            binding.artistLoading.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
