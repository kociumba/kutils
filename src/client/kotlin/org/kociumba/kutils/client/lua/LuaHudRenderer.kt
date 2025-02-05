package org.kociumba.kutils.client.lua

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import org.kociumba.kutils.log
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class LuaHudRenderer(
    private val scriptId: String? = null,
    private val scriptVersion: Int = 0,
    private val globals: Globals
) : LuaFunction() {
    companion object {
        private val scriptCallbacks = mutableMapOf<String, Boolean>()
        private val scriptVersions = mutableMapOf<String, Int>()

        fun register(globals: Globals, scriptId: String? = null) {
            if (scriptId != null) {
                val newVersion = (scriptVersions[scriptId] ?: 0) + 1
                scriptVersions[scriptId] = newVersion
                globals.set("registerHudRenderer", LuaHudRenderer(scriptId, newVersion, globals))
            } else {
                globals.set("registerHudRenderer", LuaHudRenderer(null, 0, globals))
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
                if (scriptCallbacks[scriptId] == true && scriptVersions[scriptId] == scriptVersion) {
                    try {
                        // actually insane coding, I love it xd
                        val requireJVM = globals.get("requireJVM")

                        val wrappedDrawContext = requireJVM.call(LuaString.valueOf("net.minecraft.client.gui.DrawContext"))
                            .apply {
                                getmetatable().set("__instance", CoerceJavaToLua.coerce(drawContext))
                            }

                        val wrappedTickCounter = requireJVM.call(LuaString.valueOf("net.minecraft.client.render.RenderTickCounter"))
                            .apply {
                                getmetatable().set("__instance", CoerceJavaToLua.coerce(tickCounter))
                            }

                        luaCallback.checkfunction().call(
                            wrappedDrawContext,
                            wrappedTickCounter
                        )
                    } catch (e: Exception) {
                        if (scriptVersions[scriptId] == scriptVersion) {
                            log.error("Error in Lua HUD render callback for module $scriptId: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }

            HudRenderCallback.EVENT.register(callback)
        }
        return NIL
    }
}