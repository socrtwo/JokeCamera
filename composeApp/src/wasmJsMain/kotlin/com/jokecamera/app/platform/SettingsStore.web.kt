package com.jokecamera.app.platform

import kotlinx.browser.window

class WebSettingsStore : SettingsStore {
    private val storage get() = window.localStorage

    override fun getString(key: String, default: String): String {
        return storage.getItem(key) ?: default
    }

    override fun putString(key: String, value: String) {
        storage.setItem(key, value)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return storage.getItem(key)?.toBooleanStrictOrNull() ?: default
    }

    override fun putBoolean(key: String, value: Boolean) {
        storage.setItem(key, value.toString())
    }

    override fun getFloat(key: String, default: Float): Float {
        return storage.getItem(key)?.toFloatOrNull() ?: default
    }

    override fun putFloat(key: String, value: Float) {
        storage.setItem(key, value.toString())
    }

    override fun getInt(key: String, default: Int): Int {
        return storage.getItem(key)?.toIntOrNull() ?: default
    }

    override fun putInt(key: String, value: Int) {
        storage.setItem(key, value.toString())
    }

    override fun getStringSet(key: String, default: Set<String>): Set<String> {
        val stored = storage.getItem(key) ?: return default
        return if (stored.isBlank()) default else stored.split("\u001F").toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        storage.setItem(key, value.joinToString("\u001F"))
    }

    override fun remove(key: String) {
        storage.removeItem(key)
    }
}

actual fun createSettingsStore(): SettingsStore = WebSettingsStore()
