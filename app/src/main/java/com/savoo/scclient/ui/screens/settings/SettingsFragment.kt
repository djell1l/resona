package com.savoo.scclient.ui.screens.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.savoo.scclient.R
import com.savoo.scclient.auth.TokenStore
import com.savoo.scclient.data.repository.DarkModeOption
import com.savoo.scclient.data.repository.SettingsRepository
import com.savoo.scclient.databinding.FragmentSettingsBinding
import com.savoo.scclient.security.AppLockStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var appLockStore: AppLockStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        binding.settingDarkModeItem.setOnClickListener {
            showDarkModeDialog()
        }

        binding.switchDynamicPalette.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                settingsRepository.setDynamicFromTrack(isChecked)
            }
        }

        binding.settingAboutItem.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showDarkModeDialog() {
        val options = arrayOf(
            getString(R.string.dark_mode_system),
            getString(R.string.dark_mode_light),
            getString(R.string.dark_mode_dark)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_dark_mode)
            .setItems(options) { _, which ->
                val choice = when (which) {
                    0 -> DarkModeOption.SYSTEM
                    1 -> DarkModeOption.LIGHT
                    else -> DarkModeOption.DARK
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsRepository.setDarkMode(choice)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Resona One UI 8")
            .setMessage("Resona is an open-source, ad-free SoundCloud client translated into native Samsung One UI 8 with SESL.\n\nCrafted with One UI reachability principles, dark mode, and dynamic palette.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    settingsRepository.settings.collectLatest { s ->
                        val darkModeStr = when (s.darkMode) {
                            DarkModeOption.SYSTEM -> getString(R.string.dark_mode_system)
                            DarkModeOption.LIGHT -> getString(R.string.dark_mode_light)
                            DarkModeOption.DARK -> getString(R.string.dark_mode_dark)
                        }
                        binding.settingDarkModeValue.text = darkModeStr
                        binding.switchDynamicPalette.isChecked = s.dynamicFromTrack
                    }
                }
                launch {
                    tokenStore.isLoggedIn.collectLatest { loggedIn ->
                        if (loggedIn) {
                            binding.settingAccountTitle.text = getString(R.string.login_logged_in)
                            binding.settingAccountSubtitle.text = getString(R.string.account_title)
                            binding.btnSettingLogin.text = getString(R.string.account_log_out)
                            binding.btnSettingLogin.setOnClickListener {
                                viewLifecycleOwner.lifecycleScope.launch { tokenStore.clear() }
                            }
                        } else {
                            binding.settingAccountTitle.text = getString(R.string.login_not_logged_in)
                            binding.settingAccountSubtitle.text = getString(R.string.login_subtitle)
                            binding.btnSettingLogin.text = getString(R.string.login_button)
                        }
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
