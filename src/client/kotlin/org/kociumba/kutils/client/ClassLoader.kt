package org.kociumba.kutils.client

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * This is an adapter class loader to enable lua the usage of already loaded classes through the minecraft class loader
 */
class KutilsClassLoader(private val modClassLoader: ClassLoader) : LuaFunction() {
    override fun call(className: LuaValue): LuaValue {
        val cls = modClassLoader.loadClass(className.checkjstring())
        val coerced = CoerceJavaToLua.coerce(cls)
        val meta = LuaTable()

        // Handle method calls
        meta.set(INDEX, object : LuaFunction() {
            override fun call(self: LuaValue): LuaValue {
                return self
            }

            override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                if (arg.isfunction()) {
                    if (cls == Runnable::class.java) {
                        return CoerceJavaToLua.coerce(object : Runnable {
                            override fun run() {
                                arg.checkfunction().call()
                            }
                        })
                    }
                }

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
                            arg.checkfunction().call()
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
            val method = obj.javaClass.methods.find { it.name == methodName }
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
                            arg.checkfunction().call()
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
        fun register(globals: Globals, modClassLoader: ClassLoader) {
            globals.set("requireJVM", KutilsClassLoader(modClassLoader))
        }
    }
}