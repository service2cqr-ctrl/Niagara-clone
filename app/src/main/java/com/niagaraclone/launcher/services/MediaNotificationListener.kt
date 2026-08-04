package com.niagaraclone.launcher.services

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaState(
    val title: String = "",
    val artist: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val albumArt: android.graphics.Bitmap? = null
)

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        private val _mediaState = MutableStateFlow<MediaState?>(null)
        val mediaState: StateFlow<MediaState?> = _mediaState

        var activeController: MediaController? = null
            private set
    }

    private lateinit var sessionManager: MediaSessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, MediaNotificationListener::class.java)

        sessionManager.addOnActiveSessionsChangedListener({ controllers ->
            updateActiveSession(controllers)
        }, component)

        updateActiveSession(sessionManager.getActiveSessions(component))
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        val controller = controllers?.firstOrNull()
        activeController = controller

        if (controller == null) {
            _mediaState.value = null
            return
        }

        controller.registerCallback(object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                extractData(controller)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                extractData(controller)
            }
        })

        extractData(controller)
    }

    private fun extractData(controller: MediaController) {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        if (title.isNotEmpty()) {
            _mediaState.value = MediaState(
                title = title,
                artist = artist,
                packageName = controller.packageName,
                isPlaying = isPlaying,
                albumArt = bitmap
            )
        }
    }
}
