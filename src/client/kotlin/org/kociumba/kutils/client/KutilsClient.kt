package org.kociumba.kutils.client

import com.github.only52607.luakt.CoerceKotlinToLua
import com.github.only52607.luakt.lib.LuaKotlinExLib
import com.github.only52607.luakt.lib.LuaKotlinLib
import gg.essential.universal.UChat
import gg.essential.universal.UScreen
import gg.essential.universal.utils.MCMinecraft
import imgui.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
//import net.hypixel.modapi.HypixelModAPI
//import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
//import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.kociumba.kutils.Kutils
import org.kociumba.kutils.client.bazaar.WeightEdit
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.client.chat.ChatImageUI
import org.kociumba.kutils.client.chat.registerChatMessageHandler
import org.kociumba.kutils.client.funny.SaabMode
import org.kociumba.kutils.client.hud.hud
import org.kociumba.kutils.client.hud.performanceHud
import org.kociumba.kutils.client.lua.LuaEditor
import org.kociumba.kutils.client.lua.LuaHudRenderer
import org.kociumba.kutils.client.lua.LuaLogger
import org.kociumba.kutils.client.lua.MainThreadExecutor
import org.kociumba.kutils.client.lua.ModuleManager
import org.kociumba.kutils.client.notes.NoteData
import org.kociumba.kutils.client.notes.NotesScreen
import org.kociumba.kutils.log
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import org.lwjgl.glfw.GLFW
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.imguiInternal.ImguiLoader
import xyz.breadloaf.imguimc.imguiInternal.InitCallback
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File

var c: ConfigGUI = ConfigGUI()
var displayingCalc = false
var displayNotes = false
var client: MinecraftClient = MinecraftClient.getInstance()
var chatHud: ChatHud? = null
var isOnHypixel = false
//var loacationPacket: ClientboundLocationPacket? = null
//val LUA_GLOBAL: Globals = JsePlatform.standardGlobals()
lateinit var scriptManager: ModuleManager
var saab = SaabMode().apply {
    initialize()
}

//val mainWindow = UMinecraft.getMinecraft().window
var largeRoboto: ImFont? = null

@Environment(EnvType.CLIENT)
class KutilsClient : ClientModInitializer {
    var loadedWindow = false
    var loadedOptions = false

    override fun onInitializeClient() {

        val open: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open kutils Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "kutils"
            )
        )

        val bazaar: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open bazaar info",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                "kutils"
            )
        )

        val openCalc: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open Calculator",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "kutils"
            )
        )

        val notesKey: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open Notes",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "kutils"
            )
        )

        val luaEditorKey: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "open the lua module editor (WIP)",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                "kutils"
            )
        )

        // Use once, minimize performance impact
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (open.wasPressed()) {
                UScreen.displayScreen(c.gui())
                log.info("kutils config opened")
            }

            while (bazaar.wasPressed()) {
                if (client.currentScreen !is bazaarUI) {
                    bazaarUI.reset()  // Reset some stuff to get around binding limitations
                    UScreen.displayScreen(bazaarUI)
                }
            }

            while (openCalc.wasPressed()) {
                if (!displayingCalc) {
                    displayingCalc = true
                    MinecraftClient.getInstance().gameRenderer.renderBlur(1f)
                    ImCalcUI.reset()
                    Imguimc.pushRenderable(ImCalcUI)
                } else {
                    displayingCalc = false
                    Imguimc.pullRenderable(ImCalcUI)
                }
            }

            while (notesKey.wasPressed()) {
                if (client.currentScreen !is NotesScreen) {
                    NotesScreen.reset()
                    UScreen.displayScreen(NotesScreen)
                }
            }

            while (luaEditorKey.wasPressed()) {
                if (client.currentScreen !is LuaEditor) {
                    LuaEditor.reset()
                    UScreen.displayScreen(LuaEditor)
                }
            }

            if (!loadedWindow && c.customWindowTitle != "" && MinecraftClient.getInstance().window != null) {
                loadedWindow = true
                WindowTitleListener.notifyWindowChanged(c.customWindowTitle)
            }

            if (!loadedOptions && c.shouldUseFullbright && MinecraftClient.getInstance().options != null) {
                loadedOptions = true
                MCMinecraft.getInstance().options.gamma.value = 1000.0
            }

            if (chatHud == null) {
                try {
                    chatHud = client.inGameHud.chatHud
                } catch (e: Exception) {
                    log.error("Failed to get chat hud", e)
                }
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            dispatcher.register(ClientCommandManager.literal("kutils").executes { context ->
                var client = MinecraftClient.getInstance()
                client.send {
                    client.setScreenAndRender(c.gui())
                    log.info("kutils config opened by command")
                }
                0 // need this to return something
            })
        }

        if (MinecraftClient.getInstance().isFinishedLoading && c.fontScale != 1.0f) {
            ImGui.getIO().fontGlobalScale = c.fontScale
        }

        if (c.displayPerformanceHud) {
            Imguimc.pushRenderable(performanceHud)
        }

        if (c.displayHud) {
            Imguimc.pushRenderable(hud)
        }

        // no fucking way we have font loading 😎
        val tempDir = Files.createTempDirectory("imgui_fonts")
        val fontPath = tempDir.resolve("CascadiaCode-Regular.ttf")

        // have to unpack the font from the jar, couse imgui can't see inside the jar
        KutilsClient::class.java.getResourceAsStream("/assets/fonts/CascadiaCode-Regular.ttf").use { input ->
            Files.copy(input, fontPath, StandardCopyOption.REPLACE_EXISTING)
            log.info("font extracted to $fontPath")
        }

        // TODO: actually find a font that works here.
        //  labels: imgui issue
        var fontLoader = InitCallback { io: ImGuiIO, fontAtlas: ImFontAtlas, fontConfig: ImFontConfig, glyphRanges: ShortArray  ->
            // loads custom font
            fontConfig.oversampleH = 16
            fontConfig.oversampleV = 16
//            io.setFontDefault(fontAtlas.addFontFromFileTTF(fontPath.toAbsolutePath().toString(), 16f, fontConfig))
            fontAtlas.addFontFromFileTTF(fontPath.toAbsolutePath().toString(), 14f, fontConfig, glyphRanges)

            // additional init stuff
            io.fontAllowUserScaling = true
            io.configWindowsMoveFromTitleBarOnly = false
            log.info("custom font loaded successfully")
        }
        ImguiLoader.initCallback = fontLoader

        ImguiLoader.iniFileName = "config/imgui/kutils.ini"

        registerChatMessageHandler()

        if (c.shouldPreviewChatImages) {
            Imguimc.pushRenderable(ChatImageUI)
            ChatImageUI.initialize()
        }

        WeightEdit.loadWeights()
        log.info("prediction weights loaded")

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            WeightEdit.saveWeights()
            log.info("prediction weights saved")
        }

        NoteData.loadNotes()
        log.info("notes loaded")

//        HudRenderCallback.EVENT.register { drawContext, tickDeltaManager ->
//            TestOverlay().renderElementaOverlay()
//        }

//        HypixelModAPI.getInstance().createHandler(ClientboundHelloPacket::class.java) { packet ->
//            log.info("received hello packet")
//            isOnHypixel = true
//        }
//
//        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)
//
//        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket::class.java) { packet ->
//            client.execute {
//                log.info(packet.toString())
//                loacationPacket = packet
//            }
//        }

        // moved to the module manager
        // register everything related to lua
//        LUA_GLOBAL.load(LuaKotlinLib())
//        LUA_GLOBAL.load(LuaKotlinExLib())
//        KutilsClassLoader.register(LUA_GLOBAL, Kutils::class.java.classLoader)
//        LuaLogger.register(LUA_GLOBAL)
//        MainThreadExecutor.register(LUA_GLOBAL, client)
//        LuaHudRenderer.register(LUA_GLOBAL)
//        log.info("lua capabilities loaded")

        val scriptsFolder = File("config/kutils/lua/")
        val typesFolder = File(scriptsFolder, "types")

        ClientLifecycleEvents.CLIENT_STARTED.register {
            scriptManager = ModuleManager(client).apply {
                // Load scripts from disk
                loadScripts()
            }
            LuaEditor.initialize(scriptManager)

            scriptsFolder.mkdirs()
            typesFolder.mkdirs()
        }

        // Extract LSP config files from resources
        try {
            javaClass.getResourceAsStream("/lua/configs/.luarc.json")?.use { input ->
                File(scriptsFolder, ".luarc.json").outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            javaClass.getResourceAsStream("/lua/configs/types/kutils.lua")?.use { input ->
                File(typesFolder, "kutils.lua").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to extract LSP configuration files: ${e.message}")
        }

        if (c.saabMode) Imguimc.pushRenderable(saab)

        log.info("kutils initial setup done")
    }
}