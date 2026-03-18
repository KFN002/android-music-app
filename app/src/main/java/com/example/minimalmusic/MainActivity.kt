package com.example.minimalmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.minimalmusic.data.AlbumGroup
import com.example.minimalmusic.data.Playlist
import com.example.minimalmusic.data.Song
import com.example.minimalmusic.player.MusicViewModel

private val tabs = listOf("All Songs", "Albums", "Folders", "Playlists", "Queue")

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MusicScreen(musicViewModel)
                }
            }
        }
    }
}

@Composable
private fun MusicScreen(vm: MusicViewModel) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()
    val currentSong = vm.getCurrentSong()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionGranted = ContextCompat.checkSelfPermission(
        context,
        requiredPermission
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.loadSongs()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) vm.loadSongs()
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600), repeatMode = RepeatMode.Reverse),
        label = "alphaAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF121212), Color(0xFF181818))
                )
            )
            .padding(16.dp)
    ) {
        if (!permissionGranted) {
            PermissionBlock { permissionLauncher.launch(requiredPermission) }
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Minimal Music ++",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha)
            )
            Text(
                text = "Shuffle • Random • Queue • Albums • Playlists",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3)
            )

            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(visible = uiState.isLoading, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF8ECAE6))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Scanning songs from storage...", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            NowPlayingCard(
                song = currentSong,
                isPlaying = uiState.isPlaying,
                shuffleEnabled = uiState.shuffleEnabled,
                onPrevious = vm::playPreviousOrStart,
                onPlayPause = vm::togglePlayPause,
                onNext = vm::playNext,
                onRestartSong = vm::seekToStart,
                onToggleShuffle = vm::toggleShuffle,
                onRandom = vm::playRandomSong,
                onEqualizer = {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, vm.player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    runCatching { context.startActivity(intent) }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent, contentColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedTabIndex,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            when (selectedTabIndex) {
                0 -> SongsTab(
                    songs = uiState.songs,
                    favoriteSongIds = uiState.favoriteSongIds,
                    onPlay = vm::playSong,
                    onQueue = vm::enqueueSong,
                    onFavorite = vm::toggleFavorite
                )

                1 -> AlbumTab(items = uiState.albums, onPlayGroup = vm::playAlbum)
                2 -> AlbumTab(items = uiState.folders, onPlayGroup = vm::playAlbum)
                3 -> PlaylistsTab(playlists = uiState.playlists, onPlayPlaylist = vm::playPlaylist)
                4 -> QueueTab(queue = uiState.queue, onPlay = vm::playQueueSong, onRemove = vm::removeFromQueue, onClear = vm::clearQueue)
            }
        }
    }
}

@Composable
private fun PermissionBlock(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Allow storage permission to scan local music files.", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}

@Composable
private fun NowPlayingCard(
    song: Song?,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRestartSong: () -> Unit,
    onToggleShuffle: () -> Unit,
    onRandom: () -> Unit,
    onEqualizer: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Now Playing", color = Color(0xFF8ECAE6), style = MaterialTheme.typography.labelMedium)
            AnimatedContent(targetState = song, label = "songChange") { current ->
                Column {
                    Text(
                        text = current?.title ?: "No track selected",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = current?.artist ?: "-",
                        color = Color(0xFFB3B3B3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White) }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                        contentDescription = "Play Pause",
                        tint = Color(0xFF8ECAE6),
                        modifier = Modifier.size(52.dp)
                    )
                }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White) }
                IconButton(onClick = onRestartSong) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Start Song", tint = Color.White) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) Color(0xFF8ECAE6) else Color.White)
                }
                IconButton(onClick = onRandom) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = "Random Song", tint = Color.White)
                }
                IconButton(onClick = onEqualizer) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = "Equalizer", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    favoriteSongIds: Set<Long>,
    onPlay: (Song) -> Unit,
    onQueue: (Song) -> Unit,
    onFavorite: (Song) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(songs, key = { it.id }) { song ->
            SongRow(song = song, isFavorite = song.id in favoriteSongIds, onPlay = { onPlay(song) }, onQueue = { onQueue(song) }, onFavorite = { onFavorite(song) })
        }
    }
}

@Composable
private fun SongRow(song: Song, isFavorite: Boolean, onPlay: () -> Unit, onQueue: () -> Unit, onFavorite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF191919))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onPlay)) {
            Text(text = song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "${song.artist} • ${song.album}", color = Color(0xFFA1A1A1), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onQueue) { Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", tint = Color(0xFF8ECAE6)) }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFFF6B6B) else Color.White
            )
        }
        IconButton(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color(0xFF8ECAE6)) }
    }
}

@Composable
private fun AlbumTab(items: List<AlbumGroup>, onPlayGroup: (AlbumGroup) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.name }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF191919))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${group.songs.size} songs", color = Color(0xFFA1A1A1), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onPlayGroup(group) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Group", tint = Color(0xFF8ECAE6))
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(playlists: List<Playlist>, onPlayPlaylist: (Playlist) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playlists, key = { it.name }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF191919))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White)
                    Text("${playlist.songs.size} tracks", color = Color(0xFFA1A1A1), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onPlayPlaylist(playlist) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play playlist", tint = Color(0xFF8ECAE6))
                }
            }
        }
    }
}

@Composable
private fun QueueTab(queue: List<Song>, onPlay: (Int) -> Unit, onRemove: (Song) -> Unit, onClear: () -> Unit) {
    Column {
        if (queue.isNotEmpty()) {
            Button(onClick = onClear) { Text("Clear Queue") }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF191919))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color(0xFFA1A1A1), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onPlay(index) }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play queue", tint = Color(0xFF8ECAE6)) }
                    IconButton(onClick = { onRemove(song) }) { Text("✕", color = Color.White) }
                }
            }
        }
    }
}
