package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uk.co.fireburn.gettaeit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerBookScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    
    val allStickers = listOf(
        StickerDef("sticker_wizard", "Hyperfocus Wizard", R.drawable.sticker_wizard),
        StickerDef("sticker_knight", "Procrastination Slayer", R.drawable.sticker_knight),
        StickerDef("sticker_spoon", "Spoon Master", R.drawable.sticker_spoon)
    )

    val unlockedStickers = runCatching {
        Gson().fromJson<List<String>>(prefs.unlockedStickersJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }.getOrDefault(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sticker Book & Trophies") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Keep completing tasks to unlock more stickers for your collection!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allStickers) { sticker ->
                    val isUnlocked = unlockedStickers.contains(sticker.id)
                    StickerItem(sticker = sticker, isUnlocked = isUnlocked)
                }
            }
        }
    }
}

data class StickerDef(val id: String, val title: String, val resId: Int)

@Composable
fun StickerItem(sticker: StickerDef, isUnlocked: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isUnlocked) 1f else 0.5f),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            if (isUnlocked) {
                Image(
                    painter = painterResource(id = sticker.resId),
                    contentDescription = sticker.title,
                    modifier = Modifier.weight(1f),
                    contentScale = ContentScale.Fit
                )
            } else {
                val matrix = ColorMatrix()
                matrix.setToSaturation(0f)
                Image(
                    painter = painterResource(id = sticker.resId),
                    contentDescription = "Locked Sticker",
                    modifier = Modifier.weight(1f).alpha(0.3f),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(matrix)
                )
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Text(
                text = if (isUnlocked) sticker.title else "???",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
