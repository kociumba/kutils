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
class LuaHudRenderer : LuaFunction() {
    override fun call(luaCallback: LuaValue): LuaValue {
        if (luaCallback.isfunction()) {
            HudRenderCallback.EVENT.register { drawContext: DrawContext, tickCounter: RenderTickCounter ->
                try {
                    luaCallback.checkfunction().call(
                        CoerceJavaToLua.coerce(drawContext),
                        CoerceJavaToLua.coerce(tickCounter)
                    )
                } catch (e: Exception) {
                    println("Error in Lua HUD render callback: ${e.message}")
                }
            }
        }
        return NIL
    }

    companion object {
        fun register(globals: Globals) {
            globals.set("registerHudRenderer", LuaHudRenderer())
        }
    }
}