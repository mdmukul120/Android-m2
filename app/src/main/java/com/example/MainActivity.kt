package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.DownloadHelper
import com.example.ui.components.MediaDetailsSheet
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.SearchDialog
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MukulBlack
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MukulPlusApp()
            }
        }
    }
}

data class NavItem(val bnTitle: String, val enTitle: String, val icon: ImageVector)

@Composable
fun MukulPlusApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isBangla = state.language == "bn"

    val navItems = listOf(
        NavItem("হোম", "Home", Icons.Default.Home),
        NavItem("মুভিজ", "Movies", Icons.Default.Movie),
        NavItem("সিরিজ", "Series", Icons.Default.Tv),
        NavItem("লাইভ টিভি", "Live TV", Icons.Default.LiveTv),
        NavItem("সেটিংস", "Settings", Icons.Default.Settings)
    )

    // Handle Android system Back button
    BackHandler(
        enabled = state.activePlayingMedia != null ||
                state.activePlayingChannel != null ||
                state.offlinePlayingMedia != null ||
                state.selectedMediaForDetails != null ||
                state.isSearchOpen
    ) {
        if (state.activePlayingMedia != null || state.activePlayingChannel != null || state.offlinePlayingMedia != null) {
            viewModel.stopPlayback()
        } else if (state.selectedMediaForDetails != null) {
            viewModel.closeMediaDetails()
        } else if (state.isSearchOpen) {
            viewModel.setSearchOpen(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(if (state.isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))) {
        // 1. Splash Screen on Launch
        if (state.isSplashVisible) {
            SplashScreen(
                onTimeout = { viewModel.dismissSplash() }
            )
        } else {
            // 2. Main App Screen Layout
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    if (state.activePlayingMedia == null && state.activePlayingChannel == null && state.offlinePlayingMedia == null) {
                        MukulTopBar(
                            isDarkMode = state.isDarkMode,
                            onSearchClick = { viewModel.setSearchOpen(true) },
                            onProfileClick = { viewModel.selectTab(4) }
                        )
                    }
                },
                bottomBar = {
                    if (state.activePlayingMedia == null && state.activePlayingChannel == null && state.offlinePlayingMedia == null) {
                        NavigationBar(
                            containerColor = if (state.isDarkMode) MukulBlack else Color.White,
                            contentColor = if (state.isDarkMode) MukulTextPrimary else Color.Black,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(64.dp)
                                .testTag("bottom_navigation_bar")
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = state.selectedTab == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(index) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = if (isBangla) item.bnTitle else item.enTitle,
                                            tint = if (isSelected) MukulRedPrimary else MukulTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isBangla) item.bnTitle else item.enTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) (if (state.isDarkMode) Color.White else Color.Black) else MukulTextSecondary
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MukulRedPrimary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Screen Router
                    when (state.selectedTab) {
                        0 -> HomeScreen(
                            heroMedia = state.heroMedia,
                            sections = state.sections,
                            liveChannels = state.allChannels,
                            watchlistIds = state.watchlistIds,
                            isLoading = state.isLoading,
                            isBangla = isBangla,
                            onMediaClick = { viewModel.openMediaDetails(it) },
                            onChannelClick = { viewModel.playChannel(it) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                            onSeeAllChannels = { viewModel.selectTab(3) }
                        )

                        1 -> {
                            val allMovies = state.sections.flatMap { it.items }.distinctBy { it.id }
                            MoviesScreen(
                                movies = allMovies,
                                selectedCategory = state.movieCategory,
                                watchlistIds = state.watchlistIds,
                                isBangla = isBangla,
                                onSelectCategory = { viewModel.setMovieCategory(it) },
                                onMovieClick = { viewModel.openMediaDetails(it) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                            )
                        }

                        2 -> {
                            val seriesList = state.sections
                                .flatMap { it.items }
                                .filter { it.category == "series" || it.category == "bongo" || it.episodes.isNotEmpty() }
                                .distinctBy { it.id }
                            SeriesScreen(
                                seriesList = seriesList,
                                watchlistIds = state.watchlistIds,
                                isBangla = isBangla,
                                onPlayMedia = { viewModel.openMediaDetails(it) },
                                onPlayEpisode = { series, episode ->
                                    if (episode.streamUrl.isNotEmpty()) {
                                        viewModel.playDirectStream("${series.title} - ${episode.title}", episode.streamUrl)
                                    } else {
                                        viewModel.openMediaDetails(series)
                                    }
                                },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                            )
                        }

                        3 -> LiveTvScreen(
                            channels = state.allChannels,
                            selectedCategory = state.channelCategory,
                            isBangla = isBangla,
                            onSelectCategory = { viewModel.setChannelCategory(it) },
                            onChannelClick = { viewModel.playChannel(it) }
                        )

                        4 -> SettingsScreen(
                            currentLanguage = state.language,
                            onLanguageChange = { viewModel.setLanguage(it) },
                            isDarkMode = state.isDarkMode,
                            onThemeToggle = { viewModel.setDarkMode(it) },
                            selectedQuality = state.selectedQuality,
                            onQualityChange = { viewModel.setSelectedQuality(it) },
                            onPlayOfflineMedia = { viewModel.playOfflineMedia(it) }
                        )
                    }

                    // Video Player Layer
                    val media = state.activePlayingMedia
                    val channel = state.activePlayingChannel
                    val offlineMedia = state.offlinePlayingMedia

                    if (media != null) {
                        VideoPlayerView(
                            title = media.title,
                            streamUrl = media.streamUrl.ifEmpty { "https://4397879b.wurl.com/master/f36d25e7e52f1ba8d7e56eb859c636563214f541/UmFrdXRlblRWLWRlX0ZJRkFQbHVzR2VybWFuX0hMUw/playlist.m3u8" },
                            isLive = false,
                            subtitle = "${media.year} • ${media.genre}",
                            onDownloadClick = {
                                val url = media.downloadUrl.ifEmpty { media.streamUrl }
                                DownloadHelper.downloadMovie(context, media.title, url)
                            },
                            onClose = { viewModel.stopPlayback() }
                        )
                    } else if (channel != null) {
                        VideoPlayerView(
                            title = channel.name,
                            streamUrl = channel.streamUrl,
                            isLive = true,
                            subtitle = "${channel.group} • Live Stream",
                            onClose = { viewModel.stopPlayback() }
                        )
                    } else if (offlineMedia != null) {
                        VideoPlayerView(
                            title = offlineMedia.title,
                            streamUrl = "file://${offlineMedia.filePath}",
                            isLive = false,
                            subtitle = "Offline In-App Playback • ${offlineMedia.fileSize}",
                            onClose = { viewModel.stopPlayback() }
                        )
                    }

                    // Media Details Bottom Sheet Layer (Movie extraction, quality & in-app downloads)
                    state.selectedMediaForDetails?.let { item ->
                        MediaDetailsSheet(
                            media = item,
                            isBangla = isBangla,
                            onDismiss = { viewModel.closeMediaDetails() },
                            onPlayMedia = { title, streamUrl ->
                                viewModel.playDirectStream(title, streamUrl)
                            }
                        )
                    }

                    // Search Dialog Layer
                    if (state.isSearchOpen) {
                        val allMedia = state.sections.flatMap { it.items }.distinctBy { it.id }
                        SearchDialog(
                            searchQuery = state.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            allMedia = allMedia,
                            allChannels = state.allChannels,
                            onSelectMedia = {
                                viewModel.setSearchOpen(false)
                                viewModel.openMediaDetails(it)
                            },
                            onSelectChannel = {
                                viewModel.setSearchOpen(false)
                                viewModel.playChannel(it)
                            },
                            onDismiss = { viewModel.setSearchOpen(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MukulTopBar(
    isDarkMode: Boolean,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(if (isDarkMode) MukulBlack else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("mukul_top_bar")
    ) {
        // Logo: "mukul plus"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_logo")
        ) {
            Text(
                text = "mukul",
                color = if (isDarkMode) Color.White else Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MukulRedPrimary
            ) {
                Text(
                    text = "PLUS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF1B1C26) else Color(0xFFEEEEF4))
                    .testTag("search_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User Profile / Settings Avatar Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MukulRedPrimary)
                    .border(1.5.dp, MukulRedGlowing, CircleShape)
                    .clickable(onClick = onProfileClick)
                    .testTag("profile_avatar")
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
