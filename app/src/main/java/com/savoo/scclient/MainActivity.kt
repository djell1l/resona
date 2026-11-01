package com.savoo.scclient

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.google.android.material.tabs.TabLayout
import com.savoo.scclient.data.local.FavoritesDao
import com.savoo.scclient.data.model.Track
import com.savoo.scclient.data.remote.WebViewApiBridge
import com.savoo.scclient.data.repository.DarkModeOption
import com.savoo.scclient.data.repository.FavoritesRepository
import com.savoo.scclient.data.repository.LanguageOption
import com.savoo.scclient.data.repository.SettingsRepository
import com.savoo.scclient.data.repository.systemDefaultLanguage
import com.savoo.scclient.databinding.ActivityMainBinding
import com.savoo.scclient.debug.CrashReporter
import com.savoo.scclient.i18n.CustomStrings
import com.savoo.scclient.i18n.CustomStringsDisableReason
import com.savoo.scclient.i18n.withCustomStrings
import com.savoo.scclient.player.PlayerController
import com.savoo.scclient.security.AppLockStore
import com.savoo.scclient.ui.navigation.DeepLinkTarget
import com.savoo.scclient.ui.navigation.ShortcutTarget
import com.savoo.scclient.ui.screens.player.PlayerBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var favoritesRepository: FavoritesRepository
    @Inject lateinit var favoritesDao: FavoritesDao
    @Inject lateinit var webBridge: WebViewApiBridge
    @Inject lateinit var appLockStore: AppLockStore
    @Inject lateinit var dataDomeChallenge: com.savoo.scclient.data.remote.DataDomeChallenge

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    private var apiWebView: WebView? = null
    private var currentPlayingTrack: Track? = null

    fun navigateTo(destinationId: Int, args: Bundle? = null) {
        navController.navigate(destinationId, args)
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = try {
            val prefs = newBase.getSharedPreferences("sc_settings", MODE_PRIVATE)
            prefs.getString("language", null)?.let {
                runCatching { LanguageOption.valueOf(it) }.getOrNull()
            } ?: systemDefaultLanguage(newBase)
        } catch (_: Exception) {
            LanguageOption.ENGLISH
        }

        val locale = lang.locale ?: Locale.getDefault()
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val updatedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(updatedContext.withCustomStrings())
    }

    private fun applyOrientationLock() {
        requestedOrientation = if (resources.getBoolean(R.bool.lock_portrait)) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationLock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOrientationLock()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupMiniPlayer()
        setupWebViewBridge()
        observePlayback()
        observeSettings()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> navController.navigate(R.id.nav_home)
                    1 -> navController.navigate(R.id.nav_search)
                    2 -> navController.navigate(R.id.nav_favorites)
                    3 -> navController.navigate(R.id.nav_settings)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Return to root of the tab if reselected
                when (tab?.position) {
                    0 -> navController.popBackStack(R.id.nav_home, false)
                    1 -> navController.popBackStack(R.id.nav_search, false)
                    2 -> navController.popBackStack(R.id.nav_favorites, false)
                    3 -> navController.popBackStack(R.id.nav_settings, false)
                }
            }
        })
    }

    private fun setupMiniPlayer() {
        val mini = binding.miniPlayerView

        mini.miniPlayerRoot.setOnClickListener {
            // Expand to full One UI Player Bottom Sheet
            val playerSheet = PlayerBottomSheetFragment.newInstance()
            playerSheet.show(supportFragmentManager, PlayerBottomSheetFragment.TAG)
        }

        mini.miniPlayerPlayPause.setOnClickListener {
            playerController.togglePlayPause()
        }

        mini.miniPlayerNext.setOnClickListener {
            playerController.skipToNext()
        }

        mini.miniPlayerLike.setOnClickListener {
            currentPlayingTrack?.let { track ->
                lifecycleScope.launch {
                    favoritesRepository.toggleTrackFavorite(track)
                }
            }
        }
    }

    private fun setupWebViewBridge() {
        apiWebView = WebView(this).apply {
            alpha = 0f
            addContentView(this, android.view.ViewGroup.LayoutParams(1, 1))
            webBridge.setWebView(this)
        }
    }

    private fun observePlayback() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var favoriteJob: kotlinx.coroutines.Job? = null
                playerController.state.collectLatest { state ->
                    val track = state.currentTrack
                    currentPlayingTrack = track

                    val mini = binding.miniPlayerView
                    if (track != null) {
                        favoriteJob?.cancel()
                        favoriteJob = launch {
                            favoritesDao.isTrackFavorite(track.id).collectLatest { isFav ->
                                mini.miniPlayerLike.setImageResource(
                                    if (isFav) R.drawable.ic_oui_heart else R.drawable.ic_oui_heart_outline
                                )
                            }
                        }

                        mini.miniPlayerRoot.visibility = View.VISIBLE
                        mini.miniPlayerTitle.text = track.title
                        mini.miniPlayerArtist.text = track.user.username

                        mini.miniPlayerArt.load(track.artworkUrl?.replace("-large", "-t300x300")) {
                            crossfade(true)
                            placeholder(R.drawable.logo_git)
                            error(R.drawable.logo_git)
                        }

                        mini.miniPlayerPlayPause.setImageResource(
                            if (state.isPlaying) R.drawable.ic_oui_pause else R.drawable.ic_oui_play
                        )

                        if (track.durationMs > 0) {
                            val progress = ((state.positionMs * 1000L) / track.durationMs).toInt()
                            mini.miniPlayerProgress.progress = progress.coerceIn(0, 1000)
                        }
                    } else {
                        favoriteJob?.cancel()
                        mini.miniPlayerRoot.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collectLatest { s ->
                    val targetNightMode = when (s.darkMode) {
                        DarkModeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        DarkModeOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        DarkModeOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
                        AppCompatDelegate.setDefaultNightMode(targetNightMode)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val deepLinkTarget = DeepLinkTarget.fromIntent(intent)
        when (deepLinkTarget) {
            is DeepLinkTarget.Artist -> {
                val bundle = Bundle().apply { putLong("userId", deepLinkTarget.userId) }
                navController.navigate(R.id.nav_artist, bundle)
            }
            is DeepLinkTarget.Playlist -> {
                val bundle = Bundle().apply { putLong("playlistId", deepLinkTarget.playlistId) }
                navController.navigate(R.id.nav_playlist, bundle)
            }
            is DeepLinkTarget.Shortcut -> {
                when (deepLinkTarget.target) {
                    ShortcutTarget.FAVORITES -> navController.navigate(R.id.nav_favorites)
                    ShortcutTarget.OFFLINE -> navController.navigate(R.id.nav_offline)
                    ShortcutTarget.SEARCH -> navController.navigate(R.id.nav_search)
                }
            }
            else -> {}
        }
    }

    override fun onDestroy() {
        apiWebView?.destroy()
        apiWebView = null
        playerController.saveState()
        super.onDestroy()
    }
}
