package sh.kau.playground.features.settings.viewmodel

import sh.kau.playground.usf.api.Usf

interface SettingsBViewModel : Usf<SettingsBEvent, SettingsBUiState, SettingsBEffect>

sealed interface SettingsBEvent

data class SettingsBUiState(
    val title: String = "Settings B",
    val quoteText: String = "",
    val quoteAuthor: String = "",
)

sealed interface SettingsBEffect
