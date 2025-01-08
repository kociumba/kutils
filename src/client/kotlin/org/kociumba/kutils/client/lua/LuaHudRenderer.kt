package org.kociumba.kutils.client.lua

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.kociumba.kutils.log

/**
 * Wrapper for HUD rendering callbacks from Lua
 *
 * This is a very leaky abstraction, but has to be done this way to stop spamming callback errors 🤷
 */
class LuaHudRenderer(private val scriptId: String? = null, private val scriptVersion: Int = 0) : LuaFunction() {
    companion object {
        // Keep track of which script owns which callback
        private val scriptCallbacks = mutableMapOf<String, Boolean>()
        // Keep track of current version for each script
        private val scriptVersions = mutableMapOf<String, Int>()

        fun register(globals: Globals, scriptId: String? = null) {
            if (scriptId != null) {
                // Increment version when registering a new script instance
                val newVersion = (scriptVersions[scriptId] ?: 0) + 1
                scriptVersions[scriptId] = newVersion
                globals.set("registerHudRenderer", LuaHudRenderer(scriptId, newVersion))
            } else {
                globals.set("registerHudRenderer", LuaHudRenderer(null))
            }
        }

        fun setScriptEnabled(scriptId: String, enabled: Boolean) {
            scriptCallbacks[scriptId] = enabled
        }

        fun removeScript(scriptId: String) {
            scriptCallbacks.remove(scriptId)
        }
    }

    override fun call(luaCallback: LuaValue): LuaValue {
        if (luaCallback.isfunction() && scriptId != null) {
            val callback = HudRenderCallback { drawContext: DrawContext, tickCounter: RenderTickCounter ->
                // Only execute if script is enabled AND this is the current version
                if (scriptCallbacks[scriptId] == true && scriptVersions[scriptId] == scriptVersion) {
                    try {
                        luaCallback.checkfunction().call(
                            CoerceJavaToLua.coerce(drawContext),
                            CoerceJavaToLua.coerce(tickCounter)
                        )
                    } catch (e: Exception) {
                        // Only log errors if this is the current version of the script
                        if (scriptVersions[scriptId] == scriptVersion) {
                            log.error("Error in Lua HUD render callback for script $scriptId: ${e.message}")
                        }
                    }
                }
            }
            
            // Register the callback - we can't unregister it, but we can make it no-op
            HudRenderCallback.EVENT.register(callback)
        }
        return NIL
    }
}