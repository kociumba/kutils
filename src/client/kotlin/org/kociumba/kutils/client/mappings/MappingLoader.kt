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
    private lateinit var metadata: TinyMetadata

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
        var currentClass: ClassMapping? = null
        var currentMethod: MethodMapping? = null
        var lastPushedComment: String? = null

        TinyV2Parser.visit(file, object : TinyVisitor {
            override fun start(metadata: TinyMetadata) {
                this@MappingLoader.metadata = metadata
            }

            override fun pushClass(getter: MappingGetter) {
                val mapping = ClassMapping(
                    names = getter.getRawNames(),
                    comment = lastPushedComment
                )
                classMappings[getter.get(1).replace('/', '.')] = mapping
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

    // Name Strings
    fun getClass(mappedName: String): String? =
        classMappings[mappedName]?.obfuscatedName

    fun getMethod(className: String, methodName: String): String? =
        classMappings[className]?.methods?.get(methodName)?.obfuscatedName

    fun getField(className: String, fieldName: String): String? =
        classMappings[className]?.fields?.get(fieldName)?.obfuscatedName

    // Full mapping objects
    fun getClassMapping(mappedName: String): ClassMapping? = classMappings[mappedName]
    fun getMethodMapping(className: String, methodName: String): MethodMapping? =
        classMappings[className]?.methods?.get(methodName)
    fun getFieldMapping(className: String, fieldName: String): FieldMapping? =
        classMappings[className]?.fields?.get(fieldName)
}