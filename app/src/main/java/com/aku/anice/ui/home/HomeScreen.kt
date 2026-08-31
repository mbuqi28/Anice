package com.aku.anice.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aku.anice.R
import com.aku.anice.data.model.Anime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Anime) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isPaginationLoading && !uiState.endOfList) {
            viewModel.loadNextPage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrandingHeader(onFilterClick = { viewModel.toggleFilter(true) })

        // Search Field
        OutlinedTextField(
            value = searchText,
            onValueChange = { 
                searchText = it
                viewModel.loadAnime(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            placeholder = { Text("Cari judul favoritmu...", color = Color.Gray, fontSize = 14.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        if (uiState.isLoading && uiState.animeList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(uiState.animeList) { _, anime ->
                    AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                }

                if (uiState.isPaginationLoading) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }

    if (uiState.isFilterOpen) {
        FilterBottomSheet(
            currentFilter = uiState.filter,
            onApply = { newFilter -> 
                viewModel.loadAnime(searchText, newFilter)
                viewModel.toggleFilter(false)
            },
            onDismiss = { viewModel.toggleFilter(false) }
        )
    }
}

@Composable
fun BrandingHeader(onFilterClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "branding")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary,
        ),
        start = Offset(shimmerOffset, shimmerOffset),
        end = Offset(shimmerOffset + 200f, shimmerOffset + 200f),
        tileMode = TileMode.Mirror
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Anice",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = shimmerBrush
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    lineHeight = 36.sp
                )
                Text(
                    text = "Donghua Streaming",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        IconButton(
            onClick = onFilterClick,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterState,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var tempStatus by remember { mutableStateOf(currentFilter.status) }
        var tempOrder by remember { mutableStateOf(currentFilter.order) }

        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Filter Donghua", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            Text("Status", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tempStatus == "", onClick = { tempStatus = "" }, label = { Text("Semua") })
                FilterChip(selected = tempStatus == "ongoing", onClick = { tempStatus = "ongoing" }, label = { Text("Ongoing") })
                FilterChip(selected = tempStatus == "completed", onClick = { tempStatus = "completed" }, label = { Text("Tamat") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Urutkan", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tempOrder == "update", onClick = { tempOrder = "update" }, label = { Text("Update") })
                FilterChip(selected = tempOrder == "latest", onClick = { tempOrder = "latest" }, label = { Text("Terbaru") })
                FilterChip(selected = tempOrder == "popular", onClick = { tempOrder = "popular" }, label = { Text("Populer") })
                FilterChip(selected = tempOrder == "title", onClick = { tempOrder = "title" }, label = { Text("A-Z") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onApply(FilterState(tempStatus, tempOrder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Terapkan Filter")
            }
        }
    }
}

@Composable
fun AnimeCard(anime: Anime, onClick: () -> Unit) {
    val displayTitle = anime.title.split(" ").distinct().joinToString(" ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = anime.thumbUrl,
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 200f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = displayTitle,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
