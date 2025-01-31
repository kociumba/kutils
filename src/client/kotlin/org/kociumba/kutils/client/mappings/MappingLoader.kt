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
    private val mappingsUrl = "https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/mappings-$version.zip"
    private val classMappings = mutableMapOf<String, ClassMapping>()
    private val obfuscatedToYarn = mutableMapOf<String, String>()
    private lateinit var metadata: TinyMetadata

    private val namespaces: List<String>
        get() = metadata.namespaces

    fun downloadAndExtractMappings(): File {
        val versionFolder = File("mods/kutils/mappings/$version")
        val zipFile = File("mods/kutils/mappings/mappings-$version.zip")
        val extractedFile = File(versionFolder, "mappings.tiny")

        if (!versionFolder.exists()) { versionFolder.mkdirs() }

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
        var currentClass: ClassMapping? = null
        var currentMethod: MethodMapping? = null
        var lastPushedComment: String? = null

        TinyV2Parser.visit(file, object : TinyVisitor {
            override fun start(metadata: TinyMetadata) {
                this@MappingLoader.metadata = metadata
            }

            override fun pushClass(getter: MappingGetter) {
                val yarnName = getter.get(1).replace('/', '.')
                val obfName = getter.get(0).replace('/', '.')

                val mapping = ClassMapping(
                    names = getter.getRawNames(),
                    comment = lastPushedComment
                )
                classMappings[yarnName] = mapping
                obfuscatedToYarn[obfName] = yarnName  // Populate reverse map
                currentClass = mapping
                stack.addLast(mapping)
                lastPushedComment = null
            }

            override fun pushMethod(getter: MappingGetter, descriptor: String) {
                currentClass?.let { classDef ->
                    val mapping = MethodMapping(
                        names = getter.getRawNames(),
                        descriptor = descriptor,
                        comment = lastPushedComment
                    )
                    classDef.methods[getter.get(1)] = mapping
                    currentMethod = mapping
                    stack.addLast(mapping)
                    lastPushedComment = null
                }
            }

            override fun pushField(getter: MappingGetter, descriptor: String) {
                currentClass?.let { classDef ->
                    val mapping = FieldMapping(
                        names = getter.getRawNames(),
                        descriptor = descriptor,
                        comment = lastPushedComment
                    )
                    classDef.fields[getter.get(1)] = mapping
                    stack.addLast(mapping)
                    lastPushedComment = null
                }
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
                        else -> { /* no state change needed */ }
                    }
                    remainingCount--
                }
                lastPushedComment = null
            }
        })
    }

    // Class resolution
    fun getClass(mappedName: String): String? = classMappings[mappedName]?.obfuscatedName
    fun getYarnClassName(obfuscatedName: String): String? = obfuscatedToYarn[obfuscatedName]

    // Method resolution
    fun getMethod(className: String, methodName: String): String? {
        val direct = classMappings[className]?.methods?.get(methodName)?.obfuscatedName
        if (direct != null) return direct

        val yarnName = obfuscatedToYarn[className] ?: return null
        return classMappings[yarnName]?.methods?.get(methodName)?.obfuscatedName
    }

    fun getMethodReverse(obfClassName: String, obfMethodName: String): String? {
        val yarnClassName = getYarnClassName(obfClassName) ?: return null
        return classMappings[yarnClassName]?.methods?.entries
            ?.find { it.value.obfuscatedName == obfMethodName }
            ?.key
    }

    // Field resolution
    fun getField(className: String, fieldName: String): String? {
        val yarnName = obfuscatedToYarn[className] ?: className
        return classMappings[yarnName]?.fields?.get(fieldName)?.obfuscatedName
    }

    // Full mapping objects
    fun getClassMapping(mappedName: String): ClassMapping? = classMappings[mappedName]
    fun getMethodMapping(className: String, methodName: String): MethodMapping? =
        classMappings[obfuscatedToYarn[className] ?: className]?.methods?.get(methodName)
    fun getFieldMapping(className: String, fieldName: String): FieldMapping? =
        classMappings[obfuscatedToYarn[className] ?: className]?.fields?.get(fieldName)
}