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
import com.savoo.scclient.R
import com.savoo.scclient.databinding.FragmentHomeBinding
import com.savoo.scclient.ui.adapters.QuickPickAdapter
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private lateinit var quickPickAdapter: TrackAdapter
    private lateinit var recentAdapter: QuickPickAdapter

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

        setupGreeting()
        setupAdapters()
        setupListeners()
        observeData()
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
        quickPickAdapter = TrackAdapter(
            onTrackClick = { track ->
                viewModel.playerController.play(track)
            },
            onLikeClick = { track ->
                viewModel.toggleFavorite(track)
            }
        )
        binding.rvQuickPicks.adapter = quickPickAdapter

        recentAdapter = QuickPickAdapter(
            onTrackClick = { track ->
                viewModel.playerController.play(track)
            }
        )
        binding.rvRecent.adapter = recentAdapter
    }

    private fun setupListeners() {
        binding.btnPlayMix.setOnClickListener {
            val mixTracks = viewModel.mixTracks.value
            if (mixTracks.isNotEmpty()) {
                viewModel.playMix(mixTracks)
            }
        }

        binding.cardCharts.setOnClickListener {
            findNavController().navigate(R.id.nav_charts)
        }

        binding.cardHistory.setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteTracks.collectLatest { tracks ->
                        quickPickAdapter.submitList(tracks)
                    }
                }
                launch {
                    viewModel.recentTracks.collectLatest { tracks ->
                        recentAdapter.submitList(tracks)
                        binding.titleRecent.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
                        binding.rvRecent.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.playerController.state.collectLatest { state ->
                        quickPickAdapter.currentPlayingTrackId = state.currentTrack?.id
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
