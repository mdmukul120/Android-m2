package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.MediaCard
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun MoviesScreen(
    movies: List<MediaItem>,
    selectedCategory: String,
    watchlistIds: Set<String>,
    isBangla: Boolean,
    onSelectCategory: (String) -> Unit,
    onMovieClick: (MediaItem) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Bangla Cinema", "Bollywood", "Hindi Dubbed")

    val filteredMovies = when (selectedCategory) {
        "Bangla Cinema" -> movies.filter { it.genre.contains("Bangla", true) || it.category.contains("bioscope", true) }
        "Bollywood" -> movies.filter { it.title.contains("Hindi", true) || it.genre.contains("Hindi", true) || it.provider == "hdhub4u" || it.provider == "moviesmod" }
        "Hindi Dubbed" -> movies.filter { it.title.contains("Dubbed", true) || it.title.contains("Dual", true) || it.provider == "topmovies" }
        else -> movies
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MukulDarkBg)
            .testTag("movies_screen")
    ) {
        // Categories Filter Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                val label = when (category) {
                    "All" -> if (isBangla) "সকল মুভি" else "All"
                    "Bangla Cinema" -> if (isBangla) "বাংলা সিনেমা" else "Bangla Cinema"
                    "Bollywood" -> if (isBangla) "বলিউড" else "Bollywood"
                    "Hindi Dubbed" -> if (isBangla) "হিন্দি ডাবড" else "Hindi Dubbed"
                    else -> category
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MukulRedPrimary else Color(0xFF1B1C26),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262738)),
                    modifier = Modifier.clickable { onSelectCategory(category) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MukulTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Movies Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredMovies, key = { it.id }) { movie ->
                MediaCard(
                    media = movie,
                    isInWatchlist = watchlistIds.contains(movie.id),
                    onClick = { onMovieClick(movie) },
                    onToggleWatchlist = { onToggleWatchlist(movie.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
