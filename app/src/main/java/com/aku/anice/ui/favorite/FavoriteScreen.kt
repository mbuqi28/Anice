package com.aku.anice.ui.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aku.anice.data.model.Anime
import com.aku.anice.ui.home.AnimeCard

@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    onAnimeClick: (Anime) -> Unit
) {
    val favoriteList by viewModel.favoriteList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Favorit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp
                )
                Text(
                    text = "${favoriteList.size} donghua tersimpan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        if (favoriteList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BookmarkBorder, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Belum ada donghua favorit", 
                        color = Color.Gray, 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Simpan judul yang ingin kamu tonton nanti", 
                        color = Color.Gray.copy(alpha = 0.6f), 
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoriteList) { favorite ->
                    AnimeCard(
                        anime = Anime(
                            title = favorite.title,
                            thumbUrl = favorite.thumbUrl,
                            detailUrl = favorite.detailUrl
                        ),
                        onClick = {
                            onAnimeClick(
                                Anime(
                                    title = favorite.title,
                                    thumbUrl = favorite.thumbUrl,
                                    detailUrl = favorite.detailUrl
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
