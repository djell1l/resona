package com.savoo.scclient.ui.screens.charts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.savoo.scclient.databinding.FragmentChartsBinding
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<ChartsViewModel>()
    private lateinit var trackAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                val allTracks = viewModel.tracks.value
                val index = allTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                viewModel.playerController.playQueue(allTracks, index, tag = "charts")
            }
        )
        binding.rvCharts.adapter = trackAdapter

        binding.chartsRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.chartsRefresh.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tracks.collectLatest { tracks ->
                        trackAdapter.submitList(tracks)
                    }
                }
                launch {
                    viewModel.isLoading.collectLatest { loading ->
                        binding.chartsLoading.visibility = if (loading) View.VISIBLE else View.GONE
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
