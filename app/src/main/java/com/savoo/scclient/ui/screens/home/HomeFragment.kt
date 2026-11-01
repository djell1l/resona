package com.savoo.scclient.ui.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.savoo.scclient.R
import com.savoo.scclient.databinding.FragmentHomeBinding
import com.savoo.scclient.ui.adapters.HomeArtistAdapter
import com.savoo.scclient.ui.adapters.HomePlaylistAdapter
import com.savoo.scclient.ui.adapters.HomeTrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private lateinit var jumpBackInAdapter: HomeTrackAdapter
    private lateinit var favoritesAdapter: HomeTrackAdapter
    private lateinit var artistsAdapter: HomeArtistAdapter
    private lateinit var playlistsAdapter: HomePlaylistAdapter
    private lateinit var offlineAdapter: HomeTrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbarMenu()
        setupGreeting()
        setupAdapters()
        setupListeners()
        observeData()
    }

    private fun setupToolbarMenu() {
        binding.toolbarLayout.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbarLayout.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_charts -> {
                    findNavController().navigate(R.id.nav_charts)
                    true
                }
                R.id.action_history -> {
                    findNavController().navigate(R.id.nav_history)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.nav_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        binding.homeGreeting.text = getString(greetingRes)
    }

    private fun setupAdapters() {
        jumpBackInAdapter = HomeTrackAdapter { track ->
            val recent = viewModel.recentTracks.value
            if (viewModel.playerController.state.value.currentTrack?.id == track.id) {
                viewModel.playerController.togglePlayPause()
            } else {
                viewModel.playFrom(recent, track.id, tag = "recent")
            }
        }
        binding.rvJumpBackIn.adapter = jumpBackInAdapter

        favoritesAdapter = HomeTrackAdapter { track ->
            if (viewModel.playerController.state.value.currentTrack?.id == track.id) {
                viewModel.playerController.togglePlayPause()
            } else {
                viewModel.playFavoritesFrom(track)
            }
        }
        binding.rvFavorites.adapter = favoritesAdapter

        artistsAdapter = HomeArtistAdapter { artistId ->
            val bundle = Bundle().apply { putLong("userId", artistId) }
            findNavController().navigate(R.id.nav_artist, bundle)
        }
        binding.rvArtists.adapter = artistsAdapter

        playlistsAdapter = HomePlaylistAdapter { playlistId ->
            val bundle = Bundle().apply { putLong("playlistId", playlistId) }
            findNavController().navigate(R.id.nav_playlist, bundle)
        }
        binding.rvPlaylists.adapter = playlistsAdapter

        offlineAdapter = HomeTrackAdapter { track ->
            val offline = viewModel.offlineTracks.value
            if (viewModel.playerController.state.value.currentTrack?.id == track.id) {
                viewModel.playerController.togglePlayPause()
            } else {
                viewModel.playFrom(offline, track.id, tag = "offline")
            }
        }
        binding.rvOffline.adapter = offlineAdapter
    }

    private fun setupListeners() {
        val playMixAction = {
            val isMixQueueActive = viewModel.playerController.state.value.queueTag == HomeViewModel.MIX_QUEUE_TAG
            if (isMixQueueActive) {
                viewModel.playerController.togglePlayPause()
            } else {
                val mix = viewModel.mixTracks.value
                if (mix.isNotEmpty()) {
                    viewModel.playMix(mix)
                }
            }
        }

        binding.btnPlayMix.setOnClickListener { playMixAction() }
        binding.mixCollageContainer.setOnClickListener { playMixAction() }

        binding.imgUserAvatar.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }

        binding.headerJumpBackIn.setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }

        binding.headerFavorites.setOnClickListener {
            findNavController().navigate(R.id.nav_favorites)
        }

        binding.headerArtists.setOnClickListener {
            findNavController().navigate(R.id.nav_favorites)
        }

        binding.headerPlaylists.setOnClickListener {
            findNavController().navigate(R.id.nav_favorites)
        }

        binding.headerOffline.setOnClickListener {
            findNavController().navigate(R.id.nav_offline)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collectLatest { user ->
                        if (user != null) {
                            binding.homeUsername.text = user.username
                            binding.imgUserAvatar.load(user.avatarUrl?.replace("-large", "-t300x300")) {
                                crossfade(true)
                                placeholder(R.drawable.logo_git)
                                error(R.drawable.logo_git)
                            }
                        } else {
                            binding.homeUsername.text = getString(R.string.onboarding_welcome_title)
                            binding.imgUserAvatar.setImageResource(R.drawable.logo_git)
                        }
                    }
                }

                launch {
                    viewModel.mixTracks.collectLatest { tracks ->
                        if (tracks.isNotEmpty()) {
                            binding.imgMixCenter.load(tracks[0].artworkUrl?.replace("-large", "-t500x500")) {
                                crossfade(true)
                                placeholder(R.drawable.logo_git)
                                error(R.drawable.logo_git)
                            }
                            if (tracks.size > 1) {
                                binding.imgMixCorner1.visibility = View.VISIBLE
                                binding.imgMixCorner1.load(tracks[1].artworkUrl?.replace("-large", "-t300x300")) {
                                    crossfade(true)
                                    placeholder(R.drawable.logo_git)
                                    error(R.drawable.logo_git)
                                }
                            } else {
                                binding.imgMixCorner1.visibility = View.INVISIBLE
                            }
                            if (tracks.size > 2) {
                                binding.imgMixCorner2.visibility = View.VISIBLE
                                binding.imgMixCorner2.load(tracks[2].artworkUrl?.replace("-large", "-t300x300")) {
                                    crossfade(true)
                                    placeholder(R.drawable.logo_git)
                                    error(R.drawable.logo_git)
                                }
                            } else {
                                binding.imgMixCorner2.visibility = View.INVISIBLE
                            }

                            val artists = tracks.map { it.user.username }.filter { it.isNotBlank() }.distinct().take(3)
                            binding.mixSubtitle.text = if (artists.isNotEmpty()) {
                                artists.joinToString(", ")
                            } else {
                                getString(R.string.home_mix_subtitle_count, tracks.size)
                            }
                        } else {
                            binding.imgMixCenter.setImageResource(R.drawable.logo_git)
                            binding.imgMixCorner1.visibility = View.INVISIBLE
                            binding.imgMixCorner2.visibility = View.INVISIBLE
                            binding.mixSubtitle.setText(R.string.home_mix_empty_desc)
                        }
                    }
                }

                launch {
                    viewModel.isMixLoading.collectLatest { loading ->
                        binding.homeLoading.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.recentTracks.collectLatest { tracks ->
                        jumpBackInAdapter.submitList(tracks.take(12))
                        binding.sectionJumpBackIn.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.favoriteTracks.collectLatest { tracks ->
                        favoritesAdapter.submitList(tracks.take(12))
                        binding.sectionFavorites.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.favoriteArtists.collectLatest { artists ->
                        artistsAdapter.submitList(artists.take(12))
                        binding.sectionArtists.visibility = if (artists.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.favoritePlaylists.collectLatest { playlists ->
                        playlistsAdapter.submitList(playlists.take(12))
                        binding.sectionPlaylists.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.offlineTracks.collectLatest { tracks ->
                        offlineAdapter.submitList(tracks.take(12))
                        binding.sectionOffline.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.playerController.state.collectLatest { state ->
                        val isMixQueueActive = state.queueTag == HomeViewModel.MIX_QUEUE_TAG
                        val isMixPlaying = state.isPlaying && isMixQueueActive
                        binding.btnPlayMix.setImageResource(
                            if (isMixPlaying) R.drawable.ic_oui_pause else R.drawable.ic_oui_play
                        )
                        jumpBackInAdapter.currentPlayingTrackId = state.currentTrack?.id
                        favoritesAdapter.currentPlayingTrackId = state.currentTrack?.id
                        offlineAdapter.currentPlayingTrackId = state.currentTrack?.id
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
