package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContentSection
import com.example.data.model.DownloadedMedia
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MukulUiState(
    val isSplashVisible: Boolean = true,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0: Home, 1: Movies, 2: Series, 3: Live TV, 4: Settings
    val language: String = "bn", // "bn" (Bangla) or "en" (English)
    val isDarkMode: Boolean = true,
    val selectedQuality: String = "Auto",
    val heroMedia: MediaItem? = null,
    val sections: List<ContentSection> = emptyList(),
    val allChannels: List<LiveChannel> = emptyList(),
    val channelCategory: String = "Bangla", // Bangla is prioritized first!
    val movieCategory: String = "All",
    val activePlayingMedia: MediaItem? = null,
    val activePlayingChannel: LiveChannel? = null,
    val selectedMediaForDetails: MediaItem? = null,
    val offlinePlayingMedia: DownloadedMedia? = null,
    val watchlistIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(
    private val repository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MukulUiState())
    val uiState: StateFlow<MukulUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (hero, sections, channels) = repository.getHomeContent()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        heroMedia = hero,
                        sections = sections,
                        allChannels = channels
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load content. Please check network."
                    )
                }
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(isSplashVisible = false) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(language = lang) }
    }

    fun setDarkMode(isDark: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun setSelectedQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun openMediaDetails(media: MediaItem) {
        _uiState.update { it.copy(selectedMediaForDetails = media) }
    }

    fun closeMediaDetails() {
        _uiState.update { it.copy(selectedMediaForDetails = null) }
    }

    fun playMedia(media: MediaItem) {
        _uiState.update {
            it.copy(
                activePlayingMedia = media,
                activePlayingChannel = null,
                offlinePlayingMedia = null,
                selectedMediaForDetails = null
            )
        }
    }

    fun playDirectStream(title: String, streamUrl: String) {
        val tempMedia = MediaItem(
            id = "direct_${streamUrl.hashCode()}",
            title = title,
            streamUrl = streamUrl
        )
        playMedia(tempMedia)
    }

    fun playChannel(channel: LiveChannel) {
        _uiState.update {
            it.copy(
                activePlayingChannel = channel,
                activePlayingMedia = null,
                offlinePlayingMedia = null
            )
        }
    }

    fun playOfflineMedia(downloaded: DownloadedMedia) {
        _uiState.update {
            it.copy(
                offlinePlayingMedia = downloaded,
                activePlayingMedia = null,
                activePlayingChannel = null
            )
        }
    }

    fun stopPlayback() {
        _uiState.update {
            it.copy(
                activePlayingMedia = null,
                activePlayingChannel = null,
                offlinePlayingMedia = null
            )
        }
    }

    fun toggleWatchlist(id: String) {
        _uiState.update { state ->
            val updated = state.watchlistIds.toMutableSet()
            if (updated.contains(id)) {
                updated.remove(id)
            } else {
                updated.add(id)
            }
            state.copy(watchlistIds = updated)
        }
    }

    fun setChannelCategory(category: String) {
        _uiState.update { it.copy(channelCategory = category) }
    }

    fun setMovieCategory(category: String) {
        _uiState.update { it.copy(movieCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open, searchQuery = if (!open) "" else it.searchQuery) }
    }
}
