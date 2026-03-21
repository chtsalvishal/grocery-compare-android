package com.example.grocerycompare.ui.screens.home

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.repository.MasterCatalogueRepository
import com.example.grocerycompare.data.source.remote.ScrapeProgress
import com.example.grocerycompare.ui.theme.*


// ─────────────────────────────────────────────────────────────────────────────
// Store brand colours (intentionally fixed — not theme-aware)
// ─────────────────────────────────────────────────────────────────────────────
private val ColesColor   = Color(0xFFE01A22)
private val WooliesColor = Color(0xFF178841)
private val AldiColor    = Color(0xFF001E7E)

private fun storeColor(store: String) = when (store) {
    "Coles"   -> ColesColor
    "Woolies" -> WooliesColor
    "Aldi"    -> AldiColor
    else      -> Color.Gray
}

private val SORT_OPTIONS = listOf(
    "multi_store" to "Multi-store",
    "price_asc"   to "Price ↑",
    "price_desc"  to "Price ↓",
    "savings"     to "Savings",
    "az"          to "A–Z",
)

data class StorePriceEntry(
    val store: String,
    val price: Double,
    val wasPrice: Double?,
)

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: MasterCatalogueRepository) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(application, repository))
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(FreshGreen)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(color = FreshGreen, modifier = Modifier.fillMaxWidth()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Smart Compare",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    "${uiState.suburb}  •  ${uiState.postcode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        actions = {
                            IconButton(
                                onClick = { viewModel.triggerScrape() },
                                enabled = !uiState.isRefreshing,
                            ) {
                                if (uiState.isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                                }
                            }
                        },
                        windowInsets = TopAppBarDefaults.windowInsets,
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    ),
            ) {
                SearchBar(uiState.searchQuery, viewModel::onSearchQueryChanged)

                FilterRow(
                    selectedStores = uiState.selectedStores,
                    onStoreToggled = viewModel::toggleStoreSelection,
                    sortOrder      = uiState.sortOrder,
                    onSortSelected = viewModel::setSortOrder,
                )

                CategoryRow(
                    categories       = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                )

                if (uiState.products.isNotEmpty()) {
                    DealCountBar(count = uiState.products.size, isRefreshing = uiState.isRefreshing)
                }

                if (uiState.isWarmingUp) WarmingUpBanner()

                uiState.syncError?.let { ErrorBanner(it) }

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isRefreshing && uiState.products.isEmpty() -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) { items(6) { ShimmerCard() } }
                        }
                        uiState.products.isEmpty() -> {
                            EmptyScreen(onScrape = { viewModel.triggerScrape() })
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp, bottom = 120.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(uiState.products, key = { it.universalName }) { product ->
                                    MasterProductCard(product, uiState.selectedStores)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = uiState.isRefreshing,
                            enter   = slideInVertically { it } + fadeIn(),
                            exit    = slideOutVertically { it } + fadeOut(),
                        ) {
                            SyncProgressPanel(progress = uiState.progress)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FilterRow(
    selectedStores: Set<String>,
    onStoreToggled: (String) -> Unit,
    sortOrder: String,
    onSortSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Coles", "Woolies", "Aldi").forEach { store ->
                val selected = selectedStores.contains(store)
                val color    = storeColor(store)
                Surface(
                    modifier = Modifier.clickable { onStoreToggled(store) },
                    shape    = RoundedCornerShape(20.dp),
                    color    = if (selected) color else MaterialTheme.colorScheme.surface,
                    border   = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selected) color else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(
                        text       = store,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (selected) Color.White else color,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        SortDropdown(sortOrder = sortOrder, onSortSelected = onSortSelected)
    }
}

@Composable
fun SortDropdown(sortOrder: String, onSortSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = SORT_OPTIONS.firstOrNull { it.first == sortOrder }?.second ?: "Sort"

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier  = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            SORT_OPTIONS.forEach { (key, optLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            optLabel,
                            fontWeight = if (key == sortOrder) FontWeight.Bold else FontWeight.Normal,
                            color      = if (key == sortOrder) FreshGreen else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingIcon = if (key == sortOrder) ({
                        Icon(Icons.Default.Check, null, tint = FreshGreen, modifier = Modifier.size(16.dp))
                    }) else null,
                    onClick = { onSortSelected(key); expanded = false },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category chip row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    if (categories.size <= 1) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = cat == selectedCategory,
                onClick  = { onCategorySelected(cat) },
                label    = { Text(cat, fontSize = 12.sp) },
                shape    = RoundedCornerShape(20.dp),
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FreshGreen,
                    selectedLabelColor     = Color.White,
                    containerColor         = MaterialTheme.colorScheme.surface,
                    labelColor             = MaterialTheme.colorScheme.onSurface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled           = true,
                    selected          = cat == selectedCategory,
                    borderColor       = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = FreshGreen,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deal count bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DealCountBar(count: Int, isRefreshing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = "$count deals found",
            style      = MaterialTheme.typography.labelMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        if (isRefreshing) {
            Text(
                text       = "Updating…",
                style      = MaterialTheme.typography.labelSmall,
                color      = FreshGreen,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar  — fixes white-on-white text bug in dark mode
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value       = query,
        onValueChange = onQueryChange,
        modifier    = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        placeholder = {
            Text("Search deals…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape     = RoundedCornerShape(14.dp),
        singleLine = true,
        colors    = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
            cursorColor             = FreshGreen,
            focusedBorderColor      = FreshGreen,
            unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Warming-up banner (Render cold-start info)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WarmingUpBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color    = FreshGreen.copy(alpha = 0.10f),
        shape    = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color       = FreshGreen,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Server is warming up…",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = FreshGreen,
                )
                Text(
                    "First sync takes ~30 s. Hang tight!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color    = ColesColor.copy(alpha = 0.10f),
        shape    = RoundedCornerShape(8.dp),
    ) {
        Text(
            text     = "⚠  $message",
            style    = MaterialTheme.typography.labelSmall,
            color    = ColesColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sync progress panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SyncProgressPanel(progress: ScrapeProgress?) {
    val pct = if (progress != null && progress.total > 0)
        progress.current.toFloat() / progress.total.toFloat() else 0f

    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        Modifier.graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else Modifier

    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Surface(
            modifier        = Modifier.matchParentSize().then(blurMod),
            color           = surfaceColor.copy(alpha = 0.92f),
            shape           = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp,
        ) {}

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Syncing fresh prices",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text       = "${(pct * 100).toInt()}%",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = FreshGreen,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress    = { pct },
                modifier    = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color       = FreshGreen,
                trackColor  = FreshGreen.copy(alpha = 0.15f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("Coles", "Woolies", "Aldi").forEach { store ->
                    val status = progress?.storeStatuses?.firstOrNull {
                        it.name.equals(store, ignoreCase = true) ||
                        (store == "Woolies" && it.name.equals("Woolworths", ignoreCase = true))
                    }
                    StoreIndicator(
                        store     = store,
                        isDone    = status?.isDone ?: false,
                        isLoading = progress != null && !(status?.isDone ?: false),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text      = progress?.message ?: "Connecting…",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun StoreIndicator(store: String, isDone: Boolean, isLoading: Boolean) {
    val color = storeColor(store)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape    = CircleShape,
                color    = color.copy(alpha = if (isDone) 1f else if (isLoading) alpha else 0.2f),
            ) {}
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text       = store.first().toString(),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color      = Color.White,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = if (isDone) "Done" else if (isLoading) "Syncing" else "Waiting",
            style      = MaterialTheme.typography.labelSmall,
            color      = if (isDone) FreshGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Product comparison card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MasterProductCard(product: MasterProductEntity, selectedStores: Set<String>) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    val entries = buildList {
        if (selectedStores.contains("Coles")   && product.colesPrice   > 0)
            add(StorePriceEntry("Coles",   product.colesPrice,   product.colesWasPrice.takeIf   { it > 0 }))
        if (selectedStores.contains("Woolies") && product.wooliesPrice > 0)
            add(StorePriceEntry("Woolies", product.wooliesPrice, product.wooliesWasPrice.takeIf { it > 0 }))
        if (selectedStores.contains("Aldi")    && product.aldiPrice    > 0)
            add(StorePriceEntry("Aldi",    product.aldiPrice,    product.aldiWasPrice.takeIf    { it > 0 }))
    }
    if (entries.isEmpty()) return

    val minPrice = entries.minOf { it.price }
    val maxPrice = entries.maxOf { it.price }
    val priceDiff = maxPrice - minPrice
    val cheapestEntry = entries.first { it.price == minPrice }

    val biggestSpecialSaving = entries
        .filter { it.wasPrice != null && it.wasPrice > it.price }
        .maxOfOrNull { it.wasPrice!! - it.price }

    val specialStoreEntry = if (biggestSpecialSaving != null && biggestSpecialSaving >= 0.10)
        entries.firstOrNull { it.wasPrice != null && it.wasPrice > it.price &&
            kotlin.math.abs((it.wasPrice - it.price) - biggestSpecialSaving) < 0.001 }
    else null

    val savingText = when {
        specialStoreEntry != null && biggestSpecialSaving != null ->
            "🏷  Save \$${String.format(java.util.Locale.US, "%.2f", biggestSpecialSaving)} on special at ${specialStoreEntry.store}"
        entries.size > 1 && priceDiff >= 0.10 ->
            "💚  Save \$${String.format(java.util.Locale.US, "%.2f", priceDiff)} by choosing ${cheapestEntry.store}"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model            = product.imageUrl,
                            contentDescription = product.universalName,
                            contentScale     = ContentScale.Fit,
                            modifier         = Modifier.fillMaxSize().padding(6.dp),
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = product.universalName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FreshGreen.copy(alpha = 0.10f),
                        ) {
                            Text(
                                text       = product.category,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = FreshGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (entries.size > 1) {
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${entries.size} stores",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entries.forEach { entry ->
                    StorePriceTile(
                        entry      = entry,
                        isCheapest = entry.price == minPrice && entries.size > 1,
                        modifier   = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            if (savingText != null) {
                Surface(
                    color = FreshGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                ) {
                    Text(
                        text       = savingText,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = FreshGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StorePriceTile(entry: StorePriceEntry, isCheapest: Boolean, modifier: Modifier = Modifier) {
    val color       = storeColor(entry.store)
    val borderColor = if (isCheapest) FreshGreen else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .border(if (isCheapest) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(
                if (isCheapest) FreshGreen.copy(alpha = 0.04f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text          = entry.store.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            fontWeight    = FontWeight.ExtraBold,
            color         = color,
            letterSpacing = 0.5.sp,
        )

        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (entry.wasPrice != null && entry.wasPrice > entry.price) {
                Text(
                    text           = "\$${String.format(java.util.Locale.US, "%.2f", entry.wasPrice)}",
                    fontSize       = 10.sp,
                    color          = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }

        Text(
            text       = "\$${String.format(java.util.Locale.US, "%.2f", entry.price)}",
            fontWeight = FontWeight.Black,
            color      = if (isCheapest) FreshGreen else MaterialTheme.colorScheme.onSurface,
            fontSize   = 22.sp,
        )

        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (isCheapest) {
                Surface(color = FreshGreen, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text       = "BEST PRICE",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 8.sp,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShimmerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    // Theme-aware shimmer colours — visible in both light and dark mode
    val shimmerBase     = MaterialTheme.colorScheme.surfaceVariant
    val shimmerHighlight = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(shimmerBase, shimmerHighlight, shimmerBase),
        start  = Offset.Zero,
        end    = Offset(x, x),
    )
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)).background(brush))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.65f).height(15.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(10.dp)).background(brush))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EmptyScreen(onScrape: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = FreshGreen.copy(alpha = 0.1f), modifier = Modifier.size(96.dp)) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(28.dp),
                tint     = FreshGreen.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Deals Yet",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap below to sync the latest specials from Coles, Woolies & Aldi.",
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick  = onScrape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = FreshGreen),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Fresh Prices", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
