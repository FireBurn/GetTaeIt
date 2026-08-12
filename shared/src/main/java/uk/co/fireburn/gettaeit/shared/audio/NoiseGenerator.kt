package uk.co.fireburn.gettaeit.shared.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class NoiseGenerator {
    private var job: Job? = null
    private var audioTrack: AudioTrack? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val minSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()

            val random = Random()
            val buffer = ShortArray(minSize)
            var lastOut = 0.0

            while (isActive) {
                for (i in buffer.indices) {
                    val white = (random.nextDouble() * 2 - 1)
                    // Brown noise filter (leaky integrator)
                    val brown = (lastOut + (0.02 * white)) / 1.02
                    lastOut = brown
                    
                    // Normalize to short and lower volume
                    val volumeScaling = 15000.0 // keep it quiet in background
                    buffer[i] = (brown * volumeScaling).toInt().toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
