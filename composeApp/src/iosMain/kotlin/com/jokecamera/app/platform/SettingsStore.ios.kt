package com.jokecamera.app.platform

import platform.Foundation.NSUserDefaults

class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, default: String): String {
        return defaults.stringForKey(key) ?: default
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
        defaults.synchronize()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
        defaults.synchronize()
    }

    override fun getFloat(key: String, default: Float): Float {
        return if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else default
    }

    override fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, key)
        defaults.synchronize()
    }

    override fun getInt(key: String, default: Int): Int {
        return if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default
    }

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
        defaults.synchronize()
    }

    override fun getStringSet(key: String, default: Set<String>): Set<String> {
        val array = defaults.stringArrayForKey(key) ?: return default
        @Suppress("UNCHECKED_CAST")
        return (array as List<String>).toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        defaults.setObject(value.toList(), key)
        defaults.synchronize()
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }
}

actual fun createSettingsStore(): SettingsStore = IosSettingsStore()
