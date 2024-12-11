package org.kociumba.kutils

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

val log: Logger = LogManager.getLogger("kutils")

/**
 * log wrapper for use in java mixins, where log from above is not available
 */
object KutilsLogger {
    fun info(msg: String) = log.info(msg)
    fun warn(msg: String) = log.warn(msg)
}

class Kutils : ModInitializer {

    override fun onInitialize() {
        log.info("initializing kutils...")
    }
}
