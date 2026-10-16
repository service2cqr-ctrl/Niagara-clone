package com.niagaraclone.launcher.services

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
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
    val postTime: Long,
    val sbn: StatusBarNotification? = null
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

        fun sendDirectReply(notificationKey: String, replyMessage: String): Boolean {
            val listener = instance ?: return false
            val activeSbns = listener.activeNotifications ?: return false
            val targetSbn = activeSbns.firstOrNull { it.key == notificationKey } ?: return false

            val actions = targetSbn.notification.actions ?: return false
            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    if (remoteInput.allowFreeFormInput) {
                        try {
                            val intent = Intent()
                            val bundle = Bundle()
                            bundle.putCharSequence(remoteInput.resultKey, replyMessage)
                            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                            action.actionIntent.send(listener, 0, intent)
                            return true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return false
        }

        fun dismissNotification(notificationKey: String) {
            try {
                instance?.cancelNotification(notificationKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var sessionManager: MediaSessionManager
    private var currentCallback: MediaController.Callback? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, MediaNotificationListener::class.java)

        try {
            sessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateActiveSession(controllers)
            }, component)

            updateActiveSession(sessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeController?.let { controller ->
            currentCallback?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (instance == this) instance = null
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
        if (instance == this) instance = null
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
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                if (title.isNotEmpty() || text.isNotEmpty()) {
                    summaries.add(
                        NotificationSummaryModel(
                            id = sbn.key,
                            packageName = pkg,
                            title = title,
                            text = text,
                            postTime = sbn.postTime,
                            sbn = sbn
                        )
                    )
                }
            }
            _activeNotificationCounts.value = countMap
            _notificationSummaries.value = summaries.sortedByDescending { it.postTime }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        activeController?.let { oldController ->
            currentCallback?.let { oldCb ->
                try {
                    oldController.unregisterCallback(oldCb)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val controller = controllers?.firstOrNull()
        activeController = controller

        if (controller == null) {
            _mediaState.value = null
            return
        }

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                extractData(controller)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                extractData(controller)
            }
        }

        currentCallback = callback
        controller.registerCallback(callback)
        extractData(controller)
    }

    private fun extractData(controller: MediaController) {
        try {
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
            } else {
                _mediaState.value = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
