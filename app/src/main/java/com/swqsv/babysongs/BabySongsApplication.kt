package com.swqsv.babysongs

import android.app.Application
import com.swqsv.babysongs.data.cache.SongIndexCache
import com.swqsv.babysongs.data.prefs.LibraryRootPreferences
import com.swqsv.babysongs.data.prefs.PlaybackPreferences
import com.swqsv.babysongs.playback.PlaybackController

class BabySongsApplication : Application() {

    lateinit var playbackPreferences: PlaybackPreferences
        private set

    lateinit var libraryRootPreferences: LibraryRootPreferences
        private set

    lateinit var songIndexCache: SongIndexCache
        private set

    lateinit var playbackController: PlaybackController
        private set

    override fun onCreate() {
        super.onCreate()
        playbackPreferences = PlaybackPreferences(this)
        libraryRootPreferences = LibraryRootPreferences(this)
        songIndexCache = SongIndexCache(this)
        playbackController = PlaybackController(this, playbackPreferences)
    }
}
