package org.kociumba.kutils.client.utils

import org.kociumba.kutils.client.client

fun isHypixel(): Boolean {
    var name = client.server?.name
    if (name == null) return false
    if (name.lowercase().contains("hypixel")) return true
    return false
}