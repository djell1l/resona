package com.savoo.scclient.ui.screens.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.savoo.scclient.R
import com.savoo.scclient.databinding.FragmentSearchBinding
import com.savoo.scclient.ui.adapters.ArtistAdapter
import com.savoo.scclient.ui.adapters.PlaylistAdapter
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<SearchViewModel>()

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var playlistAdapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupAdapters()
        setupSearchInput()
        observeData()
    }

    private fun setupTabs() {
        val tabLayout = binding.searchTabs
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_tracks))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_artists))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_playlists))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val targetTab = when (tab.position) {
                    0 -> SearchTab.TRACKS
                    1 -> SearchTab.ARTISTS
                    else -> SearchTab.ALBUMS
                }
                viewModel.onTabChange(targetTab)
                updateCurrentAdapter(targetTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                viewModel.playTrack(track)
            },
            onLikeClick = { track ->
                viewModel.toggleFavorite(track)
            }
        )

        artistAdapter = ArtistAdapter(
            onArtistClick = { artist ->
                val bundle = Bundle().apply { putLong("userId", artist.id) }
                findNavController().navigate(R.id.nav_artist, bundle)
            }
        )

        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val bundle = Bundle().apply { putLong("playlistId", playlist.id) }
                findNavController().navigate(R.id.nav_playlist, bundle)
            }
        )

        binding.rvSearchResults.adapter = trackAdapter
    }

    private fun updateCurrentAdapter(tab: SearchTab) {
        when (tab) {
            SearchTab.TRACKS -> binding.rvSearchResults.adapter = trackAdapter
            SearchTab.ARTISTS -> binding.rvSearchResults.adapter = artistAdapter
            SearchTab.ALBUMS -> binding.rvSearchResults.adapter = playlistAdapter
        }
    }

    private fun setupSearchInput() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.onQueryChange(it)
                    viewModel.onQuerySubmit()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.onQueryChange(it) }
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshResults()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        trackAdapter.submitList(state.tracks)
                        artistAdapter.submitList(state.artists)
                        playlistAdapter.submitList(state.albums)

                        binding.searchLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                        val currentCount = when (state.activeTab) {
                            SearchTab.TRACKS -> state.tracks.size
                            SearchTab.ARTISTS -> state.artists.size
                            SearchTab.ALBUMS -> state.albums.size
                        }

                        val isEmpty = currentCount == 0 && !state.isLoading
                        binding.emptySearchView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvSearchResults.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.playerController.state.collectLatest { state ->
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
