package org.kociumba.kutils.client.lua

import org.apache.logging.log4j.LogManager
import org.kociumba.kutils.client.mappings.MappingLoader
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Use to inspect any class loaded through `requireJVM` in lua, should implement rendering in the ui later for this
 *
 * This also needs to get the real names in prod, couse nobody is going to find method_1551 useful xd
 */
class LuaInspector(
    private val mappingLoader: MappingLoader
) : LuaFunction() {
    private val log = LogManager.getLogger("kutils/inspector")
    private val inspectedClasses = mutableSetOf<Class<*>>()
    private val StringBuilder.nl: StringBuilder get() = append("\n")

    override fun call(value: LuaValue): LuaValue {
        inspectedClasses.clear()
        val output = StringBuilder()

        when {
            value.isuserdata() -> {
                val obj = value.touserdata()
                when (obj) {
                    is Class<*> -> {
                        output.append("-- Class: ${obj.name}").nl
                        inspectClass(obj, output, isStatic = true)
                    }
                    else -> obj?.let {
                        output.append("-- Instance of: ${it.javaClass.name}").nl
                        inspectInstance(it, output)
                    }
                }
            }
            else -> output.append("-- Unsupported value type: ${value.typename()}").nl
        }

        log.info("\n{}", output.toString())
        return NIL
    }

    private fun inspectClass(cls: Class<*>, output: StringBuilder, isStatic: Boolean) {
        // If we've already inspected this class, skip it to prevent cycles
        if (!inspectedClasses.add(cls)) {
            output.append("-- (Already inspected ${cls.name})").nl
            return
        }

        // Get all methods and fields, including inherited ones
        val allMethods = getAllMethods(cls)
        val allFields = getAllFields(cls)

        // First, show static members if this is a static context
        if (isStatic) {
            output.append("\n-- Static methods:").nl
            allMethods
                .filter { Modifier.isStatic(it.modifiers) }
                .sortedBy { it.name }
                .forEach { method ->
                    formatMethodUsage(method, output, isStatic = true)
                }

//            output.append("\n-- Static fields:").nl
//            allFields
//                .filter { Modifier.isStatic(it.modifiers) }
//                .forEach { field ->
//                    formatFieldUsage(field, output)
//                }
        }

        // Then show instance members
        output.append("\n-- Instance methods:").nl
        allMethods
            .filter { !Modifier.isStatic(it.modifiers) }
            .sortedBy { it.name }
            .forEach { method ->
                formatMethodUsage(method, output, isStatic = false)
            }

//        output.append("\n-- Instance fields:").nl
//        allFields
//            .filter { !Modifier.isStatic(it.modifiers) }
//            .forEach { field ->
//                formatFieldUsage(field, output)
//            }

        // Show superclass information if it exists
        cls.superclass?.let { superClass ->
            if (superClass != Object::class.java) {
                output.append("\n-- Superclass: ${superClass.name}").nl
                inspectClass(superClass, output, isStatic)
            }
        }

        // Show interface information
        val interfaces = cls.interfaces
        if (interfaces.isNotEmpty()) {
            output.append("\n-- Implemented interfaces:").nl
            interfaces.forEach { interface_ ->
                output.append("-- Interface: ${interface_.name}").nl
                inspectClass(interface_, output, isStatic)
            }
        }
    }

    private fun inspectInstance(instance: Any, output: StringBuilder) {
        val cls = instance.javaClass
        inspectClass(cls, output, isStatic = false)
    }

    private fun getAllMethods(cls: Class<*>): Set<Method> {
        val methods = mutableSetOf<Method>()
        var currentClass: Class<*>? = cls

        while (currentClass != null) {
            // Add all declared methods from the current class
            methods.addAll(currentClass.declaredMethods.filter {
                // Filter out synthetic methods and keep only public ones or those made accessible
                !it.isSynthetic && (Modifier.isPublic(it.modifiers) || it.isAccessible)
            })

            // Add methods from interfaces
            currentClass.interfaces.forEach { interface_ ->
                methods.addAll(getAllMethods(interface_))
            }

            currentClass = currentClass.superclass
        }

        return methods
    }

    private fun getAllFields(cls: Class<*>): Set<Field> {
        val fields = mutableSetOf<Field>()
        var currentClass: Class<*>? = cls

        while (currentClass != null) {
            fields.addAll(currentClass.declaredFields.filter {
                !it.isSynthetic && (Modifier.isPublic(it.modifiers) || it.isAccessible)
            })
            currentClass = currentClass.superclass
        }

        return fields
    }

    private fun formatMethodUsage(method: Method, output: StringBuilder, isStatic: Boolean) {
        try {
            // Get the mapped name if available
            val yarnClassName = mappingLoader.getYarnClassName(method.declaringClass.name)
                ?: method.declaringClass.name
            val mappedName = mappingLoader.getDeobfuscatedMethod(yarnClassName, method.name)
                ?: method.name

            val paramTypes = method.parameterTypes.joinToString(", ") {
                mappingLoader.getYarnClassName(it.name) ?: it.simpleName
            }
            val returnTypeName = mappingLoader.getYarnClassName(method.returnType.name)
                ?: method.returnType.simpleName
            val operator = if (isStatic) "." else ":"

            output.append("object$operator$mappedName(")
            if (paramTypes.isNotEmpty()) {
                output.append("-- params: $paramTypes")
            }
            output.append(")")
            if (returnTypeName != "void") {
                output.append("  -- returns: $returnTypeName")
            }
            output.append("  -- defined in: ${method.declaringClass.simpleName}")
            output.nl

        } catch (e: Exception) {
            // Fallback to unmapped names if mapping fails
            val paramTypes = method.parameterTypes.joinToString(", ") { it.simpleName }
            val operator = if (isStatic) "." else ":"
            output.append("object$operator${method.name}($paramTypes)  -- returns: ${method.returnType.simpleName}")
            output.nl
        }
    }

    // fields disabled for now since, i need to redo parsing for them
//    private fun formatFieldUsage(field: Field, output: StringBuilder) {
//        val isStatic = Modifier.isStatic(field.modifiers)
//        val operator = if (isStatic) "." else ":"
//
//        try {
//            val yarnClassName = mappingLoader.getYarnClassName(field.declaringClass.name)
//                ?: field.declaringClass.name
//            val mappedName = mappingLoader.getField(yarnClassName, field.name)
//                ?: field.name
//            val mappedType = mappingLoader.getYarnClassName(field.type.name)
//                ?: field.type.simpleName
//
//            output.append("object$operator$mappedName  -- type: $mappedType  -- defined in: ${field.declaringClass.simpleName}")
//            output.nl
//
//        } catch (e: Exception) {
//            // Fallback to unmapped names
//            output.append("object$operator${field.name}  -- type: ${field.type.simpleName}")
//            output.nl
//        }
//    }

    companion object {
        fun register(globals: Globals, mappingLoader: MappingLoader) {
            globals.set("inspect", LuaInspector(mappingLoader))
        }
    }
}