package com.niagaraclone.launcher.services

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioConnectionReceiver : BroadcastReceiver() {

    companion object {
        private val _isAudioDeviceConnected = MutableStateFlow(false)
        val isAudioDeviceConnected: StateFlow<Boolean> = _isAudioDeviceConnected.asStateFlow()

        fun checkAudioConnectionState(context: Context) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val isConnected = devices.any { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> true
                    else -> false
                }
            }
            _isAudioDeviceConnected.value = isConnected
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
        
        // Double-check with modern device routing scanner
        checkAudioConnectionState(context)
    }
}
