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
        try {
            val inputName = className.checkjstring().replace('/', '.')

            val cls = if (debug) {
                // In dev - load plain name directly
                modClassLoader.loadClass(inputName).also {
                    mappingLoader.getClass(inputName)?.let { resolved ->
                        log.info("Debug: resolved $inputName -> $resolved")
                    }
                }
            } else {
                // In prod - try intermediary first, fallback to plain name
                val intermediaryName = mappingLoader.getClass(inputName)?.replace('/', '.')
                if (intermediaryName != null) {
                    modClassLoader.loadClass(intermediaryName)
                } else {
                    // Last resort - try plain name
                    log.warn("No mapping found for $inputName, attempting direct load")
                    modClassLoader.loadClass(inputName)
                }
            }

            val coerced = CoerceJavaToLua.coerce(cls)
            val meta = LuaTable()

            // Handle method calls
            meta.set(INDEX, object : LuaFunction() {
                override fun call(self: LuaValue): LuaValue {
                    return self
                }

                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    // Check if we're dealing with a static method call
                    if (self.isuserdata() && self.touserdata() is Class<*>) {
                        // Handle static method call
                        return createMethodWrapper(self, arg)
                    }
                    // Handle instance method call
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

        } catch (e: Exception) {
            log.error("Error loading a class", e)
            throw LuaError("Failed to load class: ${e.message}")
        }
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
        try {
            val obj = self.touserdata()
            if (obj != null) {
                val cls = when (obj) {
                    is Class<*> -> obj
                    else -> obj.javaClass
                }

                val methodToUse = if (debug) {
                    // In dev - use plain name and log resolved name
                    methodName.also {
                        mappingLoader.getMethod(cls.name, methodName)?.let { resolved ->
                            log.info("Debug: resolved method $methodName -> $resolved")
                        }
                    }
                } else {
                    // In prod - use intermediary name or fallback to plain
                    mappingLoader.getMethod(cls.name, methodName) ?: methodName
                }

                val method = cls.methods.find { it.name == methodToUse }
                if (method != null) {
                    method.isAccessible = true
                    return when {
                        java.lang.reflect.Modifier.isStatic(method.modifiers) -> method.invoke(null)
                        obj is Class<*> -> method.invoke(null)
                        else -> method.invoke(obj)
                    }
                } else {
                    log.warn("Method $methodName not found in class ${cls.name}")
                }
            }
        } catch (e: Exception) {
            log.error("Error invoking method $methodName", e)
            throw LuaError("Failed to invoke method $methodName: ${e.message}")
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