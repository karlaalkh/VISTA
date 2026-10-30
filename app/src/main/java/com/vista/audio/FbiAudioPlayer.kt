package com.vista.audio

import android.content.Context
import android.media.MediaPlayer

class FbiAudioPlayer(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    fun play(
        audioResId: Int,
        onFinished: () -> Unit
    ) {
        stop()

        val mediaPlayer = MediaPlayer.create(
            context,
            audioResId
        ) ?: run {
            onFinished()
            return
        }

        player = mediaPlayer

        mediaPlayer.setOnCompletionListener {
            if (player === mediaPlayer) {
                player = null
                mediaPlayer.release()
                onFinished()
            }
        }

        mediaPlayer.start()
    }

    fun stop() {
        val current = player
        player = null
        current?.release()
    }
}
