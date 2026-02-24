package com.jokecamera.app.platform

import android.content.Context
import android.content.SharedPreferences

private var appContext: Context? = null

fun initAndroidContext(context: Context) {
    appContext = context.applicationContext
}

class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("jokecamera_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun getStringSet(key: String, default: Set<String>): Set<String> = prefs.getStringSet(key, default) ?: default
    override fun putStringSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

actual fun createSettingsStore(): SettingsStore {
    val ctx = appContext ?: throw IllegalStateException("Android context not initialized. Call initAndroidContext() first.")
    return AndroidSettingsStore(ctx)
}
