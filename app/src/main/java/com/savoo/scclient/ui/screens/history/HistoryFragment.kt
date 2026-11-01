package com.savoo.scclient.ui.screens.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.savoo.scclient.data.local.PlayHistoryDao
import com.savoo.scclient.data.model.toTrack
import com.savoo.scclient.databinding.FragmentHistoryBinding
import com.savoo.scclient.player.PlayerController
import com.savoo.scclient.ui.adapters.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var playHistoryDao: PlayHistoryDao
    @Inject lateinit var playerController: PlayerController

    private lateinit var trackAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                playerController.play(track)
            }
        )
        binding.rvHistory.adapter = trackAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playHistoryDao.observeRecent().collectLatest { events ->
                    val tracks = events.map { it.toTrack() }
                    trackAdapter.submitList(tracks)
                    val isEmpty = tracks.isEmpty()
                    binding.historyEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.rvHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
