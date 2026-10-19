package com.jokecamera.app.platform

import java.util.prefs.Preferences

class DesktopSettingsStore : SettingsStore {
    private val prefs = Preferences.userRoot().node("com/jokecamera/app")

    override fun getString(key: String, default: String): String = prefs.get(key, default)
    override fun putString(key: String, value: String) { prefs.put(key, value); prefs.flush() }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.putBoolean(key, value); prefs.flush() }
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun putFloat(key: String, value: Float) { prefs.putFloat(key, value); prefs.flush() }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) { prefs.putInt(key, value); prefs.flush() }

    override fun getStringSet(key: String, default: Set<String>): Set<String> {
        val stored = prefs.get(key, "")
        return if (stored.isBlank()) default else stored.split("\u001F").toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        prefs.put(key, value.joinToString("\u001F"))
        prefs.flush()
    }

    override fun remove(key: String) { prefs.remove(key); prefs.flush() }
}

actual fun createSettingsStore(): SettingsStore = DesktopSettingsStore()
