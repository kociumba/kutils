package org.kociumba.kutils.client.mappings.tinyv2

interface MappingGetter {
    fun get(namespace: Int): String
    fun getRaw(namespace: Int): String
    fun getRawNames(): Array<String>
    fun getAllNames(): Array<String>
}