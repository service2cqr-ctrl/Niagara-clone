package com.niagaraclone.launcher.services

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioConnectionReceiver : BroadcastReceiver() {

    companion object {
        private val _isAudioDeviceConnected = MutableStateFlow(false)
        val isAudioDeviceConnected: StateFlow<Boolean> = _isAudioDeviceConnected.asStateFlow()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                _isAudioDeviceConnected.value = (state == 1)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                _isAudioDeviceConnected.value = true
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                _isAudioDeviceConnected.value = false
            }
        }
    }
}
