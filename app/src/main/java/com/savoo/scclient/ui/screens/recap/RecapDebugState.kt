package com.savoo.scclient.ui.screens.recap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecapDebugState {
    private val _forceBanner = MutableStateFlow(false)
    val forceBanner = _forceBanner.asStateFlow()

    fun setForceBanner(value: Boolean) {
        _forceBanner.value = value
    }
}
