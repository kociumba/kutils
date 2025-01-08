package org.kociumba.kutils.client.lua

import org.luaj.vm2.LuaValue
import org.apache.logging.log4j.LogManager;
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable


class LuaLogger(private val loggerName: String) : LuaFunction() {
    private val log = LogManager.getLogger("kutils/lua")

    private inner class LogFunction(private val level: (String) -> Unit) : LuaFunction() {
        override fun call(msg: LuaValue): LuaValue {
            // Force immediate string conversion in current context
            val message = when {
                msg.isstring() -> msg.checkjstring()
                msg.isuserdata() -> msg.touserdata()?.toString()
                else -> msg.tojstring()
            }
            level("[LUA/$loggerName] $message")
            return NIL
        }
    }

    override fun call(): LuaValue {
        val table = LuaTable()
        table.set("info", LogFunction { msg -> log.info(msg) })
        table.set("warn", LogFunction { msg -> log.warn(msg) })
        table.set("error", LogFunction { msg -> log.error(msg) })
        table.set("debug", LogFunction { msg -> log.debug(msg) })
        return table
    }

    companion object {
        fun register(globals: Globals) {
            globals.set("createLogger", object : LuaFunction() {
                override fun call(name: LuaValue): LuaValue {
                    val loggerName = name.checkjstring()
                    return LuaLogger(loggerName).call()
                }
            })
        }
    }
}