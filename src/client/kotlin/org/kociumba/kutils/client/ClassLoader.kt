package org.kociumba.kutils.client

import net.fabricmc.loader.api.FabricLoader
import org.kociumba.kutils.client.lua.CustomDebugLib
import org.kociumba.kutils.client.mappings.MappingLoader
import org.kociumba.kutils.log
import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * This is an adapter class loader to enable lua the usage of already loaded classes through the minecraft class loader
 *
 * This is a very straight forward implementation in a dev env, since the class names are not obfuscated,
 * but in prod this has to remap clas names at runtime to allow for the same functionality
 */
class KutilsClassLoader(
    private val modClassLoader: ClassLoader,
    private val mappingLoader: MappingLoader,
    private var debug: Boolean = FabricLoader.getInstance().isDevelopmentEnvironment
) : LuaFunction() {

    override fun call(className: LuaValue): LuaValue {
        val obfuscatedName = if (debug) {
            className.checkjstring().apply { log.info(this) }
        } else {
            mappingLoader.getClass(className.checkjstring()) ?: className.checkjstring().apply { log.info(this) }
        }

        val cls = modClassLoader.loadClass(obfuscatedName).apply { log.info(this) }
        val coerced = CoerceJavaToLua.coerce(cls)
        val meta = LuaTable()

        // Handle method calls
        meta.set(INDEX, object : LuaFunction() {
            override fun call(self: LuaValue): LuaValue {
                return self
            }

            override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                // Create a wrapper for method calls that evaluates immediately
                return createMethodWrapper(self, arg)
            }
        })

        // Handle constructor calls
        meta.set("__call", object : LuaFunction() {
            override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                if (cls == Thread::class.java && arg.isfunction()) {
                    val runnable = object : Runnable {
                        override fun run() {
                            try {
                                arg.checkfunction().call() // the source of all errors xd
                            } catch (e: Exception) {
                                if (e !is CustomDebugLib.ScriptInterruptException && e !is InterruptedException) {
                                    log.warn("Error in thread", e)
                                }
                            }
                        }
                    }
                    return CoerceJavaToLua.coerce(Thread(runnable))
                }
                return handleConstructor(cls, arg)
            }
        })

        coerced.setmetatable(meta)
        return coerced
    }

    private fun createMethodWrapper(self: LuaValue, methodName: LuaValue): LuaValue {
        val methodWrapper = LuaTable()

        // Store the object and method name
        methodWrapper.set("self", self)
        methodWrapper.set("method", methodName)

        // Add metamethods
        val meta = LuaTable()

        // Handle toString
        meta.set("__tostring", object : LuaFunction() {
            override fun call(): LuaValue {
                val result = invokeMethod(
                    methodWrapper.get("self"),
                    methodWrapper.get("method").checkjstring()
                )
                return LuaString.valueOf(result?.toString() ?: "null")
            }
        })

        // Handle direct calls
        meta.set("__call", object : LuaFunction() {
            override fun call(): LuaValue {
                val result = invokeMethod(
                    methodWrapper.get("self"),
                    methodWrapper.get("method").checkjstring()
                )
                return CoerceJavaToLua.coerce(result)
            }
        })

        methodWrapper.setmetatable(meta)
        return methodWrapper
    }

    private fun invokeMethod(self: LuaValue, methodName: String): Any? {
        val obj = self.touserdata()
        if (obj != null) {
            val obfuscatedMethodName = if (debug) {
                methodName
            } else {
                mappingLoader.getMethod(obj.javaClass.name, methodName) ?: methodName
            }
            val method = obj.javaClass.methods.find { it.name == obfuscatedMethodName }
            if (method != null) {
                return method.invoke(obj)
            }
        }
        return null
    }

    private fun handleConstructor(cls: Class<*>, arg: LuaValue): LuaValue {
        return try {
            when {
                arg.isfunction() && cls == Thread::class.java -> {
                    val runnable = object : Runnable {
                        override fun run() {
                            try {
                                arg.checkfunction().call()
                            } catch (e: Exception) {
                                if (e !is CustomDebugLib.ScriptInterruptException && e !is InterruptedException) {
                                    log.error("Error in thread", e)
                                }
                            }
                        }
                    }
                    CoerceJavaToLua.coerce(Thread(runnable))
                }
                else -> {
                    CoerceJavaToLua.coerce(cls.getDeclaredConstructor().newInstance())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NIL
        }
    }

    companion object {
        fun register(
            globals: Globals,
            modClassLoader: ClassLoader,
            mappingLoader: MappingLoader,
            debug: Boolean = FabricLoader.getInstance().isDevelopmentEnvironment
        ) {
            globals.set("requireJVM", KutilsClassLoader(modClassLoader, mappingLoader, debug))
        }
    }
}