package com.niagaraclone.launcher.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
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

        fun checkAudioConnectionState(context: Context) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val isWiredConnected = audioManager?.isWiredHeadsetOn == true
            val isBluetoothConnected = audioManager?.isBluetoothA2dpOn == true || audioManager?.isBluetoothScoOn == true
            _isAudioDeviceConnected.value = isWiredConnected || isBluetoothConnected
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (context == null) return

        when (action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state != -1) {
                    _isAudioDeviceConnected.value = (state == 1)
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                if (state == BluetoothHeadset.STATE_CONNECTED || action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                    _isAudioDeviceConnected.value = true
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                _isAudioDeviceConnected.value = false
            }
        }
        checkAudioConnectionState(context)
    }
}
