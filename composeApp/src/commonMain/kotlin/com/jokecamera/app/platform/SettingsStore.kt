package com.jokecamera.app.platform

/**
 * Cross-platform key-value settings storage.
 * Implementations use SharedPreferences on Android, NSUserDefaults on iOS,
 * java.util.prefs.Preferences on Desktop, and localStorage on Web.
 */
interface SettingsStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getStringSet(key: String, default: Set<String>): Set<String>
    fun putStringSet(key: String, value: Set<String>)
    fun remove(key: String)
}

expect fun createSettingsStore(): SettingsStore
