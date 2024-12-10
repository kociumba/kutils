package org.kociumba.kutils.client


fun McKeyMap.value(): Int = this.value

/**
 * These are so fucked up and unhinged that I had to make this to never forget them
 */
enum class McKeyMap(val value: Int) {
    ESCAPE(256),
    ENTER(257),
}