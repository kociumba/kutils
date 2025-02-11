package org.kociumba.kutils.client.utils

import net.fabricmc.loader.api.FabricLoader
import org.kociumba.kutils.client.modID
import org.kociumba.kutils.log
import java.net.URI
import kotlin.jvm.optionals.getOrNull

fun checkKutilsUpdates() {
    Thread {
        try {
            val remoteVersionURL = "https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/gradle.properties"
//            val remoteVersionURL =
//                "https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/test-version-detection"
            var remoteVersion = ""
            val version = FabricLoader.getInstance().getModContainer(modID)
                .getOrNull()?.metadata?.version?.friendlyString
            if (version == null) throw Exception("An error occurred while getting the current mod version")

            var remoteProperties = URI.create(remoteVersionURL).toURL().openStream()
            remoteProperties.bufferedReader().forEachLine {
                if (it.startsWith("mod_version=")) {
                    remoteVersion = it.substringAfter("=").trim()
                    if (remoteVersion == "") throw Exception("An error occurred while getting the upstream version")
                    var remote = KutilsVersion.fromString(remoteVersion)
                    var local = KutilsVersion.fromString(version)
                    if (remote > local) {
                        var modrinthURL = "https://modrinth.com/mod/kutils/version/${remoteVersion}"
                        chatInfoLink(
                            "A new version of kutils is available! kutils-${remoteVersion}, click on this message to open the modrinth page",
                            modrinthURL
                        )
                    }
                }
            }
        } catch (e: Exception) {
            chatError("${e.message}, for more info look in the minecraft log")
            log.error(e)
        }
    }.start()
}