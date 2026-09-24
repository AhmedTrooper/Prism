package com.ahmedtrooper.prism

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import `is`.xyz.mpv.MPVLib

// Wrapper for native library

@Suppress("unused")
object PrismLib {
    init {
        // Forward native callbacks from MPVLib JNI bridge to PrismLib observers
        MPVLib.addObserver(object : MPVLib.EventObserver {
            override fun eventProperty(property: String) {
                PrismLib.eventProperty(property)
            }
            override fun eventProperty(property: String, value: Long) {
                PrismLib.eventProperty(property, value)
            }
            override fun eventProperty(property: String, value: Boolean) {
                PrismLib.eventProperty(property, value)
            }
            override fun eventProperty(property: String, value: String) {
                PrismLib.eventProperty(property, value)
            }
            override fun eventProperty(property: String, value: Double) {
                PrismLib.eventProperty(property, value)
            }
            override fun event(eventId: Int) {
                PrismLib.event(eventId)
            }
        })

        MPVLib.addLogObserver(object : MPVLib.LogObserver {
            override fun logMessage(prefix: String, level: Int, text: String) {
                PrismLib.logMessage(prefix, level, text)
            }
        })
    }

    fun create(appctx: Context) = MPVLib.create(appctx)
    fun init() = MPVLib.init()
    fun destroy() = MPVLib.destroy()
    fun attachSurface(surface: Surface) = MPVLib.attachSurface(surface)
    fun detachSurface() = MPVLib.detachSurface()

    fun command(cmd: Array<out String>) = MPVLib.command(cmd)

    fun setOptionString(name: String, value: String): Int = MPVLib.setOptionString(name, value)

    fun grabThumbnail(dimension: Int): Bitmap? = MPVLib.grabThumbnail(dimension)

    fun getPropertyInt(property: String): Int? = MPVLib.getPropertyInt(property)
    fun setPropertyInt(property: String, value: Int) = MPVLib.setPropertyInt(property, value)
    fun getPropertyDouble(property: String): Double? = MPVLib.getPropertyDouble(property)
    fun setPropertyDouble(property: String, value: Double) = MPVLib.setPropertyDouble(property, value)
    fun getPropertyBoolean(property: String): Boolean? = MPVLib.getPropertyBoolean(property)
    fun setPropertyBoolean(property: String, value: Boolean) = MPVLib.setPropertyBoolean(property, value)
    fun getPropertyString(property: String): String? = MPVLib.getPropertyString(property)
    fun setPropertyString(property: String, value: String) = MPVLib.setPropertyString(property, value)

    fun observeProperty(property: String, format: Int) = MPVLib.observeProperty(property, format)

    private val observers = mutableListOf<EventObserver>()

    @JvmStatic
    fun addObserver(o: EventObserver) {
        synchronized(observers) {
            observers.add(o)
        }
    }

    @JvmStatic
    fun removeObserver(o: EventObserver) {
        synchronized(observers) {
            observers.remove(o)
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Long) {
        synchronized(observers) {
            for (o in observers)
                o.eventProperty(property, value)
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Boolean) {
        synchronized(observers) {
            for (o in observers)
                o.eventProperty(property, value)
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Double) {
        synchronized(observers) {
            for (o in observers)
                o.eventProperty(property, value)
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: String) {
        synchronized(observers) {
            for (o in observers)
                o.eventProperty(property, value)
        }
    }

    @JvmStatic
    fun eventProperty(property: String) {
        synchronized(observers) {
            for (o in observers)
                o.eventProperty(property)
        }
    }

    @JvmStatic
    fun event(eventId: Int) {
        synchronized(observers) {
            for (o in observers)
                o.event(eventId)
        }
    }

    private val log_observers = mutableListOf<LogObserver>()

    @JvmStatic
    fun addLogObserver(o: LogObserver) {
        synchronized(log_observers) {
            log_observers.add(o)
        }
    }

    @JvmStatic
    fun removeLogObserver(o: LogObserver) {
        synchronized(log_observers) {
            log_observers.remove(o)
        }
    }

    @JvmStatic
    fun logMessage(prefix: String, level: Int, text: String) {
        synchronized(log_observers) {
            for (o in log_observers)
                o.logMessage(prefix, level, text)
        }
    }

    interface EventObserver {
        fun eventProperty(property: String)
        fun eventProperty(property: String, value: Long)
        fun eventProperty(property: String, value: Boolean)
        fun eventProperty(property: String, value: String)
        fun eventProperty(property: String, value: Double)
        fun event(eventId: Int)
    }

    interface LogObserver {
        fun logMessage(prefix: String, level: Int, text: String)
    }

    object MpvFormat {
        const val MPV_FORMAT_NONE: Int = 0
        const val MPV_FORMAT_STRING: Int = 1
        const val MPV_FORMAT_OSD_STRING: Int = 2
        const val MPV_FORMAT_FLAG: Int = 3
        const val MPV_FORMAT_INT64: Int = 4
        const val MPV_FORMAT_DOUBLE: Int = 5
        const val MPV_FORMAT_NODE: Int = 6
        const val MPV_FORMAT_NODE_ARRAY: Int = 7
        const val MPV_FORMAT_NODE_MAP: Int = 8
        const val MPV_FORMAT_BYTE_ARRAY: Int = 9
    }

    object MpvEvent {
        const val MPV_EVENT_NONE: Int = 0
        const val MPV_EVENT_SHUTDOWN: Int = 1
        const val MPV_EVENT_LOG_MESSAGE: Int = 2
        const val MPV_EVENT_GET_PROPERTY_REPLY: Int = 3
        const val MPV_EVENT_SET_PROPERTY_REPLY: Int = 4
        const val MPV_EVENT_COMMAND_REPLY: Int = 5
        const val MPV_EVENT_START_FILE: Int = 6
        const val MPV_EVENT_END_FILE: Int = 7
        const val MPV_EVENT_FILE_LOADED: Int = 8
        @Deprecated("")
        const val MPV_EVENT_IDLE: Int = 11
        @Deprecated("")
        const val MPV_EVENT_TICK: Int = 14
        const val MPV_EVENT_CLIENT_MESSAGE: Int = 16
        const val MPV_EVENT_VIDEO_RECONFIG: Int = 17
        const val MPV_EVENT_AUDIO_RECONFIG: Int = 18
        const val MPV_EVENT_SEEK: Int = 20
        const val MPV_EVENT_PLAYBACK_RESTART: Int = 21
        const val MPV_EVENT_PROPERTY_CHANGE: Int = 22
        const val MPV_EVENT_QUEUE_OVERFLOW: Int = 24
        const val MPV_EVENT_HOOK: Int = 25
    }

    object MpvLogLevel {
        const val MPV_LOG_LEVEL_NONE: Int = 0
        const val MPV_LOG_LEVEL_FATAL: Int = 10
        const val MPV_LOG_LEVEL_ERROR: Int = 20
        const val MPV_LOG_LEVEL_WARN: Int = 30
        const val MPV_LOG_LEVEL_INFO: Int = 40
        const val MPV_LOG_LEVEL_V: Int = 50
        const val MPV_LOG_LEVEL_DEBUG: Int = 60
        const val MPV_LOG_LEVEL_TRACE: Int = 70
    }
}
