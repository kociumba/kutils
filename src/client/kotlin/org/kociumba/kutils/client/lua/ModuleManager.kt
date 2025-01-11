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

// as good as this is going to get
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

    /**
     * The actual backbone of the whole module system, holds and manages all the contexts and data
     */
    inner class ScriptContext(val metadata: LuaScriptMetadata) {
        private var debugLib: CustomDebugLib = CustomDebugLib()
        var globals: Globals? = JsePlatform.debugGlobals().apply {
            load(debugLib) // first always load the interrupt lib, couse without it there is no stopping the lua scripts
        }
        private var isInitialized = false

        fun initialize() {
            if (isInitialized) return
            debugLib.interrupted = false

            try {
                globals?.let { g ->
                    // these are not really needed since we provide basically all the infrastructure ourselves
                    g.load(LuaKotlinLib())
                    g.load(LuaKotlinExLib())

                    // the aforementioned our infrastructure 😎
                    KutilsClassLoader.register(g, Kutils::class.java.classLoader)
                    LuaLogger.register(g)
                    MainThreadExecutor.register(g, client)

                    g.set("onDisable", object : OneArgFunction() {
                        override fun call(callback: LuaValue): LuaValue {
                            if (callback.isfunction()) {
                                metadata.cleanupCallback = callback.checkfunction()
                            }
                            return NIL
                        }
                    })

                    g.set("isEnabled", object : ZeroArgFunction() {
                        override fun call(): LuaValue = valueOf(metadata.isEnabled)
                    })

                    LuaHudRenderer.register(g, metadata.fileName)

                    isInitialized = true
                }
            } catch (e: Exception) {
                log.error("Failed to initialize module context for ${metadata.fileName}: ${e.message}")
            }
        }

        fun cleanup() {
            try {
                try {
                    metadata.cleanupCallback?.call()
                    debugLib.interrupted = true
                } catch (e: Exception) {
                    log.error("Error during cleanup callback of module ${metadata.fileName}: ${e.message}")
                }

                // Clear all references
                globals = null
                isInitialized = false

            } catch (e: Exception) {
                if (e !is CustomDebugLib.ScriptInterruptException && e !is InterruptedException) {
                    log.error("Error during cleanup of module ${metadata.fileName}: ${e.message}")
                }
            }

            // Remove from HUD renderer, this system is flawed, but good enough
            // probably due for a rewrite in the future
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
                isEnabled = false
            )
            scriptMetadata[file.name] = metadata
            
            // Restore enabled state
            if (wasEnabled) {
                enableScript(file.name)
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
            saveConfig()
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
        saveConfig()
    }

    // should probably add like a "panic" button or something, that utilizes this
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
            file.writeText("""-- Use this template to create your own lua module!
-- You can find more info on this feature here: https://kociumba.gitbook.io/kutils/

-- Create a named logger
local log = createLogger("${fileName.removeSuffix(".lua")}")
log.info("${fileName.removeSuffix(".lua")} initialized!")

-- Register cleanup callback
onDisable(function()
    log.info("${fileName.removeSuffix(".lua")} disabled!")
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
        if (scriptMetadata[fileName]?.isEnabled == true) {
            disableScript(fileName)
        }

        val file = File(scriptsFolder, fileName)
        if (file.exists()) {
            try {
                if (!file.delete()) {
                    log.error("Failed to delete module file: $fileName")
                }
            } catch (e: Exception) {
                log.error("Error deleting module file $fileName: ${e.message}")
            }
        } else {
            log.warn("Module file not found for deletion: $fileName")
        }

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
            log.error("Cannot rename module: source file not found: $oldName")
            return
        }
        
        if (newFile.exists()) {
            log.error("Cannot rename module: destination file already exists: $newName")
            return
        }

        try {
            val content = oldFile.readText()

            newFile.writeText(content)

            if (!oldFile.delete()) {
                newFile.delete()
                log.error("Failed to delete old module file during rename: $oldName")
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
            log.error("Error renaming module from $oldName to $newName: ${e.message}")
        }
    }

    // Utils for UI
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