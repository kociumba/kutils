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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class ScriptConfig(
    val enabledScripts: Set<String> = emptySet()
)

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
    private val configFile = File("config/kutils/enabled-modules.json")
    private var config = loadConfig()

    init {
        scriptsFolder.mkdirs()
    }

    private fun loadConfig(): ScriptConfig {
        return try {
            if (configFile.exists()) {
                Json.decodeFromString<ScriptConfig>(configFile.readText())
            } else {
                ScriptConfig()
            }
        } catch (e: Exception) {
            log.error("Failed to load script config: ${e.message}")
            ScriptConfig()
        }
    }

    private fun saveConfig() {
        try {
            val currentConfig = ScriptConfig(
                enabledScripts = scriptMetadata.filter { it.value.isEnabled }.keys.toSet()
            )
            configFile.writeText(Json.encodeToString(currentConfig))
        } catch (e: Exception) {
            log.error("Failed to save script config: ${e.message}")
        }
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
            val wasEnabled = config.enabledScripts.contains(file.name)
            val metadata = LuaScriptMetadata(
                fileName = file.name,
                displayName = file.nameWithoutExtension,
                lastModified = file.lastModified(),
                isEnabled = false  // Start with disabled state
            )
            scriptMetadata[file.name] = metadata
            
            // Enable scripts that were enabled in the previous session
            if (wasEnabled) {
                enableScript(file.name)  // This will set isEnabled to true if successful
            }
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
            saveConfig()  // Save config when a script is enabled
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
        saveConfig()  // Save config when a script is disabled
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
            try {
                if (!file.delete()) {
                    log.error("Failed to delete script file: $fileName")
                }
            } catch (e: Exception) {
                log.error("Error deleting script file $fileName: ${e.message}")
            }
        } else {
            log.warn("Script file not found for deletion: $fileName")
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
        
        if (!oldFile.exists()) {
            log.error("Cannot rename script: source file not found: $oldName")
            return
        }
        
        if (newFile.exists()) {
            log.error("Cannot rename script: destination file already exists: $newName")
            return
        }

        try {
            // Read the content of the old file
            val content = oldFile.readText()
            
            // Write content to the new file
            newFile.writeText(content)
            
            // Delete the old file only if writing to new file was successful
            if (!oldFile.delete()) {
                // If we can't delete the old file, delete the new one to avoid duplication
                newFile.delete()
                log.error("Failed to delete old script file during rename: $oldName")
                return
            }
            
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
        } catch (e: Exception) {
            // If anything goes wrong, try to clean up
            try {
                newFile.delete()
            } catch (cleanupError: Exception) {
                log.error("Error cleaning up after failed rename: ${cleanupError.message}")
            }
            log.error("Error renaming script from $oldName to $newName: ${e.message}")
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