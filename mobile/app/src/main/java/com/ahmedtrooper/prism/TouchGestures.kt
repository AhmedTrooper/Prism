package com.ahmedtrooper.prism

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.*

enum class PropertyChange {
    Init,
    Seek,
    Volume,
    Bright,
    Finalize,

    /* Tap gestures */
    SingleTap,
    SeekFixed,
    PlayPause,
    Custom,

    /* Zoom and Pan gestures */
    Zoom,
    Pan,
    ResetZoom,
}

internal interface TouchGesturesObserver {
    fun onPropertyChange(p: PropertyChange, diff: Float, extra: Float = 0f)
}

internal class TouchGestures(
    private val context: Context,
    private val observer: TouchGesturesObserver
) {

    private enum class State {
        Up,
        Down,
        ControlSeek,
        ControlVolume,
        ControlBright,
    }

    private var state = State.Up
    // relevant movement direction for the current state (0=H, 1=V)
    private var stateDirection = 0

    // timestamp of the last tap (ACTION_UP)
    private var lastTapTime = 0L
    // when the current gesture began
    private var lastDownTime = 0L

    // where user initially placed their finger (ACTION_DOWN)
    private var initialPos = PointF()
    // last non-throttled processed position
    private var lastPos = PointF()

    private var width = 0f
    private var height = 0f
    // minimum movement which triggers a Control state
    private var trigger = 0f

    // which property change should be invoked where
    private var gestureHoriz = State.Down
    private var gestureVertLeft = State.Down
    private var gestureVertRight = State.Down
    private var tapGestureLeft: PropertyChange? = null
    private var tapGestureCenter: PropertyChange? = null
    private var tapGestureRight: PropertyChange? = null

    // Two-finger scale & pan
    private var isMultiTouch = false
    private var lastSpan = 0f
    private val lastCenter = PointF()
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            handler.removeCallbacks(singleTapRunnable)
            lastSpan = detector.currentSpan
            if (state != State.Up && state != State.Down) {
                sendPropertyChange(PropertyChange.Finalize, 0f)
            }
            state = State.Up
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            if (factor > 0f && !factor.isNaN() && !factor.isInfinite()) {
                sendPropertyChange(PropertyChange.Zoom, factor)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // Double-tap-like pinch that didn't change: treat as zoom reset if user did a quick two-finger tap
            val spanDelta = kotlin.math.abs(detector.currentSpan - lastSpan)
            if (spanDelta < 12f && detector.timeDelta < 300) {
                // Heuristic: quick small pinch -> reset request (alternative explicit gesture)
                // Do not auto-fire; only if external double-tap handler wants it we expose via ResetZoom
            }
        }
    })

    // Single-tap delayed dispatcher
    private val handler = Handler(Looper.getMainLooper())
    private val singleTapRunnable = Runnable {
        sendPropertyChange(PropertyChange.SingleTap, 0f)
    }

    private fun checkFloat(vararg n: Float): Boolean {
        return !n.any { it.isInfinite() || it.isNaN() }
    }
    private fun assertFloat(vararg n: Float) {
        if (!checkFloat(*n))
            throw IllegalArgumentException()
    }

    fun setMetrics(width: Float, height: Float) {
        assertFloat(width, height)
        this.width = width
        this.height = height
        trigger = min(width, height) / TRIGGER_RATE
    }

    companion object {
        private const val TAG = "Prism"

        // ratio for trigger, 1/Xth of minimum dimension
        // for tap gestures this is the distance that must *not* be moved for it to trigger
        private const val TRIGGER_RATE = 30

        // maximum duration between taps (ms) for a double tap to count
        private const val TAP_DURATION = 300L

        // full sweep from left side to right side is 2:30
        private const val CONTROL_SEEK_MAX = 150f

        // same as below, we rescale it inside PlayerActivity
        private const val CONTROL_VOLUME_MAX = 1.5f

        // brightness is scaled 0..1; max's not 1f so that user does not have to start from the bottom
        // if they want to go from none to full brightness
        private const val CONTROL_BRIGHT_MAX = 1.5f

        // do not trigger on X% of screen top/bottom
        // this is so that user can open android status bar
        private const val DEADZONE = 5
    }

    private fun processMovement(p: PointF): Boolean {
        if (isMultiTouch) return false

        // throttle events: only send updates when there's some movement compared to last update
        if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() < trigger / 3)
            return false
        lastPos.set(p)

        assertFloat(initialPos.x, initialPos.y)
        val dx = p.x - initialPos.x
        val dy = p.y - initialPos.y
        val dr = if (stateDirection == 0) (dx / width) else (-dy / height)

        when (state) {
            State.Up -> {}
            State.Down -> {
                // we might get into one of Control states if user moves enough
                if (abs(dx) > trigger) {
                    state = gestureHoriz
                    stateDirection = 0
                    handler.removeCallbacks(singleTapRunnable)
                } else if (abs(dy) > trigger) {
                    state = if (initialPos.x > width / 2) gestureVertRight else gestureVertLeft
                    stateDirection = 1
                    handler.removeCallbacks(singleTapRunnable)
                }
                // send Init so that it has a chance to cache values before we start modifying them
                if (state != State.Down)
                    sendPropertyChange(PropertyChange.Init, 0f)
            }
            State.ControlSeek ->
                sendPropertyChange(PropertyChange.Seek, CONTROL_SEEK_MAX * dr)
            State.ControlVolume ->
                sendPropertyChange(PropertyChange.Volume, CONTROL_VOLUME_MAX * dr)
            State.ControlBright ->
                sendPropertyChange(PropertyChange.Bright, CONTROL_BRIGHT_MAX * dr)
        }
        return state != State.Up && state != State.Down
    }

    private fun sendPropertyChange(p: PropertyChange, diff: Float, extra: Float = 0f) {
        observer.onPropertyChange(p, diff, extra)
    }

    fun syncSettings(prefs: SharedPreferences, resources: Resources) {
        val get: (String, Int) -> String = { key, defaultRes ->
            val v = prefs.getString(key, "")
            if (v.isNullOrEmpty()) resources.getString(defaultRes) else v
        }
        val map = mapOf(
            "bright" to State.ControlBright,
            "seek" to State.ControlSeek,
            "volume" to State.ControlVolume
        )
        val map2 = mapOf(
            "seek" to PropertyChange.SeekFixed,
            "playpause" to PropertyChange.PlayPause,
            "custom" to PropertyChange.Custom
        )

        gestureHoriz = map[get("gesture_horiz", R.string.pref_gesture_horiz_default)] ?: State.Down
        gestureVertLeft = map[get("gesture_vert_left", R.string.pref_gesture_vert_left_default)] ?: State.Down
        gestureVertRight = map[get("gesture_vert_right", R.string.pref_gesture_vert_right_default)] ?: State.Down
        tapGestureLeft = map2[get("gesture_tap_left", R.string.pref_gesture_tap_left_default)]
        tapGestureCenter = map2[get("gesture_tap_center", R.string.pref_gesture_tap_center_default)]
        tapGestureRight = map2[get("gesture_tap_right", R.string.pref_gesture_tap_right_default)]
    }

    fun onTouchEvent(e: MotionEvent): Boolean {
        if (width < 1 || height < 1) {
            Log.w(TAG, "TouchGestures: width or height not set!")
            return false
        }
        if (!checkFloat(e.x, e.y)) {
            Log.w(TAG, "TouchGestures: ignoring invalid point ${e.x} ${e.y}")
            return false
        }

        // Multi-touch pinch scale detector
        if (e.pointerCount >= 2) {
            scaleDetector.onTouchEvent(e)
            val cx = (e.getX(0) + e.getX(1)) / 2f
            val cy = (e.getY(0) + e.getY(1)) / 2f
            if (isMultiTouch) {
                val dx = (cx - lastCenter.x) / width
                val dy = (cy - lastCenter.y) / height
                if (abs(dx) > 0.001f || abs(dy) > 0.001f) {
                    sendPropertyChange(PropertyChange.Pan, dx, dy)
                }
            }
            lastCenter.set(cx, cy)
            isMultiTouch = true
            handler.removeCallbacks(singleTapRunnable)
            return true
        }

        if (isMultiTouch && e.pointerCount < 2) {
            isMultiTouch = false
            state = State.Up
            return true
        }

        var gestureHandled = false
        val point = PointF(e.x, e.y)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // deadzone on top/bottom
                if (e.y < height * DEADZONE / 100 || e.y > height * (100 - DEADZONE) / 100)
                    return false
                initialPos.set(point)
                lastPos.set(point)
                lastDownTime = SystemClock.uptimeMillis()
                state = State.Down
                gestureHandled = true
            }
            MotionEvent.ACTION_MOVE -> {
                gestureHandled = processMovement(point)
            }
            MotionEvent.ACTION_UP -> {
                val now = SystemClock.uptimeMillis()
                if (state == State.Down) {
                    val dist = PointF(lastPos.x - initialPos.x, lastPos.y - initialPos.y).length()
                    if (dist < trigger && now - lastDownTime < TAP_DURATION) {
                        if (now - lastTapTime < TAP_DURATION) {
                            // Double tap!
                            handler.removeCallbacks(singleTapRunnable)
                            lastTapTime = 0
                            if (point.x <= width * 0.32f) {
                                tapGestureLeft?.let { sendPropertyChange(it, -1f) }
                            } else if (point.x >= width * 0.68f) {
                                tapGestureRight?.let { sendPropertyChange(it, 1f) }
                            } else {
                                tapGestureCenter?.let { sendPropertyChange(it, 0f) }
                            }
                            gestureHandled = true
                        } else {
                            // First tap - wait to confirm single tap
                            lastTapTime = now
                            handler.removeCallbacks(singleTapRunnable)
                            handler.postDelayed(singleTapRunnable, 250L)
                            gestureHandled = true
                        }
                    }
                } else if (state != State.Up) {
                    sendPropertyChange(PropertyChange.Finalize, 0f)
                    gestureHandled = true
                }
                state = State.Up
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(singleTapRunnable)
                if (state != State.Up && state != State.Down) {
                    sendPropertyChange(PropertyChange.Finalize, 0f)
                }
                state = State.Up
            }
        }
        return gestureHandled
    }
}
