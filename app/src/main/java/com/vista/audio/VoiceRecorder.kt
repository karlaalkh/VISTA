package com.vista.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): Boolean {
        if (recorder != null) return false

        val file = File(
            context.cacheDir,
            "maya_response_${System.currentTimeMillis()}.m4a"
        )

        val mediaRecorder = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            (MediaRecorder())
        }

        return try {
            mediaRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )
            mediaRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )
            mediaRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )
            mediaRecorder.setAudioEncodingBitRate(64_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder
            outputFile = file
            true
        } catch (e: Exception) {
            mediaRecorder.release()
            file.delete()
            false
        }
    }

    fun stop(): File? {
        val activeRecorder = recorder ?: return null
        recorder = null

        return try {
            activeRecorder.stop()
            activeRecorder.release()
            outputFile.also { outputFile = null }
        } catch (e: Exception) {
            activeRecorder.release()
            outputFile?.delete()
            outputFile = null
            null
        }
    }

    fun release() {
        if (recorder != null) {
            stop()?.delete()
        }
    }
}
