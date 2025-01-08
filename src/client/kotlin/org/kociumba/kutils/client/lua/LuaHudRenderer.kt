package org.kociumba.kutils.client.lua

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Wrapper for HUD rendering callbacks from Lua
 */
class LuaHudRenderer(private val scriptId: String? = null) : LuaFunction() {
    companion object {
        // Keep track of which script owns which callback
        private val scriptCallbacks = mutableMapOf<String, Boolean>()

        fun register(globals: Globals, scriptId: String? = null) {
            globals.set("registerHudRenderer", LuaHudRenderer(scriptId))
        }

        fun setScriptEnabled(scriptId: String, enabled: Boolean) {
            scriptCallbacks[scriptId] = enabled
        }

        fun removeScript(scriptId: String) {
            scriptCallbacks.remove(scriptId)
        }
    }

    override fun call(luaCallback: LuaValue): LuaValue {
        if (luaCallback.isfunction()) {
            HudRenderCallback.EVENT.register { drawContext: DrawContext, tickCounter: RenderTickCounter ->
                // Only execute if script is enabled or if this is a global callback (scriptId == null)
                if (scriptId == null || scriptCallbacks[scriptId] == true) {
                    try {
                        luaCallback.checkfunction().call(
                            CoerceJavaToLua.coerce(drawContext),
                            CoerceJavaToLua.coerce(tickCounter)
                        )
                    } catch (e: Exception) {
                        println("Error in Lua HUD render callback${scriptId?.let { " for script $it" } ?: ""}: ${e.message}")
                    }
                }
            }
        }
        return NIL
    }
}