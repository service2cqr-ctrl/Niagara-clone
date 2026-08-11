package com.niagaraclone.launcher.services

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaState(
    val title: String = "",
    val artist: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val albumArt: android.graphics.Bitmap? = null
)

data class NotificationSummaryModel(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        var instance: MediaNotificationListener? = null
            private set

        private val _mediaState = MutableStateFlow<MediaState?>(null)
        val mediaState: StateFlow<MediaState?> = _mediaState

        private val _activeNotificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val activeNotificationCounts: StateFlow<Map<String, Int>> = _activeNotificationCounts.asStateFlow()

        private val _notificationSummaries = MutableStateFlow<List<NotificationSummaryModel>>(emptyList())
        val notificationSummaries: StateFlow<List<NotificationSummaryModel>> = _notificationSummaries.asStateFlow()

        var activeController: MediaController? = null
            private set
    }

    private lateinit var sessionManager: MediaSessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, MediaNotificationListener::class.java)

        try {
            sessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateActiveSession(controllers)
            }, component)

            updateActiveSession(sessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
            // Triggers if listener permission is not granted yet
            e.printStackTrace()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        val component = ComponentName(this, MediaNotificationListener::class.java)
        try {
            updateActiveSession(sessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        updateNotificationStates()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotificationStates()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotificationStates()
    }

    private fun updateNotificationStates() {
        try {
            val active = activeNotifications ?: return
            val countMap = mutableMapOf<String, Int>()
            val summaries = mutableListOf<NotificationSummaryModel>()

            for (sbn in active) {
                if (sbn.isOngoing) continue
                val pkg = sbn.packageName
                countMap[pkg] = (countMap[pkg] ?: 0) + 1

                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence("android.title")?.toString() ?: ""
                val text = extras?.getCharSequence("android.text")?.toString() ?: ""

                if (title.isNotEmpty() || text.isNotEmpty()) {
                    summaries.add(
                        NotificationSummaryModel(
                            id = sbn.key,
                            packageName = pkg,
                            title = title,
                            text = text,
                            postTime = sbn.postTime
                        )
                    )
                }
            }
            _activeNotificationCounts.value = countMap
            _notificationSummaries.value = summaries
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
