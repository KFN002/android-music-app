package com.example.minimalmusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.minimalmusic.util.TextEncodingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    private val allowedMime = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/flac",
        "audio/wav",
        "audio/x-wav",
        "audio/ogg",
        "audio/mp4",
        "audio/aac",
        "audio/x-m4a"
    )

    private val allowedExtensions = listOf(".mp3", ".flac", ".wav", ".ogg", ".m4a", ".aac")

    suspend fun scanLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC,
        )

        val songs = mutableListOf<Song>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val musicCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)

            while (cursor.moveToNext()) {
                val isMusic = cursor.getInt(musicCol) == 1
                if (!isMusic) continue

                val id = cursor.getLong(idCol)
                val title = TextEncodingUtils.fixCommonMojibake(cursor.getString(titleCol) ?: "Unknown")
                val artist = TextEncodingUtils.fixCommonMojibake(cursor.getString(artistCol) ?: "Unknown Artist")
                val album = TextEncodingUtils.fixCommonMojibake(cursor.getString(albumCol) ?: "Unknown Album")
                val duration = cursor.getLong(durationCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val filePath = cursor.getString(dataCol) ?: ""

                val extensionMatch = allowedExtensions.any { filePath.lowercase().endsWith(it) }
                val mimeMatch = mimeType.lowercase() in allowedMime

                if (!mimeMatch && !extensionMatch) continue

                songs += Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    uri = ContentUris.withAppendedId(collection, id),
                    mimeType = mimeType,
                    filePath = filePath,
                    folder = extractFolderName(filePath),
                )
            }
        }

        songs.distinctBy { it.uri }
    }

    private fun extractFolderName(filePath: String): String {
        if (filePath.isBlank()) return "Unknown Folder"
        return runCatching {
            File(filePath).parentFile?.name?.takeIf { it.isNotBlank() } ?: "Unknown Folder"
        }.getOrDefault("Unknown Folder")
    }
}
