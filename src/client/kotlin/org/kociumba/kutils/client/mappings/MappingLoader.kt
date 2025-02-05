package org.kociumba.kutils.client.mappings

import net.fabricmc.loader.api.FabricLoader
import org.kociumba.kutils.client.mappings.tinyv2.MappingGetter
import org.kociumba.kutils.client.mappings.tinyv2.TinyMetadata
import org.kociumba.kutils.client.mappings.tinyv2.TinyV2Parser
import org.kociumba.kutils.client.mappings.tinyv2.TinyVisitor
import org.kociumba.kutils.log
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

sealed interface MappingElement {
    val names: Array<String>
    val comment: String?

    fun getName(namespaceIndex: Int): String = names[namespaceIndex]
}

data class ClassMapping(
    override val names: Array<String>,
    override val comment: String? = null,
    val methods: MutableMap<String, MethodMapping> = mutableMapOf(),
    val fields: MutableMap<String, FieldMapping> = mutableMapOf()
) : MappingElement {
    val obfuscatedName: String get() = getName(0)
    val mappedName: String get() = getName(1)
}

data class MethodMapping(
    override val names: Array<String>,
    val descriptor: String,
    override val comment: String? = null,
    val parameters: MutableList<ParameterMapping> = mutableListOf()
) : MappingElement {
    val obfuscatedName: String get() = getName(0)
    val mappedName: String get() = getName(1)
}

data class FieldMapping(
    override val names: Array<String>,
    val descriptor: String,
    override val comment: String? = null
) : MappingElement {
    val obfuscatedName: String get() = getName(0)
    val mappedName: String get() = getName(1)
}

data class ParameterMapping(
    override val names: Array<String>,
    val index: Int,
    override val comment: String? = null
) : MappingElement

class MappingLoader(private val version: String) {
    private val mappingsUrl =
        "https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/mappings-$version.zip"

    // Bidirectional class name mappings
    private val obfToYarn = mutableMapOf<String, String>()
    private val yarnToObf = mutableMapOf<String, String>()

    // Method mappings with both obfuscated and deobfuscated names
    private data class MethodPair(val obfuscated: String, val yarn: String, val descriptor: String)

    private val methodMappings = mutableMapOf<String, MutableMap<MethodKey, MethodPair>>()

    private data class MethodKey(val name: String, val descriptor: String)

    private lateinit var metadata: TinyMetadata

    private val namespaces: List<String>
        get() = metadata.namespaces

    fun downloadAndExtractMappings(): File {
        val versionFolder = File("mods/kutils/mappings/$version")
        val zipFile = File("mods/kutils/mappings/mappings-$version.zip")
        val extractedFile = File(versionFolder, "mappings.tiny")

        if (!versionFolder.exists()) {
            versionFolder.mkdirs()
        }

        // Download the ZIP file if the extracted file doesn't exist
        if (!extractedFile.exists()) {
            if (!zipFile.exists()) {
                URI(mappingsUrl).toURL().openStream().use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, extractedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            zipFile.delete()
        }

        return extractedFile
    }

    fun loadMappings() {
        val file = downloadAndExtractMappings()
        val stack = ArrayDeque<MappingElement>()
//        var currentClass: ClassMapping? = null
        var currentClass: String? = null
        var currentMethod: MethodMapping? = null
        var lastPushedComment: String? = null

        TinyV2Parser.visit(file, object : TinyVisitor {
            override fun start(metadata: TinyMetadata) {
                this@MappingLoader.metadata = metadata
            }

            override fun pushClass(getter: MappingGetter) {
                val rawObfName = getter.get(0)
                val rawYarnName = getter.get(1)

                // Check if this is an inner class
                if (rawObfName.contains("$")) {
                    // This is an inner class, get the outer class name
                    val outerObfName = rawObfName.substringBefore('$')
                    // Store the full name for the inner class separately
                    val fullObfName = rawObfName.replace('/', '.')
                    val fullYarnName = rawYarnName.replace('/', '.')

                    // Store inner class mapping separately
                    obfToYarn[fullObfName] = fullYarnName
                    yarnToObf[fullYarnName] = fullObfName
                    methodMappings[fullObfName] = mutableMapOf()
                } else {
                    // This is a regular class
                    val obfName = rawObfName.replace('/', '.')
                    val yarnName = rawYarnName.replace('/', '.')

                    obfToYarn[obfName] = yarnName
                    yarnToObf[yarnName] = obfName
                    methodMappings[obfName] = mutableMapOf()
                    currentClass = obfName
                }
            }

            override fun pushMethod(getter: MappingGetter, descriptor: String) {
                currentClass?.let { className ->
                    val obfName = getter.get(0)
                    val yarnName = getter.get(1)
                    val methodPair = MethodPair(obfName, yarnName, descriptor)

                    val classMethods = methodMappings[className] ?: mutableMapOf()
                    // Store with descriptor in the key
                    classMethods[MethodKey(yarnName, descriptor)] = methodPair
                    classMethods[MethodKey(obfName, descriptor)] = methodPair
                    methodMappings[className] = classMethods
                }
            }

            override fun pushField(getter: MappingGetter, descriptor: String) {
//                currentClass?.let { classDef ->
//                    val mapping = FieldMapping(
//                        names = getter.getRawNames(),
//                        descriptor = descriptor,
//                        comment = lastPushedComment
//                    )
//                    classDef.fields[getter.get(1)] = mapping
//                    stack.addLast(mapping)
//                    lastPushedComment = null
//                }
            }

            override fun pushParameter(getter: MappingGetter, index: Int) {
                currentMethod?.let { methodDef ->
                    val mapping = ParameterMapping(
                        names = getter.getRawNames(),
                        index = index,
                        comment = lastPushedComment
                    )
                    methodDef.parameters.add(mapping)
                    stack.addLast(mapping)
                    lastPushedComment = null
                }
            }

            override fun pushLocalVariable(getter: MappingGetter, index: Int, startOffset: Int, localIndex: Int) {
                // LocalVariables ignored as they're not needed for remapping
                lastPushedComment = null
            }

            override fun pushComment(comment: String) {
                lastPushedComment = comment
            }

            override fun pop(count: Int) {
                var remainingCount = count
                while (remainingCount > 0 && stack.isNotEmpty()) {
                    val element = stack.removeLastOrNull() ?: break
                    when (element) {
                        is MethodMapping -> currentMethod = null
                        is ClassMapping -> {
                            currentClass = null
                            currentMethod = null
                        }

                        else -> { /* no state change needed */
                        }
                    }
                    remainingCount--
                }
                lastPushedComment = null
            }
        })
    }

    // Class resolution
    fun getYarnClassName(obfuscatedName: String): String? {
        obfToYarn[obfuscatedName]?.let { return it }

        if (!obfuscatedName.contains('$')) {
            return obfToYarn.entries.firstOrNull {
                it.key.substringBefore('$') == obfuscatedName
            }?.value
        }
        return null
    }

    fun getObfuscatedClassName(yarnName: String): String? {
        // First try exact match
        yarnToObf[yarnName]?.let { return it }

        // If not found and doesn't contain $, try to find base class
        if (!yarnName.contains('$')) {
            return yarnToObf.entries.firstOrNull {
                it.key.substringBefore('$') == yarnName
            }?.value
        }

        return null
    }

    fun getObfuscatedMethod(className: String, methodName: String, descriptor: String? = null): String? {
        val methods = methodMappings[className] ?: methodMappings[yarnToObf[className]] ?: return null

        return if (descriptor != null) {
            // If we have a descriptor, look up exact match
            methods[MethodKey(methodName, descriptor)]?.obfuscated
        } else {
            // If no descriptor, try to find any method with matching name
            methods.entries.firstOrNull { it.key.name == methodName }?.value?.obfuscated
        }
    }

    fun getDeobfuscatedMethod(className: String, methodName: String, descriptor: String? = null): String? {
        val methods = methodMappings[className] ?: methodMappings[yarnToObf[className]] ?: return null

        return if (descriptor != null) {
            // If we have a descriptor, look up exact match
            methods[MethodKey(methodName, descriptor)]?.yarn
        } else {
            // If no descriptor, try to find any method with matching name
            methods.entries.firstOrNull { it.key.name == methodName }?.value?.yarn
        }
    }

    fun dumpMappings() {
        log.info("=== Mapping Dump ===")
        obfToYarn.forEach { (obf, yarn) ->
            log.info("Class: $obf -> $yarn")
            methodMappings[obf]?.forEach { (methodName, pair) ->
                log.info("  Method: ${pair.obfuscated} -> ${pair.yarn}")
            }
        }
    }

    // Field resolution
//    fun getField(className: String, fieldName: String): String? {
//        val yarnName = obfuscatedToYarn[className] ?: className
//        return classMappings[yarnName]?.fields?.get(fieldName)?.obfuscatedName
//    }
//
//    // Full mapping objects
//    fun getClassMapping(mappedName: String): ClassMapping? = classMappings[mappedName]
//    fun getMethodMapping(className: String, methodName: String): MethodMapping? =
//        classMappings[obfuscatedToYarn[className] ?: className]?.methods?.get(methodName)
//    fun getFieldMapping(className: String, fieldName: String): FieldMapping? =
//        classMappings[obfuscatedToYarn[className] ?: className]?.fields?.get(fieldName)
}