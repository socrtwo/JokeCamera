package com.jokecamera.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JokeType {
    @SerialName("dad") DAD,
    @SerialName("general") GENERAL,
    @SerialName("knock-knock") KNOCK_KNOCK,
    @SerialName("programming") PROGRAMMING,
    @SerialName("custom") CUSTOM;

    fun displayName(): String = when (this) {
        DAD -> "Dad Jokes"
        GENERAL -> "General"
        KNOCK_KNOCK -> "Knock-Knock"
        PROGRAMMING -> "Programming"
        CUSTOM -> "Custom"
    }

    fun toJsonString(): String = when (this) {
        DAD -> "dad"
        GENERAL -> "general"
        KNOCK_KNOCK -> "knock-knock"
        PROGRAMMING -> "programming"
        CUSTOM -> "custom"
    }

    companion object {
        fun fromString(value: String): JokeType {
            return try {
                entries.firstOrNull { it.toJsonString() == value.lowercase() }
                    ?: valueOf(value.uppercase().replace("-", "_").replace(" ", "_"))
            } catch (e: IllegalArgumentException) {
                CUSTOM
            }
        }
    }
}
