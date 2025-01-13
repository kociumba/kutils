package org.kociumba.kutils.client.mappings.tinyv2

interface TinyVisitor {
    fun start(metadata: TinyMetadata)
    fun pushClass(getter: MappingGetter)
    fun pushField(getter: MappingGetter, descriptor: String)
    fun pushMethod(getter: MappingGetter, descriptor: String)
    fun pushParameter(getter: MappingGetter, index: Int)
    fun pushLocalVariable(getter: MappingGetter, index: Int, startOffset: Int, localIndex: Int)
    fun pushComment(comment: String)
    fun pop(count: Int)
}