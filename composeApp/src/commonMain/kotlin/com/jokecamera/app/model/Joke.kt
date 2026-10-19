package com.jokecamera.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Joke(
    val id: Int,
    val type: JokeType,
    val setup: String,
    val punchline: String
)

@Serializable
data class JokeJson(
    val type: String = "general",
    val setup: String,
    val punchline: String
) {
    fun toJoke(id: Int): Joke = Joke(
        id = id,
        type = JokeType.fromString(type),
        setup = setup,
        punchline = punchline
    )
}
