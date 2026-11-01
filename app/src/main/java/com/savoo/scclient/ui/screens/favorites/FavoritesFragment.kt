package com.savoo.scclient.ui.screens.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.savoo.scclient.R
import com.savoo.scclient.data.local.FavoritesDao
import com.savoo.scclient.data.model.FavoriteArtist
import com.savoo.scclient.data.model.FavoritePlaylist
import com.savoo.scclient.data.model.Playlist
import com.savoo.scclient.data.model.User
import com.savoo.scclient.data.model.toTrack
import com.savoo.scclient.data.repository.FavoritesRepository
import com.savoo.scclient.databinding.FragmentFavoritesBinding
import com.savoo.scclient.player.PlayerController
import com.savoo.scclient.ui.adapters.ArtistAdapter
import com.savoo.scclient.ui.adapters.PlaylistAdapter
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun FavoriteArtist.toUser(): User = User(
    id = artistId,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    followersCount = followersCount,
    permalinkUrl = permalinkUrl
)

private fun FavoritePlaylist.toPlaylist(): Playlist = Playlist(
    id = playlistId,
    title = title,
    artworkUrl = artworkUrl,
    trackCount = trackCount,
    user = User(username = username),
    permalinkUrl = permalinkUrl
)

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var favoritesDao: FavoritesDao
    @Inject lateinit var favoritesRepository: FavoritesRepository
    @Inject lateinit var playerController: PlayerController

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var artistAdapter: ArtistAdapter

    private var currentTab = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupAdapters()
        setupActions()
        observeData()
    }

    private fun setupTabs() {
        val tabLayout = binding.favTabs
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_tracks))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_playlists))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_artists))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                when (currentTab) {
                    0 -> binding.rvFavorites.adapter = trackAdapter
                    1 -> binding.rvFavorites.adapter = playlistAdapter
                    2 -> binding.rvFavorites.adapter = artistAdapter
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                playerController.play(track)
            },
            onLikeClick = { track ->
                viewLifecycleOwner.lifecycleScope.launch {
                    favoritesRepository.toggleFavorite(track)
                }
            }
        )

        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val bundle = Bundle().apply { putLong("playlistId", playlist.id) }
                findNavController().navigate(R.id.nav_playlist, bundle)
            }
        )

        artistAdapter = ArtistAdapter(
            onArtistClick = { artist ->
                val bundle = Bundle().apply { putLong("userId", artist.id) }
                findNavController().navigate(R.id.nav_artist, bundle)
            }
        )

        binding.rvFavorites.adapter = trackAdapter
    }

    private fun setupActions() {
        binding.btnFavShuffle.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val allTracks = favoritesDao.getAllTracks().map { it.toTrack() }
                if (allTracks.isNotEmpty()) {
                    playerController.playQueue(allTracks.shuffled(), 0, repeatAll = true, tag = "favorites_shuffle")
                }
            }
        }

        binding.favRefresh.setOnRefreshListener {
            binding.favRefresh.isRefreshing = false
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    favoritesDao.observeAllTracks().collectLatest { entities ->
                        val tracks = entities.map { it.toTrack() }
                        trackAdapter.submitList(tracks)
                        trackAdapter.favoriteTrackIds = tracks.map { it.id }.toSet()

                        if (currentTab == 0) {
                            val isEmpty = tracks.isEmpty()
                            binding.favEmptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                            binding.rvFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        }
                    }
                }
                launch {
                    favoritesDao.observeAllPlaylists().collectLatest { playlists ->
                        playlistAdapter.submitList(playlists.map { it.toPlaylist() })
                    }
                }
                launch {
                    favoritesDao.observeAllArtists().collectLatest { artists ->
                        artistAdapter.submitList(artists.map { it.toUser() })
                    }
                }
                launch {
                    playerController.state.collectLatest { state ->
                        trackAdapter.currentPlayingTrackId = state.currentTrack?.id
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
