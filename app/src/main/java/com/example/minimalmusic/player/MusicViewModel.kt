package com.example.minimalmusic.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.minimalmusic.data.AlbumGroup
import com.example.minimalmusic.data.MusicRepository
import com.example.minimalmusic.data.Playlist
import com.example.minimalmusic.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    val player: ExoPlayer = ExoPlayer.Builder(application.applicationContext).build()

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrentSongFromPlayer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        })
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = repository.scanLocalSongs()
            val albums = songs.groupBy { it.album.ifBlank { "Unknown Album" } }
                .map { AlbumGroup(name = it.key, songs = it.value) }
                .sortedBy { it.name.lowercase() }

            val folders = songs.groupBy { it.folder }
                .map { AlbumGroup(name = it.key, songs = it.value) }
                .sortedBy { it.name.lowercase() }

            val playlists = buildAutoPlaylists(songs)

            _uiState.value = _uiState.value.copy(
                songs = songs,
                albums = albums,
                folders = folders,
                playlists = playlists,
                queue = songs.take(10),
                isLoading = false,
                currentSongId = songs.firstOrNull()?.id,
            )

            if (songs.isNotEmpty()) {
                preparePlaylist(songs, startIndex = 0)
            }
        }
    }

    fun playSong(song: Song) {
        val songs = _uiState.value.songs
        val index = songs.indexOfFirst { it.id == song.id }
        if (index < 0) return

        _uiState.value = _uiState.value.copy(currentSongId = song.id, isPlaying = true)
        preparePlaylist(songs, startIndex = index)
    }

    fun playAlbum(album: AlbumGroup) {
        if (album.songs.isEmpty()) return
        _uiState.value = _uiState.value.copy(currentSongId = album.songs.first().id, isPlaying = true)
        preparePlaylist(album.songs, startIndex = 0)
    }

    fun playPlaylist(playlist: Playlist) {
        if (playlist.songs.isEmpty()) return
        _uiState.value = _uiState.value.copy(currentSongId = playlist.songs.first().id, isPlaying = true)
        preparePlaylist(playlist.songs, startIndex = 0)
    }

    fun playQueueSong(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        _uiState.value = _uiState.value.copy(currentSongId = queue[index].id, isPlaying = true)
        preparePlaylist(queue, startIndex = index)
    }

    fun enqueueSong(song: Song) {
        val newQueue = _uiState.value.queue + song
        _uiState.value = _uiState.value.copy(queue = newQueue.distinctBy { it.id })
    }

    fun removeFromQueue(song: Song) {
        val newQueue = _uiState.value.queue.filterNot { it.id == song.id }
        _uiState.value = _uiState.value.copy(queue = newQueue)
    }

    fun clearQueue() {
        _uiState.value = _uiState.value.copy(queue = emptyList())
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            player.play()
            return
        }
        playRandomSong()
    }

    fun playPreviousOrStart() {
        if (player.currentPosition > 4_000) {
            player.seekTo(0L)
            return
        }
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
            player.play()
        }
    }

    fun seekToStart() {
        player.seekTo(0L)
    }

    fun toggleShuffle() {
        val enabled = !_uiState.value.shuffleEnabled
        player.shuffleModeEnabled = enabled
        _uiState.value = _uiState.value.copy(shuffleEnabled = enabled)
    }

    fun playRandomSong() {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        val randomIndex = Random.nextInt(songs.size)
        val selectedSong = songs[randomIndex]
        _uiState.value = _uiState.value.copy(currentSongId = selectedSong.id, isPlaying = true)
        preparePlaylist(songs, startIndex = randomIndex)
    }

    fun toggleFavorite(song: Song) {
        val favorites = _uiState.value.favoriteSongIds.toMutableSet()
        if (!favorites.add(song.id)) {
            favorites.remove(song.id)
        }
        val updated = _uiState.value.copy(favoriteSongIds = favorites)
        _uiState.value = updated.copy(playlists = buildAutoPlaylists(updated.songs, favorites))
    }

    fun getCurrentSong(): Song? {
        val songId = _uiState.value.currentSongId ?: return null
        return _uiState.value.songs.firstOrNull { it.id == songId }
    }

    private fun syncCurrentSongFromPlayer() {
        val currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            currentSongId = currentMediaId,
            isPlaying = player.isPlaying,
        )
    }

    private fun preparePlaylist(songs: List<Song>, startIndex: Int) {
        player.stop()
        player.clearMediaItems()
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    private fun buildAutoPlaylists(
        songs: List<Song>,
        favoriteSongIds: Set<Long> = _uiState.value.favoriteSongIds,
    ): List<Playlist> {
        val favorites = songs.filter { it.id in favoriteSongIds }
        val recent = songs.take(25)
        val longMix = songs.filter { it.durationMs >= 4 * 60 * 1000 }
        return listOf(
            Playlist("Favorites", favorites),
            Playlist("Recently Added", recent),
            Playlist("Long Mix", longMix),
        )
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

data class MusicUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val folders: List<AlbumGroup> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val queue: List<Song> = emptyList(),
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val favoriteSongIds: Set<Long> = emptySet(),
)
