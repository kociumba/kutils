package org.kociumba.kmod

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

val log: Logger = LogManager.getLogger("kociumba/kmod")

class Kmod : ModInitializer {

    override fun onInitialize() {
        log.info("initializing kmod")
    }
}
