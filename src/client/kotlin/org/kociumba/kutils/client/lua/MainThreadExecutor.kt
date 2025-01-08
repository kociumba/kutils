package org.kociumba.kutils.client.lua

import net.minecraft.client.MinecraftClient
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue

/**
 * Allows to simply make sure arbitrary lua code is running on the main thread
 */
class MainThreadExecutor(private val client: MinecraftClient) : LuaFunction() {
    override fun call(luaFunc: LuaValue): LuaValue {
        if (luaFunc.isfunction()) {
            client.execute {
                try {
                    luaFunc.checkfunction().call()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return NIL
    }

    companion object {
        fun register(globals: Globals, client: MinecraftClient) {
            globals.set("runOnMain", MainThreadExecutor(client))
        }
    }
}