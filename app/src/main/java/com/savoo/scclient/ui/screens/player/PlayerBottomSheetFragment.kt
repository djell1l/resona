package com.savoo.scclient.ui.screens.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.SeslSeekBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.savoo.scclient.R
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.data.repository.FavoritesRepository
import com.savoo.scclient.databinding.FragmentPlayerBinding
import com.savoo.scclient.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PlayerBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var favoritesRepository: FavoritesRepository

    private var currentTrack: Track? = null
    private var isUserTracking = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observePlayback()
    }

    private fun setupListeners() {
        binding.btnPlayerCollapse.setOnClickListener {
            dismiss()
        }

        binding.fabPlayerPlayPause.setOnClickListener {
            playerController.togglePlayPause()
        }

        binding.btnPlayerNext.setOnClickListener {
            playerController.skipToNext()
        }

        binding.btnPlayerPrev.setOnClickListener {
            playerController.skipToPrevious()
        }

        binding.btnPlayerShuffle.setOnClickListener {
            playerController.toggleShuffle()
        }

        binding.btnPlayerRepeat.setOnClickListener {
            playerController.cycleRepeatMode()
        }

        binding.btnPlayerFav.setOnClickListener {
            currentTrack?.let { track ->
                viewLifecycleOwner.lifecycleScope.launch {
                    favoritesRepository.toggleFavorite(track)
                }
            }
        }

        binding.playerArtist.setOnClickListener {
            currentTrack?.user?.id?.let { userId ->
                dismiss()
                val bundle = Bundle().apply { putLong("userId", userId) }
                // Navigate via activity or parent nav controller
            }
        }

        binding.playerSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentTrack != null && currentTrack!!.durationMs > 0) {
                    val targetMs = (progress.toLong() * currentTrack!!.durationMs) / 1000L
                    binding.playerTimeCurrent.text = formatDuration(targetMs)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserTracking = true
                playerController.beginScrub()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserTracking = false
                val progress = seekBar?.progress ?: 0
                if (currentTrack != null && currentTrack!!.durationMs > 0) {
                    val targetMs = (progress.toLong() * currentTrack!!.durationMs) / 1000L
                    playerController.endScrub(targetMs)
                }
            }
        })
    }

    private fun observePlayback() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerController.state.collectLatest { state ->
                    currentTrack = state.currentTrack
                    val track = state.currentTrack

                    if (track != null) {
                        binding.playerTitle.text = track.title
                        binding.playerArtist.text = track.user.username
                        binding.playerArtwork.load(track.artworkUrl?.replace("-large", "-t500x500")) {
                            crossfade(true)
                            placeholder(R.drawable.logo_git)
                            error(R.drawable.logo_git)
                        }

                        binding.playerTimeTotal.text = formatDuration(track.durationMs)
                        if (!isUserTracking && track.durationMs > 0) {
                            binding.playerTimeCurrent.text = formatDuration(state.positionMs)
                            val progress = ((state.positionMs * 1000L) / track.durationMs).toInt()
                            binding.playerSeekbar.progress = progress.coerceIn(0, 1000)
                        }
                    }

                    // Play/Pause icon
                    binding.fabPlayerPlayPause.setImageResource(
                        if (state.isPlaying) R.drawable.ic_oui_pause else R.drawable.ic_oui_play
                    )

                    // Shuffle & Repeat tint states
                    binding.btnPlayerShuffle.alpha = if (state.shuffleEnabled) 1.0f else 0.4f
                    binding.btnPlayerRepeat.alpha = if (state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) 1.0f else 0.4f
                }
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PlayerBottomSheetFragment"
        fun newInstance() = PlayerBottomSheetFragment()
    }
}
