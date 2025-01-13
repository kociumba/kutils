package org.kociumba.kutils.client.mappings.tinyv2

data class TinyMetadata(
    val majorVersion: Int,
    val minorVersion: Int,
    val namespaces: List<String>,
    val properties: Map<String, String?>
)