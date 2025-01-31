package org.kociumba.kutils.client

import net.fabricmc.loader.api.FabricLoader
import org.kociumba.kutils.client.lua.CustomDebugLib
import org.kociumba.kutils.client.mappings.MappingLoader
import org.kociumba.kutils.log
import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.CoerceLuaToJava
import java.lang.reflect.Modifier

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
                modClassLoader.loadClass(inputName).also {
                    mappingLoader.getObfuscatedClassName(inputName)?.let { resolved ->
                        log.info("Debug: resolved $inputName -> $resolved")
                    }
                }
            } else {
                mappingLoader.getObfuscatedClassName(inputName)
                    ?.replace('/', '.')
                    ?.let { modClassLoader.loadClass(it) }
                    ?: modClassLoader.loadClass(inputName)
            }

            // Try to create an instance if possible
            val instance = try {
                if (!Modifier.isAbstract(cls.modifiers) && !cls.isInterface) {
                    // Find the no-arg constructor
                    val constructor = cls.getDeclaredConstructor()
                    constructor.isAccessible = true
                    constructor.newInstance()
                } else {
                    null
                }
            } catch (e: Exception) {
                log.debug("Could not instantiate ${cls.name}, falling back to class reference", e)
                null
            }

            // Coerce either the instance or the class itself
            val coerced = CoerceJavaToLua.coerce(instance ?: cls)
            val meta = LuaTable()

            // Store both the class and instance (if any) in the metatable
            meta.set("__class", CoerceJavaToLua.coerce(cls))
            instance?.let { meta.set("__instance", CoerceJavaToLua.coerce(it)) }

            meta.set(INDEX, object : LuaFunction() {
                override fun call(self: LuaValue, key: LuaValue): LuaValue {
                    return createMethodWrapper(self, key.checkjstring())
                }

                override fun call(self: LuaValue, arg1: LuaValue, arg2: LuaValue): LuaValue {
                    val methodName = arg2.checkjstring()
                    log.info("Colon syntax call for $methodName")
                    return invokeMethodWithArgs(self, methodName, arrayOf(arg1))
                }
            })

            // Add special methods to access static functionality when needed
            meta.set("static", object : LuaFunction() {
                override fun call(self: LuaValue): LuaValue {
                    return CoerceJavaToLua.coerce(cls)
                }
            })

            // Add constructor access
            meta.set("new", object : LuaFunction() {
                override fun call(): LuaValue {
                    return handleConstructor(cls)
                }

                override fun call(arg: LuaValue): LuaValue {
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

    private fun handleConstructor(cls: Class<*>, vararg args: LuaValue): LuaValue {
        return try {
            when {
                args.size == 1 && args[0].isfunction() && cls == Thread::class.java -> {
                    val runnable = object : Runnable {
                        override fun run() {
                            try {
                                args[0].checkfunction().call()
                            } catch (e: Exception) {
                                if (e !is CustomDebugLib.ScriptInterruptException && e !is InterruptedException) {
                                    log.error("Error in thread", e)
                                }
                            }
                        }
                    }
                    CoerceJavaToLua.coerce(Thread(runnable))
                }
                args.isEmpty() -> {
                    val constructor = cls.getDeclaredConstructor()
                    constructor.isAccessible = true
                    CoerceJavaToLua.coerce(constructor.newInstance())
                }
                else -> {
                    // Handle constructor with arguments
                    val javaArgs = args.map { CoerceLuaToJava.coerce(it, Any::class.java) }.toTypedArray()
                    val constructor = cls.constructors.find { it.parameterTypes.size == args.size }
                        ?: throw LuaError("No matching constructor found")
                    constructor.isAccessible = true
                    CoerceJavaToLua.coerce(constructor.newInstance(*javaArgs))
                }
            }
        } catch (e: Exception) {
            log.error("Error creating instance", e)
            throw LuaError("Failed to create instance: ${e.message}")
        }
    }

    private fun invokeJavaMethod(self: LuaValue, luaMethodName: String, args: Array<Any?>): LuaValue {
        val javaObj = self.touserdata()
        val targetClass = when (javaObj) {
            is Class<*> -> javaObj
            else -> javaObj?.javaClass ?: throw LuaError("Method call on null object")
        }

        // Get the obfuscated method name in production
        val methodNameToUse = if (!debug) {
            mappingLoader.getObfuscatedMethod(targetClass.name, luaMethodName)?.also {
                log.debug("Resolved method mapping: $luaMethodName -> $it")
            } ?: throw LuaError("No mapping found for method $luaMethodName in class ${targetClass.name}")
        } else {
            luaMethodName
        }

        // Find and invoke the method using the correct name
        val method = targetClass.methods.find { it.name == methodNameToUse }
            ?: throw LuaError("Method $methodNameToUse not found in ${targetClass.name}")

        method.isAccessible = true

        val result = when {
            Modifier.isStatic(method.modifiers) -> method.invoke(null, *args)
            javaObj is Class<*> -> method.invoke(null, *args)
            else -> method.invoke(javaObj, *args)
        }

        return CoerceJavaToLua.coerce(result)
    }

    private fun matchParameters(paramTypes: Array<Class<*>>, args: Array<Any?>): Boolean {
        if (paramTypes.size != args.size) return false
        return paramTypes.indices.all { i ->
            args[i] == null || paramTypes[i].isAssignableFrom(args[i]!!::class.java)
        }
    }

    private fun createMethodWrapper(self: LuaValue, methodName: String): LuaValue {
        val methodWrapper = LuaTable()
        methodWrapper.set("self", self)
        methodWrapper.set("method", LuaValue.valueOf(methodName))

        val meta = LuaTable()
        meta.set("__call", object : LuaFunction() {
            override fun call(wrapper: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, emptyArray())
            }

            override fun call(wrapper: LuaValue, arg1: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, arrayOf(arg1))
            }

            override fun call(wrapper: LuaValue, arg1: LuaValue, arg2: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, arrayOf(arg1, arg2))
            }

            private fun invokeMethodWrapper(wrapper: LuaValue, args: Array<LuaValue>): LuaValue {
                val self = wrapper.get("self")
                val methodName = wrapper.get("method").checkjstring()
                val javaArgs = args.map { CoerceLuaToJava.coerce(it, Any::class.java) }.toTypedArray()
                return invokeJavaMethod(self, methodName, javaArgs)
            }
        })

        methodWrapper.setmetatable(meta)
        return methodWrapper
    }

    private fun invokeMethodWithArgs(self: LuaValue, methodName: String, args: Array<LuaValue>): LuaValue {
        return try {
            val javaArgs = args.map { CoerceLuaToJava.coerce(it, Any::class.java) }.toTypedArray()
            val result = invokeJavaMethod(self, methodName, javaArgs)
            CoerceJavaToLua.coerce(result)
        } catch (e: Exception) {
            throw LuaError("Method call failed: ${e.message}")
        }
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