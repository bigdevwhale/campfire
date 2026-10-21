package app.campfire.sound

import android.content.Context
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * The cassette by the fire: an honest thing, no characters. While it records, the reels turn and
 * [amplitude] carries the voices (0..1) so the flames can lean into them. Only a file name in the
 * evening's ticket survives; everything stays in the app's own folder.
 */
class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var poll: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    val recording: Boolean get() = recorder != null

    /** Starts a take; false if the microphone couldn't be had (taken, denied, broken). */
    fun start(): Boolean {
        cancel()
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val out = File(dir, "evening-${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION") // minSdk 28: the context constructor needs API 31.
        val r = MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // Voices by a fire, not music: small files that last for years.
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = out
            poll = scope.launch {
                while (isActive) {
                    val level = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    // Ease towards the new level so the flames sway rather than jump.
                    _amplitude.value = _amplitude.value * 0.6f + (level / 32767f).coerceIn(0f, 1f) * 0.4f
                    delay(80)
                }
            }
            true
        } catch (_: Exception) {
            r.release()
            out.delete()
            false
        }
    }

    /** Stops and keeps the take. Returns its file name, or null if nothing usable was recorded. */
    fun stop(): String? {
        val r = recorder ?: return null
        val out = file
        release()
        return try {
            r.stop()
            out?.name
        } catch (_: RuntimeException) {
            // Stopped before anything was written: there is no take to keep.
            out?.delete()
            null
        } finally {
            r.release()
        }
    }

    /** Stops and throws the take away: the evening never got its ticket. */
    fun cancel() {
        val r = recorder ?: return
        val out = file
        release()
        runCatching { r.stop() }
        r.release()
        out?.delete()
    }

    private fun release() {
        poll?.cancel()
        poll = null
        recorder = null
        file = null
        _amplitude.value = 0f
    }

    fun file(name: String) = File(File(context.filesDir, DIR), name)

    companion object {
        const val DIR = "recordings"
    }
}
