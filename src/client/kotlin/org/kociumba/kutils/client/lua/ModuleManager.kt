package org.kociumba.kutils.client.lua

import com.github.only52607.luakt.lib.LuaKotlinExLib
import com.github.only52607.luakt.lib.LuaKotlinLib
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.Kutils
import org.kociumba.kutils.client.KutilsClassLoader
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import org.kociumba.kutils.log
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

// TODO: Make sure reloading and disabling works
//  labels: enhancement
class ModuleManager(private val client: MinecraftClient) {
    private val scriptContexts = mutableMapOf<String, ScriptContext>()
    private val scriptMetadata = mutableMapOf<String, LuaScriptMetadata>()
    private val scriptsFolder = File("config/kutils/lua/")

    init {
        scriptsFolder.mkdirs()
    }

    data class LuaScriptMetadata(
        val fileName: String,
        val displayName: String,
        var isEnabled: Boolean = false,
        var lastModified: Long = 0,
        var cleanupCallback: LuaFunction? = null
    )

    inner class ScriptContext(val metadata: LuaScriptMetadata) {
        val globals: Globals = JsePlatform.standardGlobals()
        private var isInitialized = false

        fun initialize() {
            if (isInitialized) return

            try {
                globals.load(LuaKotlinLib())
                globals.load(LuaKotlinExLib())
                KutilsClassLoader.register(globals, Kutils::class.java.classLoader)
                LuaLogger.register(globals)
                MainThreadExecutor.register(globals, client)

                // Add cleanup callback registration
                globals.set("onDisable", object : OneArgFunction() {
                    override fun call(callback: LuaValue): LuaValue {
                        if (callback.isfunction()) {
                            metadata.cleanupCallback = callback.checkfunction()
                        }
                        return NIL
                    }
                })

                // Add enabled state check
                globals.set("isEnabled", object : ZeroArgFunction() {
                    override fun call(): LuaValue = valueOf(metadata.isEnabled)
                })

                LuaHudRenderer.register(globals, metadata.fileName)

                isInitialized = true
            } catch (e: Exception) {
                log.error("Failed to initialize script context for ${metadata.fileName}: ${e.message}")
            }
        }

        fun cleanup() {
            try {
                // Call cleanup callback if registered
                metadata.cleanupCallback?.call()
            } catch (e: Exception) {
                log.error("Error during cleanup of script ${metadata.fileName}: ${e.message}")
            }

            // Remove from HUD renderer
            LuaHudRenderer.removeScript(metadata.fileName)
            metadata.isEnabled = false
        }
    }

    fun loadScripts() {
        scriptsFolder.listFiles { file -> file.extension == "lua" }?.forEach { file ->
            val metadata = LuaScriptMetadata(
                fileName = file.name,
                displayName = file.nameWithoutExtension,
                lastModified = file.lastModified()
            )
            scriptMetadata[file.name] = metadata
        }
    }

    fun enableScript(fileName: String) {
        val metadata = scriptMetadata[fileName] ?: return
        if (metadata.isEnabled) return

        try {
            val context = ScriptContext(metadata).apply { initialize() }
            scriptContexts[fileName] = context

            val scriptFile = File(scriptsFolder, fileName)
            context.globals.load(scriptFile.reader(), fileName).call()
            metadata.isEnabled = true
            LuaHudRenderer.setScriptEnabled(fileName, true)
        } catch (e: Exception) {
            println("Failed to enable script $fileName: ${e.message}")
            disableScript(fileName)
        }
    }

    fun disableScript(fileName: String) {
        scriptContexts[fileName]?.cleanup()
        scriptContexts.remove(fileName)
        LuaHudRenderer.setScriptEnabled(fileName, false)
        scriptMetadata[fileName]?.isEnabled = false
    }

    fun disableAllScripts() {
        scriptMetadata.keys.toList().forEach { fileName ->
            disableScript(fileName)
        }
    }

    fun reloadScript(fileName: String) {
        disableScript(fileName)
        enableScript(fileName)
    }

    // Getters for UI
    fun getScriptMetadata(): Map<String, LuaScriptMetadata> = scriptMetadata.toMap()
    fun getScriptContent(fileName: String): String =
        File(scriptsFolder, fileName).takeIf { it.exists() }?.readText() ?: ""
    fun saveScriptContent(fileName: String, content: String) {
        File(scriptsFolder, fileName).writeText(content)
        if (scriptMetadata[fileName]?.isEnabled == true) {
            reloadScript(fileName)
        }
    }
}