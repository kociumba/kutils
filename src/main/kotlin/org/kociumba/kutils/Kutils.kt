package org.kociumba.kutils

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

val log: Logger = LogManager.getLogger("kutils")

class Kutils : ModInitializer {

    override fun onInitialize() {
        log.info("initializing kutils...")
    }
}
