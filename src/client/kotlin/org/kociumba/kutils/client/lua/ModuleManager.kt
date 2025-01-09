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
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

// thanks to the single stack overflow thread about this
class CustomDebugLib : DebugLib() {
    @Volatile
    var interrupted = false

    override fun onInstruction(pc: Int, v: Varargs?, top: Int) {
        if (interrupted) {
            throw ScriptInterruptException()
        }
        super.onInstruction(pc, v, top)
    }

    class ScriptInterruptException : RuntimeException("Script execution interrupted")
}

// ass good as this is going to get
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
        private var debugLib: CustomDebugLib = CustomDebugLib()
        var globals: Globals? = JsePlatform.debugGlobals().apply {
            load(debugLib)
        }
        private var isInitialized = false

        fun initialize() {
            if (isInitialized) return
            debugLib.interrupted = false

            try {
                globals?.let { g ->
                    g.load(LuaKotlinLib())
                    g.load(LuaKotlinExLib())
                    KutilsClassLoader.register(g, Kutils::class.java.classLoader)
                    LuaLogger.register(g)
                    MainThreadExecutor.register(g, client)

                    // Add cleanup callback registration
                    g.set("onDisable", object : OneArgFunction() {
                        override fun call(callback: LuaValue): LuaValue {
                            if (callback.isfunction()) {
                                metadata.cleanupCallback = callback.checkfunction()
                            }
                            return NIL
                        }
                    })

                    // Add enabled state check
                    g.set("isEnabled", object : ZeroArgFunction() {
                        override fun call(): LuaValue = valueOf(metadata.isEnabled)
                    })

                    LuaHudRenderer.register(g, metadata.fileName)

                    isInitialized = true
                }
            } catch (e: Exception) {
                log.error("Failed to initialize script context for ${metadata.fileName}: ${e.message}")
            }
        }

        fun cleanup() {
            try {
                try {
                    metadata.cleanupCallback?.call()
                    debugLib.interrupted = true
                } catch (e: Exception) {
                    log.error("Error during cleanup callback of script ${metadata.fileName}: ${e.message}")
                }

                // Clear all references
                globals = null
                isInitialized = false

            } catch (e: Exception) {
                if (e !is CustomDebugLib.ScriptInterruptException && e !is InterruptedException) {
                    log.error("Error during cleanup of script ${metadata.fileName}: ${e.message}")
                }
            }

            // Remove from HUD renderer
            LuaHudRenderer.removeScript(metadata.fileName)
            metadata.isEnabled = false
            metadata.cleanupCallback = null
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
            context.globals?.load(scriptFile.reader(), fileName)?.call()
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

    fun createScript(fileName: String) {
        val file = File(scriptsFolder, fileName)
        if (!file.exists()) {
            file.writeText("""-- New Lua script
local log = createLogger("${fileName.removeSuffix(".lua")}")
log.info("Script initialized!")

-- Register cleanup callback
onDisable(function()
    log.info("Script disabled!")
end)
""")
            val metadata = LuaScriptMetadata(
                fileName = fileName,
                displayName = fileName.removeSuffix(".lua"),
                lastModified = file.lastModified()
            )
            scriptMetadata[fileName] = metadata
        }
    }

    fun deleteScript(fileName: String) {
        // First disable the script if it's running
        if (scriptMetadata[fileName]?.isEnabled == true) {
            disableScript(fileName)
        }
        
        // Remove the script file
        val file = File(scriptsFolder, fileName)
        if (file.exists()) {
            file.delete()
        }
        
        // Remove from metadata
        scriptMetadata.remove(fileName)
    }

    fun renameScript(oldName: String, newName: String) {
        // First disable the script if it's running
        val wasEnabled = scriptMetadata[oldName]?.isEnabled == true
        if (wasEnabled) {
            disableScript(oldName)
        }
        
        // Rename the file
        val oldFile = File(scriptsFolder, oldName)
        val newFile = File(scriptsFolder, newName)
        if (oldFile.exists() && !newFile.exists()) {
            oldFile.renameTo(newFile)
            
            // Update metadata
            scriptMetadata[oldName]?.let { oldMetadata ->
                val newMetadata = oldMetadata.copy(
                    fileName = newName,
                    displayName = newName.removeSuffix(".lua")
                )
                scriptMetadata.remove(oldName)
                scriptMetadata[newName] = newMetadata
                
                // Re-enable if it was enabled
                if (wasEnabled) {
                    enableScript(newName)
                }
            }
        }
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