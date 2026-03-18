package com.example.minimalmusic.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val mimeType: String,
    val filePath: String,
    val folder: String,
)

data class AlbumGroup(
    val name: String,
    val songs: List<Song>,
)

data class Playlist(
    val name: String,
    val songs: List<Song>,
)
