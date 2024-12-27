package org.kociumba.kutils.client.imgui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import org.kociumba.kutils.log
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import javax.imageio.ImageIO

enum class LoadingState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

/**
 * ImImage is a wrapper for handling minecraft images, and allows for uploading
 * them to the GPU and getting the texture id needed for rendering with ImGui
 */
class ImImage : AutoCloseable {
    /**
     * the underlying minecraft AbstractTexture
     */
    var abstractTexture : AbstractTexture? = null

    /**
     * the texture id on the GPU, if -1 the texture has not been uploaded
     */
    var glID : Int = -1

    /**
     * the unique prefix generated for this texture. If empty, the texture has not been uploaded
     */
    var prefix : String = ""
    val isValid: Boolean get() = glID != -1
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var loadingState: LoadingState = LoadingState.IDLE
        private set
    var errorMessage: String = ""
        private set

    private val defPrefix : String = "kutils_"
    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private fun convertToPng(input: InputStream): ByteArrayInputStream {
        return try {
            input.use { stream ->
                val originalImage: BufferedImage = ImageIO.read(stream)
                ByteArrayOutputStream().use { outputStream ->
                    ImageIO.write(originalImage, "png", outputStream)
                    ByteArrayInputStream(outputStream.toByteArray())
                }
            }
        } catch (e: Exception) {
            log.error("Failed to convert image to png", e)
            ByteArrayInputStream(byteArrayOf())
        }
    }

    /**
     * Load an image from the given path. If the image is not a .png, it will be converted to one.
     * If the image fails to load, an error will be logged and the ImImage will be returned unchanged.
     */
    fun loadImage(path: String): ImImage {
        this.loadingState = LoadingState.LOADING
        this.errorMessage = ""
        try {
            FileInputStream(path).use { fileStream ->
                NativeImage.read(convertToPng(fileStream)).use { image ->
                    return loadFromNativeImage(image)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to load image from path $path", e)
            this.loadingState = LoadingState.ERROR
            this.errorMessage = e.message ?: "Unknown error"
            return this
        }
    }

    /**
     * the same as this.loadImage, but loads from a url, this needs to be a direct url to the image.
     * If the image fails to load, an error will be logged and the ImImage will be returned unchanged
     */
    fun loadImageFromURL(url: String, onComplete: ((Boolean) -> Unit)? = null): ImImage {
        this.loadingState = LoadingState.LOADING
        this.errorMessage = ""

        var b = this

        var r: ImImage? = null
        scope.launch {
            try {
                val bytes: ByteArray = URI(url).toURL().readBytes()

                client.submitAndJoin {
                    ByteArrayInputStream(bytes).use { stream ->
                        NativeImage.read(convertToPng(stream)).use { image ->
                            r = loadFromNativeImage(image)
                            loadingState = LoadingState.LOADED
                            onComplete?.invoke(true)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to load image from url $url", e)
                loadingState = LoadingState.ERROR
                errorMessage = e.message ?: "Unknown error"
                r = b
                onComplete?.invoke(false)
            }
        }
        return r ?: this
    }

    private fun generateUniquePrefix(): String {
        this.prefix = "${defPrefix}${System.nanoTime()}"
        return this.prefix
    }

    private fun loadFromNativeImage(image: NativeImage): ImImage {
        destroyTexture() // Clean up any existing texture

        this.width = image.width
        this.height = image.height

        val texture = NativeImageBackedTexture(image)
        val txID = client.textureManager.registerDynamicTexture(this.generateUniquePrefix(), texture)
        client.textureManager.bindTexture(txID)
        this.abstractTexture = client.textureManager.getTexture(txID)
            ?: throw IllegalStateException("Failed to get texture from texture manager")
        this.glID = this.abstractTexture?.glId ?: -1

        this.loadingState = LoadingState.LOADED
        return this
    }

    /**
     * call to close the texture and destroy the ImImage
     */
    fun destroyTexture() {
        this.abstractTexture?.close()
        this.abstractTexture = null
        this.loadingState = LoadingState.IDLE
        this.errorMessage = ""
        this.width = 0
        this.height = 0
        this.glID = -1
    }

    override fun close() {
        destroyTexture()
    }
}