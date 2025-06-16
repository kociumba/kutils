package org.kociumba.kutils.client

import imgui.ImGui
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.client.bazaar.WeightEdit
import org.kociumba.kutils.client.chat.ChatImageUI
import org.kociumba.kutils.client.events.OverlayColorChangeEvent
import org.kociumba.kutils.client.events.WindowTitleChangedEvent
import org.kociumba.kutils.client.hud.hud
import org.kociumba.kutils.client.hud.networkingHud
import org.kociumba.kutils.client.hud.performanceHud
import xyz.breadloaf.imguimc.Imguimc
import java.awt.Color
import java.io.File
import java.lang.reflect.Modifier

object ColorStringSerializer : KSerializer<Color> {
    override val descriptor =
        PrimitiveSerialDescriptor("ColorString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val s = "${value.red},${value.green},${value.blue},${value.alpha}"
        encoder.encodeString(s)
    }

    override fun deserialize(decoder: Decoder): Color {
        val parts = decoder.decodeString()
            .split(',').map { it.trim().toInt() }
        require(parts.size == 4) { "ColorString must have 4 comma-separated ints" }
        return Color(parts[0], parts[1], parts[2], parts[3])
    }
}

/**
 * The best color presets I could think of
 *
 * They are in fact so good I'm not going to enable them in the public build 💀
 */
enum class DamageTintPresets(val color: Color) {
    PissYellow(Color(255, 242, 78)),
    ShitBrown(Color(82, 50, 15)),
    TittyMilk(Color(242, 223, 228)),
    PussyPink(Color(227, 153, 143)),
    WeedGreen(Color(1, 94, 7)),
    MethBlue(Color(140, 200, 222)),
}

@Serializable
@Environment(EnvType.CLIENT)
class Config(@Transient private val configFile: File = File("./config/kutils.json")) {

    // Rendering - Entity
    var shouldTintDamage: Boolean = true
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var damageTintColor: Color = Color(255, 0, 0, 77)
        set(value) {
            field = value; save()
        }

    var damageTintPresets: Int = DamageTintPresets.PissYellow.ordinal
        set(value) {
            field = value; save()
        }

    var shouldUsecustomXpOrbs: Boolean = false
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var customXpOrbColor: Color = Color(9, 137, 9, 255)
        set(value) {
            field = value; save()
        }

    var customXpOrbSize: Float = 1.0f
        set(value) {
            field = value; save()
        }

    // Rendering - World
    var shouldColorWater: Boolean = false
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var waterColor: Color = Color(0, 0, 255, 77)
        set(value) {
            field = value; save()
        }

    var shouldChangeTime: Boolean = false
        set(value) {
            field = value; save()
        }

    var userTime: Float = 0.0f
        set(value) {
            field = value; save()
        }

    // Rendering - Particles
    var disableBlockBreakParticle: Boolean = false
        set(value) {
            field = value; save()
        }

    // Rendering - Utils
    var displayHud: Boolean = false
        set(value) {
            field = value; save()
        }

    var displayPerformanceHud: Boolean = false
        set(value) {
            field = value; save()
        }

    var displayNetworkingHud: Boolean = false
        set(value) {
            field = value; save()
        }

    var shouldUseFullbright: Boolean = false
        set(value) {
            field = value; save()
        }

    // Player - Movement
    var shouldAlwaysSprint: Boolean = false
        set(value) {
            field = value; save()
        }

    // Player - Camera
    var removeSelfieCamera: Boolean = true
        set(value) {
            field = value; save()
        }

    // GUI - Kutils UI
    var fontScale: Float = 1.0f
        set(value) {
            field = value; save()
        }

    var showWeeklyTraffic: Boolean = false
        set(value) {
            field = value; save()
        }

    var showWeeklyAveragePrice: Boolean = true
        set(value) {
            field = value; save()
        }

    var shouldConsiderInflatedPercent: Float = 0.2f
        set(value) {
            field = value; save()
        }

    var hudHasBackground: Boolean = true
        set(value) {
            field = value; save()
        }

    var hudIsDraggable: Boolean = true
        set(value) {
            field = value; save()
        }

    var mainThemeBackgroundOpacity: Float = 1.0f
        set(value) {
            field = value; save()
        }

    // GUI - Theme
    @Serializable(with = ColorStringSerializer::class)
    var mainWindowBackground: Color = Color(0.04f, 0.04f, 0.04f, 0.94f)
        set(value) {
            field = value; save()
        }

    var windowRounding: Float = 5.0f
        set(value) {
            field = value; save()
        }

    var wholeWindowAlpha: Float = 1.0f
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var productIDColor: Color = Color.decode("#cba6f7")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var sellPriceColor: Color = Color.decode("#94e2d5")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var buyPriceColor: Color = Color.decode("#eba0ac")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var differenceColor: Color = Color.decode("#89b4fa")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var weeklyTrafficColor: Color = Color.decode("#fab387")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var averagesColor: Color = Color.decode("#f9e2af")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var positivePredictionColor: Color = Color.decode("#a6e3a1")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var negativePredictionColor: Color = Color.decode("#f38ba8")
        set(value) {
            field = value; save()
        }

    @Serializable(with = ColorStringSerializer::class)
    var inflatedItemWarningColor: Color = Color.decode("#ff0000")
        set(value) {
            field = value; save()
        }

    // GUI - Signs
    var shouldSubmitSignsWithEnter: Boolean = true
        set(value) {
            field = value; save()
        }

    // Chat - Addons
    var shouldPreviewChatImages: Boolean = true
        set(value) {
            field = value; save()
        }

    // Misc - Window
    var customWindowTitle: String = ""
        set(value) {
            field = value; save()
        }

    // Misc - ???
    var saabMode: Boolean = false
        set(value) {
            field = value; save()
        }

    var shouldShowOldConfigWarning: Boolean = true
        set(value) {
            field = value; save()
        }

    // JSON configuration for pretty printing
    @Transient
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(Color::class, ColorStringSerializer)
        }
    }

    private var isLoading = false

    fun load() {
        isLoading = true
        try {
            if (configFile.exists()) {
                val jsonString = configFile.readText()
                val loadedConfig = json.decodeFromString<Config>(jsonString)
                copyFrom(loadedConfig)
            } else {
                save()
            }
        } catch (e: Exception) {
            println("Failed to load config: ${e.message}")
            save()
        } finally {
            isLoading = false
        }
    }

    fun save() {
        if (isLoading) return

        try {
            configFile.parentFile?.mkdirs()
            val jsonString = json.encodeToString(this)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Failed to save config: ${e.message}")
        }
    }

    private fun copyFrom(other: Config) {
        this::class.java.declaredFields
            .filter { f ->
                !Modifier.isStatic(f.modifiers)
                        && f.getAnnotation(Transient::class.java) == null
            }
            .forEach { f ->
                f.isAccessible = true
                f.set(this, f.get(other))
            }
    }

    fun editWeights() {
        if (!WeightEdit.rendered) {
            WeightEdit.loadWeights()
            WeightEdit.rendered = true
            Imguimc.pushRenderableAfterRender(WeightEdit)
        } else {
            Imguimc.pullRenderableAfterRender(WeightEdit)
            WeightEdit.rendered = false
            WeightEdit.saveWeights()
        }
    }

    companion object {
        private const val DEFAULT_PATH = "config/kutils.json"

        fun loadFrom(
            file: File = File(DEFAULT_PATH)
        ): Config {
            val base = Config(file)

            if (file.exists()) {
                try {
                    base.isLoading = true
                    val text = file.readText()
                    val loaded = base.json.decodeFromString<Config>(text)
                    base.copyFrom(loaded)
                } catch (e: Exception) {
                    println("Failed to load, using defaults: ${e.message}")
                } finally {
                    base.isLoading = false
                }
            }

            if (!file.exists()) base.save()

            return base
        }
    }
}
