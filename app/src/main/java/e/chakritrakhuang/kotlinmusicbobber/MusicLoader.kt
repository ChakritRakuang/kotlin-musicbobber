package e.chakritrakhuang.kotlinmusicbobber

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

import java.util.ArrayList
import java.util.Collections

internal class MusicLoader(context : Context) : BaseAsyncTaskLoader<Collection<MusicItem>>(context) {

    private val albumArtUri = Uri.parse("content://media/external/audio/albumart")

    @SuppressLint("Recycle")
    override fun loadInBackground() : Collection<MusicItem> {
        val projection = arrayOf(MediaStore.Audio.Media.TITLE , MediaStore.Audio.Media.ALBUM , MediaStore.Audio.Media.ALBUM_ID , MediaStore.Audio.Media.ARTIST , MediaStore.Audio.Media.DURATION , MediaStore.Audio.Media.DATA)
        val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,
                projection ,
                MediaStore.Audio.Media.IS_MUSIC + "=1" , null ,
                "LOWER(" + MediaStore.Audio.Media.ARTIST + ") ASC, " +
                        "LOWER(" + MediaStore.Audio.Media.ALBUM + ") ASC, " +
                        "LOWER(" + MediaStore.Audio.Media.TITLE + ") ASC"
        ) ?: return emptyList()
        val items = ArrayList<MusicItem>()
        try {
            if (cursor.moveToFirst()) {
                val title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val album = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumId = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                do {
                    val item = MusicItem()
                            .title(cursor.getString(title))
                            .album(cursor.getString(album))
                            .artist(cursor.getString(artist))
                            .duration(cursor.getLong(duration))
                            .albumArtUri(ContentUris.withAppendedId(albumArtUri , cursor.getLong(albumId)))
                            .fileUri(Uri.parse(cursor.getString(data)))
                    items.add(item)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        return items
    }
}