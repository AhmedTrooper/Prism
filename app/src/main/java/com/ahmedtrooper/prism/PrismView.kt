package com.ahmedtrooper.prism

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import com.ahmedtrooper.prism.PrismLib.MpvFormat.MPV_FORMAT_DOUBLE
import com.ahmedtrooper.prism.PrismLib.MpvFormat.MPV_FORMAT_FLAG
import com.ahmedtrooper.prism.PrismLib.MpvFormat.MPV_FORMAT_INT64
import com.ahmedtrooper.prism.PrismLib.MpvFormat.MPV_FORMAT_NONE
import com.ahmedtrooper.prism.PrismLib.MpvFormat.MPV_FORMAT_STRING
import kotlin.reflect.KProperty

internal class PrismView(context: Context, attrs: AttributeSet) : BasePrismView(context, attrs) {
    override fun initOptions() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // apply phone-optimized defaults
        PrismLib.setOptionString("profile", "fast")

        // vo
        setVo(if (sharedPreferences.getBoolean("gpu_next", false))
            "gpu-next"
        else
            "gpu")

        // hwdec
        val hwdec = if (sharedPreferences.getBoolean("hardware_decoding", true))
            HWDECS
        else
            "no"

        // vo: set display fps as reported by android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val disp = ContextCompat.getDisplayOrDefault(context)
            val refreshRate = disp.mode.refreshRate

            Log.v(TAG, "Display ${disp.displayId} reports FPS of $refreshRate")
            PrismLib.setOptionString("display-fps-override", refreshRate.toString())
        } else {
            Log.v(TAG, "Android version too old, disabling refresh rate functionality " +
                       "(${Build.VERSION.SDK_INT} < ${Build.VERSION_CODES.M})")
        }

        // set non-complex options
        data class Property(val preferenceName: String, val mpvOption: String)
        val opts = arrayOf(
                Property("default_audio_language", "alang"),
                Property("default_subtitle_language", "slang")
        )

        for ((preferenceName, mpvOption) in opts) {
            val preference = sharedPreferences.getString(preferenceName, "")
            if (!preference.isNullOrBlank())
                PrismLib.setOptionString(mpvOption, preference)
        }

        PrismLib.setOptionString("gpu-context", "android")
        PrismLib.setOptionString("opengl-es", "yes")
        PrismLib.setOptionString("hwdec", hwdec)
        PrismLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        PrismLib.setOptionString("ao", "audiotrack,opensles")
        PrismLib.setOptionString("audio-set-media-role", "yes")
        PrismLib.setOptionString("tls-verify", "yes")
        PrismLib.setOptionString("tls-ca-file", "${this.context.filesDir.path}/cacert.pem")
        PrismLib.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
        PrismLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        PrismLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
        //
        val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        screenshotDir.mkdirs()
        PrismLib.setOptionString("screenshot-directory", screenshotDir.path)
    }

    override fun postInitOptions() {
        // we need to call write-watch-later manually
        PrismLib.setOptionString("save-position-on-quit", "no")
    }

    fun onPointerEvent(event: MotionEvent): Boolean {
        assert (event.isFromSource(InputDevice.SOURCE_CLASS_POINTER))
        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val h = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val v = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (h > 0)
                PrismLib.command(arrayOf("keypress", "WHEEL_RIGHT", "$h"))
            else if (h < 0)
                PrismLib.command(arrayOf("keypress", "WHEEL_LEFT", "${-h}"))
            if (v > 0)
                PrismLib.command(arrayOf("keypress", "WHEEL_UP", "$v"))
            else if (v < 0)
                PrismLib.command(arrayOf("keypress", "WHEEL_DOWN", "${-v}"))
            return true
        }
        return false
    }

    fun onKey(event: KeyEvent): Boolean {
        // ACTION_MULTIPLE is deprecated in API 29+ because the framework can
        // dispatch multiple keys per event. The constant is still emitted by
        // some IMEs and keyboard devices on Android 6-10, so we keep the check
        // for legacy support and suppress the platform deprecation warning.
        @Suppress("DEPRECATION")
        val isMultiple = event.action == KeyEvent.ACTION_MULTIPLE
        if (isMultiple)
            return false
        if (KeyEvent.isModifierKey(event.keyCode))
            return false

        var mapped = keyMapping[event.keyCode]
        if (mapped == null) {
            // Fallback to produced glyph
            if (!event.isPrintingKey) {
                if (event.repeatCount == 0)
                    Log.d(TAG, "Unmapped non-printable key ${event.keyCode}")
                return false
            }

            val ch = event.unicodeChar
            if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0)
                return false // dead key
            mapped = ch.toChar().toString()
        }

        if (event.repeatCount > 0)
            return true // eat event but ignore it, mpv has its own key repeat

        val mod: MutableList<String> = mutableListOf()
        event.isShiftPressed && mod.add("shift")
        event.isCtrlPressed && mod.add("ctrl")
        event.isAltPressed && mod.add("alt")
        event.isMetaPressed && mod.add("meta")

        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        mod.add(mapped)
        PrismLib.command(arrayOf(action, mod.joinToString("+")))

        return true
    }

    override fun observeProperties() {
        // This observes all properties needed by PrismView, PlayerActivity or other classes
        data class Property(val name: String, val format: Int = MPV_FORMAT_NONE)
        val p = arrayOf(
            Property("time-pos", MPV_FORMAT_INT64),
            Property("duration/full", MPV_FORMAT_DOUBLE),
            Property("pause", MPV_FORMAT_FLAG),
            Property("paused-for-cache", MPV_FORMAT_FLAG),
            Property("speed", MPV_FORMAT_STRING),
            Property("track-list"),
            Property("video-params/aspect", MPV_FORMAT_DOUBLE),
            Property("video-params/rotate", MPV_FORMAT_DOUBLE),
            Property("playlist-pos", MPV_FORMAT_INT64),
            Property("playlist-count", MPV_FORMAT_INT64),
            Property("current-tracks/video/image"),
            Property("media-title", MPV_FORMAT_STRING),
            Property("metadata"),
            Property("loop-playlist"),
            Property("loop-file"),
            Property("shuffle", MPV_FORMAT_FLAG),
            Property("hwdec-current"),
            Property("mute", MPV_FORMAT_FLAG),
            Property("current-tracks/audio/selected")
        )

        for ((name, format) in p)
            PrismLib.observeProperty(name, format)
    }

    fun addObserver(o: PrismLib.EventObserver) {
        PrismLib.addObserver(o)
    }
    fun removeObserver(o: PrismLib.EventObserver) {
        PrismLib.removeObserver(o)
    }

    data class Track(val mpvId: Int, val name: String)
    var tracks = mapOf<String, MutableList<Track>>(
            "audio" to arrayListOf(),
            "video" to arrayListOf(),
            "sub" to arrayListOf())

    fun loadTracks() {
        for (list in tracks.values) {
            list.clear()
            // pseudo-track to allow disabling audio/subs
            list.add(Track(-1, context.getString(R.string.track_off)))
        }
        val count = PrismLib.getPropertyInt("track-list/count")!!
        // Note that because events are async, properties might disappear at any moment
        // so use ?: continue instead of !!
        for (i in 0 until count) {
            val type = PrismLib.getPropertyString("track-list/$i/type") ?: continue
            if (!tracks.containsKey(type)) {
                Log.w(TAG, "Got unknown track type: $type")
                continue
            }
            val mpvId = PrismLib.getPropertyInt("track-list/$i/id") ?: continue
            val lang = PrismLib.getPropertyString("track-list/$i/lang")
            val title = PrismLib.getPropertyString("track-list/$i/title")

            val trackName = if (!lang.isNullOrEmpty() && !title.isNullOrEmpty())
                context.getString(R.string.ui_track_title_lang, mpvId, title, lang)
            else if (!lang.isNullOrEmpty() || !title.isNullOrEmpty())
                context.getString(R.string.ui_track_text, mpvId, (lang ?: "") + (title ?: ""))
            else
                context.getString(R.string.ui_track, mpvId)
            tracks.getValue(type).add(Track(
                    mpvId=mpvId,
                    name=trackName
            ))
        }
    }

    data class PlaylistItem(val index: Int, val filename: String, val title: String?)

    fun loadPlaylist(): MutableList<PlaylistItem> {
        val playlist = mutableListOf<PlaylistItem>()
        val count = PrismLib.getPropertyInt("playlist-count")!!
        for (i in 0 until count) {
            val filename = PrismLib.getPropertyString("playlist/$i/filename")!!
            val title = PrismLib.getPropertyString("playlist/$i/title")
            playlist.add(PlaylistItem(index=i, filename=filename, title=title))
        }
        return playlist
    }

    data class Chapter(val index: Int, val title: String?, val time: Double)

    fun loadChapters(): MutableList<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val count = PrismLib.getPropertyInt("chapter-list/count")!!
        for (i in 0 until count) {
            val title = PrismLib.getPropertyString("chapter-list/$i/title")
            val time = PrismLib.getPropertyDouble("chapter-list/$i/time")!!
            chapters.add(Chapter(
                    index=i,
                    title=title,
                    time=time
            ))
        }
        return chapters
    }

    // Property getters/setters

    var paused: Boolean?
        get() = PrismLib.getPropertyBoolean("pause")
        set(paused) = PrismLib.setPropertyBoolean("pause", paused!!)

    var timePos: Double?
        get() = PrismLib.getPropertyDouble("time-pos/full")
        set(progress) = PrismLib.setPropertyDouble("time-pos", progress!!)

    /** name of currently active hardware decoder or "no" */
    val hwdecActive: String
        get() = PrismLib.getPropertyString("hwdec-current") ?: "no"

    var playbackSpeed: Double?
        get() = PrismLib.getPropertyDouble("speed")
        set(speed) = PrismLib.setPropertyDouble("speed", speed!!)

    var subDelay: Double?
        get() = PrismLib.getPropertyDouble("sub-delay")
        set(speed) = PrismLib.setPropertyDouble("sub-delay", speed!!)

    var secondarySubDelay: Double?
        get() = PrismLib.getPropertyDouble("secondary-sub-delay")
        set(speed) = PrismLib.setPropertyDouble("secondary-sub-delay", speed!!)

    val estimatedVfFps: Double?
        get() = PrismLib.getPropertyDouble("estimated-vf-fps")

    /**
     * Returns the video aspect ratio. Rotation is taken into account.
     */
    fun getVideoAspect(): Double? {
        return PrismLib.getPropertyDouble("video-params/aspect")?.let {
            if (it < 0.001)
                return 0.0
            val rot = PrismLib.getPropertyInt("video-params/rotate") ?: 0
            if (rot % 180 == 90)
                1.0 / it
            else
                it
        }
    }

    fun setAudioSessionId(id: Int) {
        PrismLib.setPropertyInt("audiotrack-session-id", id)
        PrismLib.setPropertyInt("aaudio-session-id", id)
    }

    class TrackDelegate(private val name: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = PrismLib.getPropertyString(name)
            // we can get null here for "no" or other invalid value
            return v?.toIntOrNull() ?: -1
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            if (value == -1)
                PrismLib.setPropertyString(name, "no")
            else
                PrismLib.setPropertyInt(name, value)
        }
    }

    var vid: Int by TrackDelegate("vid")
    var sid: Int by TrackDelegate("sid")
    var secondarySid: Int by TrackDelegate("secondary-sid")
    var aid: Int by TrackDelegate("aid")

    // Commands

    fun cyclePause() = PrismLib.command(arrayOf("cycle", "pause"))
    fun cycleAudio() = PrismLib.command(arrayOf("cycle", "audio"))
    fun cycleSub() = PrismLib.command(arrayOf("cycle", "sub"))
    fun cycleHwdec() = PrismLib.command(arrayOf("cycle-values", "hwdec", HWDECS, "no"))

    fun cycleSpeed() {
        val speeds = arrayOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
        val currentSpeed = playbackSpeed ?: 1.0
        val index = speeds.indexOfFirst { it > currentSpeed }
        playbackSpeed = speeds[if (index == -1) 0 else index]
    }

    fun getRepeat(): Int {
        return when (PrismLib.getPropertyString("loop-playlist") +
                PrismLib.getPropertyString("loop-file")) {
            "noinf" -> 2
            "infno" -> 1
            else -> 0
        }
    }

    fun cycleRepeat() {
        when (val state = getRepeat()) {
            0, 1 -> {
                PrismLib.setPropertyString("loop-playlist", if (state == 1) "no" else "inf")
                PrismLib.setPropertyString("loop-file", if (state == 1) "inf" else "no")
            }
            2 -> PrismLib.setPropertyString("loop-file", "no")
        }
    }

    fun getShuffle(): Boolean {
        return PrismLib.getPropertyBoolean("shuffle") == true
    }

    fun changeShuffle(cycle: Boolean, value: Boolean = true) {
        // Use the 'shuffle' property to store the shuffled state, since changing
        // it at runtime doesn't do anything.
        val state = getShuffle()
        val newState = if (cycle) state.xor(value) else value
        if (state == newState)
            return
        PrismLib.command(arrayOf(if (newState) "playlist-shuffle" else "playlist-unshuffle"))
        PrismLib.setPropertyBoolean("shuffle", newState)
    }

    companion object {
        private const val TAG = "Prism"

        // mpv option `hwdec` is set to this
        private const val HWDECS = "mediacodec,mediacodec-copy"
    }
}
