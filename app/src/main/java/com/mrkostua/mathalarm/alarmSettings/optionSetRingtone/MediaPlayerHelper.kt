package com.mrkostua.mathalarm.alarmSettings.optionSetRingtone

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import com.mrkostua.mathalarm.tools.ConstantValues
import com.mrkostua.mathalarm.tools.ShowLogs
import javax.inject.Inject

/**
 * @author Kostiantyn Prysiazhnyi on 17.01.2018.
 */
class MediaPlayerHelper @Inject constructor(private val context: Context) : MediaPlayer.OnErrorListener {
    private val TAG = this.javaClass.simpleName
    private var isMpPlaying = false
    private var mediaPlayer: MediaPlayer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var userVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    private val handlerAdjustVolume = 3
    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                handlerAdjustVolume -> {
                    ShowLogs.log(TAG, "handleMessage : handlerAdjustVolume")
                    adjustVolume()

                }
            }
        }
    }

    fun playRingtoneFromRingtoneOb(ringtoneOb: RingtoneObject, isAlarmStreamType: Boolean = false) {
        if (ringtoneOb.uri == null) {
            playRingtoneFromStringResource(ringtoneOb.name, isAlarmStreamType)

        } else {
            playRingtoneFromUri(ringtoneOb.uri, isAlarmStreamType)

        }
    }

    /**
     * sets stream volume to 1 and increase it every 10 seconds
     */
    fun playDeepWakeUpRingtone(ringtoneOb: RingtoneObject) {
        userVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
        playRingtoneFromRingtoneOb(ringtoneOb, true)
        sendHandlerDelayAdjustVolume()

    }

    fun stopRingtone() {
        if (isMpPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            isMpPlaying = false
        }
        disableHandlerMessages(handlerAdjustVolume)

    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()

    }

    private fun playRingtoneFromStringResource(ringtoneResourceName: String, isAlarmStreamType: Boolean) {
        ShowLogs.log(TAG, "playRingtoneFromStringResource : isAlarmStream type : " + isAlarmStreamType + " and : res id : " + ringtoneResourceName)
        if (isMpPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()

        }
        mediaPlayer = getNewMediaPlayer(ringtoneResourceName)
        startPlayingMusic(mediaPlayer, isAlarmStreamType)
        isMpPlaying = true

    }

    private fun playRingtoneFromUri(ringtoneUri: Uri, isAlarmStreamType: Boolean) {
        ShowLogs.log(TAG, "playRingtoneFromStringResource : isAlarmStream type : " + isAlarmStreamType + " and uri : " + ringtoneUri)
        if (isMpPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()

        }
        mediaPlayer = getNewMediaPlayer(ringtoneUri)
        startPlayingMusic(mediaPlayer, isAlarmStreamType)
        isMpPlaying = true

    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        ShowLogs.log(TAG, "getNewMediaPlayer onErrorListener what :" + what + " extra : " + extra)
        mp.stop()
        mp.release()
        mediaPlayer = null
        isMpPlaying = false
        return true
    }

    private fun startPlayingMusic(mp: MediaPlayer?, isAlarmStreamType: Boolean = false) {
        if (isAlarmStreamType) {
            setAlarmStream()

        }
        mp?.isLooping = true
        mediaPlayer?.setOnPreparedListener({
            it.start()
        })
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnErrorListener(this)
    }

    private fun setAlarmStream() {
        if (Build.VERSION.SDK_INT >= 26) {
            mediaPlayer?.setAudioAttributes(audioAttributes)
        } else {
            @Suppress("DEPRECATION")
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_ALARM)

        }

    }

    private inline fun <reified T : Any> getNewMediaPlayer(ringtone: T): MediaPlayer? {
        ShowLogs.log(TAG, "getNewMediaPlayer int ringtone : " + ringtone.toString())
        mediaPlayer = MediaPlayer()
        when (T::class) {
            String::class -> mediaPlayer?.setDataSource(context,
                    Uri.parse(ConstantValues.ANDROID_RESOURCE_PATH + context.packageName + "/raw/" + ringtone))

            Uri::class -> mediaPlayer?.setDataSource(context, ringtone as Uri)

            else -> throw UnsupportedOperationException("Not implemented")
        }
        return mediaPlayer

    }

    private fun sendHandlerDelayAdjustVolume() {
        handler.sendMessageDelayed(handler.obtainMessage(handlerAdjustVolume),
                ConstantValues.DEEP_WAKE_UP_VOLUME_ADJUSTMENT_MILLISECONDS)

    }

    private fun disableHandlerMessages(what: Int) {
        handler.removeMessages(what)

    }

    private fun adjustVolume() {
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) < userVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    audioManager.getStreamVolume(AudioManager.STREAM_ALARM) + 1,
                    0)
            sendHandlerDelayAdjustVolume()

        } else {
            ShowLogs.log(TAG, "adjustVolume() volume " +
                    audioManager.getStreamVolume(AudioManager.STREAM_ALARM) + " is set to max")
        }
    }

}