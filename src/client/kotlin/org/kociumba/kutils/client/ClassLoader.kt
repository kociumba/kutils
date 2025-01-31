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
                    mappingLoader.getClass(inputName)?.let { resolved ->
                        log.info("Debug: resolved $inputName -> $resolved")
                    }
                }
            } else {
                mappingLoader.getClass(inputName)
                    ?.replace('/', '.')
                    ?.let { modClassLoader.loadClass(it) }
                    ?: modClassLoader.loadClass(inputName)
            }

            val coerced = CoerceJavaToLua.coerce(cls)
            val meta = LuaTable()

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

            val methodMeta = LuaTable()
            methodMeta.set("__call", object : LuaFunction() {
                override fun call(methodWrapper: LuaValue): LuaValue {
                    return invokeMethodWrapper(methodWrapper, emptyArray())
                }


                override fun call(methodWrapper: LuaValue, arg1: LuaValue): LuaValue {
                    return invokeMethodWrapper(methodWrapper, arrayOf(arg1))
                }

                override fun call(methodWrapper: LuaValue, arg1: LuaValue, arg2: LuaValue): LuaValue {
                    return invokeMethodWrapper(methodWrapper, arrayOf(arg1, arg2))
                }

                private fun invokeMethodWrapper(wrapper: LuaValue, args: Array<LuaValue>): LuaValue {
                    val self = wrapper.get("self")
                    val methodName = resolveRuntimeMethodName(
                        self = self,
                        luaMethodName = wrapper.get("method").checkjstring()
                    )

                    val javaArgs = args.map { CoerceLuaToJava.coerce(it, Any::class.java) }.toTypedArray()
                    return invokeJavaMethod(self, methodName, javaArgs)
                }
            })

            coerced.setmetatable(meta)
            return coerced

        } catch (e: Exception) {
            log.error("Error loading a class", e)
            throw LuaError("Failed to load class: ${e.message}")
        }
    }

    private fun resolveRuntimeMethodName(self: LuaValue, luaMethodName: String): String {
        val javaClass = when (val obj = self.touserdata()) {
            is Class<*> -> obj
            else -> obj?.javaClass ?: throw LuaError("Invalid method call on null")
        }

        return if (debug) luaMethodName else {
            val yarnClassName = mappingLoader.getYarnClassName(javaClass.name)
                ?: throw LuaError("No Yarn mapping for ${javaClass.name}")

            mappingLoader.getMethod(yarnClassName, luaMethodName)
                ?: throw LuaError("No method mapping for $luaMethodName in $yarnClassName")
        }
    }

    private fun invokeJavaMethod(self: LuaValue, luaMethodName: String, args: Array<Any?>): LuaValue {
        val javaObj = self.touserdata()

        // 1. Get the actual target class (MinecraftClient.class)
        val (targetClass, isStaticCall) = when (javaObj) {
            is Class<*> -> javaObj to true
            else -> (javaObj?.javaClass ?: throw LuaError("Method call on null object")) to false
        }

        // 2. Resolve Yarn class name for mapping lookup
        val yarnClassName = mappingLoader.getYarnClassName(targetClass.name) ?: run {
            log.warn("No Yarn mapping for class ${targetClass.name}")
            targetClass.name
        }

        // 3. Translate method name using MappingLoader
        val runtimeMethodName = if (debug) {
            luaMethodName
        } else {
            mappingLoader.getMethod(yarnClassName, luaMethodName) ?: run {
                log.warn("No mapping for method $luaMethodName in class $yarnClassName")
                luaMethodName
            }
        }

        log.info("Resolved method $luaMethodName -> $runtimeMethodName in class ${targetClass.name}")

        // 4. Find method with translated name
        val method = targetClass.methods.find {
            it.name == runtimeMethodName && matchParameters(it.parameterTypes, args)
        } ?: throw LuaError("Method $runtimeMethodName not found in ${targetClass.name}")

        method.isAccessible = true

        // 5. Invoke with proper context
        val result = if (Modifier.isStatic(method.modifiers)) {
            method.invoke(null, *args)
        } else {
            method.invoke(javaObj, *args)
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
            // Handle no-argument calls
            override fun call(wrapper: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, emptyArray())
            }

            // Handle single argument calls
            override fun call(wrapper: LuaValue, arg1: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, arrayOf(arg1))
            }

            // Handle two argument calls
            override fun call(wrapper: LuaValue, arg1: LuaValue, arg2: LuaValue): LuaValue {
                return invokeMethodWrapper(wrapper, arrayOf(arg1, arg2))
            }

            private fun invokeMethodWrapper(wrapper: LuaValue, args: Array<LuaValue>): LuaValue {
                val self = wrapper.get("self")
                val methodName = wrapper.get("method").checkjstring()
                val javaArgs = args.map { CoerceLuaToJava.coerce(it, Any::class.java) }.toTypedArray()

                // Handle static methods by filtering out self argument
                val targetSelf = if (self.touserdata() is Class<*>) {
                    // Static method call - ignore self argument from colon syntax
                    javaArgs.drop(1).toTypedArray()
                } else {
                    javaArgs
                }

                return try {
                    val result = invokeJavaMethod(self, methodName, targetSelf)
                    CoerceJavaToLua.coerce(result)
                } catch (e: Exception) {
                    throw LuaError("Method call failed: ${e.message}")
                }
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

    private fun invokeMethod(self: LuaValue, methodName: String): Any? {
        try {
            val obj = self.touserdata()
            if (obj != null) {
                val cls = when (obj) {
                    is Class<*> -> obj
                    else -> obj.javaClass
                }

                log.info("Attempting method '$methodName' on class: ${cls.name}")

                val yarnClassName = mappingLoader.getYarnClassName(cls.name).also {
                    log.info("Yarn class name resolved: ${it ?: "NOT FOUND"}")
                }

                val methodToUse = if (debug) {
                    methodName.also {
                        log.info("Dev mode using direct method name: $it")
                    }
                } else {
                    mappingLoader.getMethod(yarnClassName ?: cls.name, methodName).also {
                        log.info("Prod mode resolved method: ${it ?: "NOT FOUND"}")
                    } ?: methodName
                }

                log.info("Final method name being used: $methodToUse")
                log.info("Available methods: ${cls.methods.joinToString { it.name }}")

                val method = cls.methods.find { it.name == methodToUse }
                if (method != null) {
                    method.isAccessible = true
                    return when {
                        Modifier.isStatic(method.modifiers) -> method.invoke(null)
                        obj is Class<*> -> method.invoke(null)
                        else -> method.invoke(obj)
                    }
                } else {
                    log.warn("Method $methodToUse not found in class ${cls.name} (Yarn: $yarnClassName)")
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