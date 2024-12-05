package org.kociumba.kutils.client.httpClient

import java.net.HttpURLConnection
import java.net.URI

/**
 * Little wrapper for http requests to not have to use ktor,
 * only wraps get right now couse I only need get right now
 */
object Http {
    /**
     * This isn't actually an env secret, it's just a spam and ddos protection
     */
    private const val KUTILS_PROXY_PASSWORD = "gabagool"

    fun get(uri: String, bearerToken: String? = null): String {
        val url = URI(uri).toURL()
        return with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            if (bearerToken != null) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (responseCode in 200..299) {
                inputStream.bufferedReader().use {
                    it.readText()
                }
            } else {
                errorStream.bufferedReader().use {
                    it.readText()
                }
            }
        }
    }

    /**
     * Shortcut to get from the kutils proxy
     */
    fun getProxy(uri: String): String {
        return get(uri, KUTILS_PROXY_PASSWORD)
    }
}