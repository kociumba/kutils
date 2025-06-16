package org.kociumba.kutils.client

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImInt
import imgui.type.ImString
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text
import org.kociumba.kutils.client.chat.ChatImageUI
import org.kociumba.kutils.client.events.OverlayColorChangeEvent
import org.kociumba.kutils.client.events.WindowTitleChangedEvent
import org.kociumba.kutils.client.hud.networkingHud
import org.kociumba.kutils.client.hud.performanceHud
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.icons.FontAwesomeIcons
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import java.awt.Color

@Environment(EnvType.CLIENT)
class KutilsConfig(
    private val config: Config
) : ImGuiScreen(Text.literal("KUtils Settings"), true) {

    // State variables for all settings
    private val shouldTintDamageFlag = ImBoolean(config.shouldTintDamage)
    private val shouldColorWaterFlag = ImBoolean(config.shouldColorWater)
    private val shouldChangeTimeFlag = ImBoolean(config.shouldChangeTime)
    private val shouldAlwaysSprintFlag = ImBoolean(config.shouldAlwaysSprint)
    private val displayHudFlag = ImBoolean(config.displayHud)
    private val displayPerformanceHudFlag = ImBoolean(config.displayPerformanceHud)
    private val displayNetworkingHudFlag = ImBoolean(config.displayNetworkingHud)
    private val disableBlockBreakParticleFlag = ImBoolean(config.disableBlockBreakParticle)
    private val shouldUseFullbrightFlag = ImBoolean(config.shouldUseFullbright)
    private val showWeeklyTrafficFlag = ImBoolean(config.showWeeklyTraffic)
    private val showWeeklyAveragePriceFlag = ImBoolean(config.showWeeklyAveragePrice)
    private val hudHasBackgroundFlag = ImBoolean(config.hudHasBackground)
    private val hudIsDraggableFlag = ImBoolean(config.hudIsDraggable)
    private val removeSelfieCamera = ImBoolean(config.removeSelfieCamera)
    private val shouldPreviewChatImagesFlag = ImBoolean(config.shouldPreviewChatImages)
    private val saabModeFlag = ImBoolean(config.saabMode)
    private val shouldSubmitSignsWithEnterFlag = ImBoolean(config.shouldSubmitSignsWithEnter)
    private val shouldUsecustomXpOrbsFlag = ImBoolean(config.shouldUsecustomXpOrbs)

    // Slider values
    private val userTimeSlider = floatArrayOf(config.userTime)
    private val fontScaleSlider = floatArrayOf(config.fontScale)
    private val shouldConsiderInflatedPercentSlider = floatArrayOf(config.shouldConsiderInflatedPercent)
    private val mainThemeBackgroundOpacitySlider = floatArrayOf(config.mainThemeBackgroundOpacity)
    private val windowRoundingSlider = floatArrayOf(config.windowRounding)
    private val wholeWindowAlphaSlider = floatArrayOf(config.wholeWindowAlpha)
    private val customXpOrbSizeSlider = floatArrayOf(config.customXpOrbSize)

    // Text inputs
    private val customWindowTitleStr = ImString(config.customWindowTitle, 256)

    // Damage tint presets
    private val damageTintPresetsCombo = ImInt(config.damageTintPresets)
    private val presetNames = arrayOf("PissYellow", "ShitBrown", "TittyMilk", "PussyPink", "WeedGreen", "MethBlue")

    override fun initImGui(): List<ImGuiWindow?>? {
        // Fullscreen overlay style flags
        val flags = ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoSavedSettings or
                ImGuiWindowFlags.NoFocusOnAppearing or
                ImGuiWindowFlags.NoBringToFrontOnFocus or
                ImGuiWindowFlags.NoBackground

        return listOf(
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("KUtils Settings"),
                { renderFullscreenOverlay() },
                false,
                flags
            )
        )
    }

    private fun renderFullscreenOverlay() {
        val viewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(viewport.posX, viewport.posY)
        ImGui.setNextWindowSize(viewport.sizeX, viewport.sizeY)

        if (ImGui.begin(
                "KUtils Settings Overlay",
                ImGuiWindowFlags.NoDecoration or
                        ImGuiWindowFlags.NoMove or
                        ImGuiWindowFlags.NoNavFocus or
                        ImGuiWindowFlags.NoNavInputs
            )
        ) {
            // Center the content
            val contentWidth = 800f
            val availWidth = ImGui.getContentRegionAvailX()
            val offsetX = (availWidth - contentWidth) * 0.5f
            if (offsetX > 0) ImGui.setCursorPosX(ImGui.getCursorPosX() + offsetX)

            ImGui.beginChild("SettingsContent", contentWidth, 0f, true)

            // Title
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 10f)
            ImGui.text("KUtils Configuration")
            ImGui.separator()
            ImGui.popStyleVar()

            renderAllSettings()

//            ImGui.separator()
//            if (ImGui.button("Save", 200f, 40f)) {
//                config.writeData()
//            }

            ImGui.endChild()
        }
        ImGui.end()
    }

    private fun renderAllSettings() {
        val treeFlags = ImGuiTreeNodeFlags.DefaultOpen or ImGuiTreeNodeFlags.Framed

        // ── RENDERING ──────────────────────────────────────────────────────────────
        if (ImGui.collapsingHeader("Rendering", treeFlags)) {
            renderEntitySettings()
            renderWorldSettings()
            renderParticleSettings()
            renderUtilsSettings()
        }

        // ── PLAYER ─────────────────────────────────────────────────────────────────
        if (ImGui.collapsingHeader("Player", treeFlags)) {
            renderMovementSettings()
            renderCameraSettings()
        }

        // ── GUI / KUTILS UI ────────────────────────────────────────────────────────
        if (ImGui.collapsingHeader("GUI", treeFlags)) {
            renderKutilsUISettings()
            renderThemeSettings()
            renderSignSettings()
        }

        // ── CHAT ──────────────────────────────────────────────────────────────────
        if (ImGui.collapsingHeader("Chat", treeFlags)) {
            renderChatSettings()
        }

        // ── MISC ───────────────────────────────────────────────────────────────────
        if (ImGui.collapsingHeader("Miscellaneous", treeFlags)) {
            renderMiscSettings()
            renderInternalSettings()
        }
    }

    private fun renderEntitySettings() {
        if (ImGui.treeNodeEx("Entity", ImGuiTreeNodeFlags.DefaultOpen)) {
            // Custom damage tint
            if (ImGui.checkbox("Custom damage tint", shouldTintDamageFlag)) {
                config.shouldTintDamage = shouldTintDamageFlag.get()
                val color = if (shouldTintDamageFlag.get()) config.damageTintColor else Color(255, 0, 0, 77)
                OverlayColorChangeEvent.publish(OverlayColorChangeEvent(color))
            }

            // Damage tint color
            val damageColor = floatArrayOf(
                config.damageTintColor.red / 255f,
                config.damageTintColor.green / 255f,
                config.damageTintColor.blue / 255f,
                config.damageTintColor.alpha / 255f
            )
            if (ImGui.colorEdit4("Damage tint color", damageColor) && shouldTintDamageFlag.get()) {
                config.damageTintColor = Color(damageColor[0], damageColor[1], damageColor[2], damageColor[3])
                OverlayColorChangeEvent.publish(OverlayColorChangeEvent(config.damageTintColor))
            }

            // Damage tint presets (commented out in original but kept for completeness)
            // if (ImGui.combo("Damage tint presets", damageTintPresetsCombo, presetNames)) {
            //     config.damageTintPresets = damageTintPresetsCombo.get()
            //     val presetColor = DamageTintPresets.entries[damageTintPresetsCombo.get()].color
            //     OverlayColorChangeEvent.publish(OverlayColorChangeEvent(presetColor))
            //     config.damageTintColor = presetColor
            // }

            // Custom XP orbs
            if (ImGui.checkbox("Use custom XP orbs", shouldUsecustomXpOrbsFlag)) {
                config.shouldUsecustomXpOrbs = shouldUsecustomXpOrbsFlag.get()
            }

            if (shouldUsecustomXpOrbsFlag.get()) {
                // XP orb color
                val xpOrbColor = floatArrayOf(
                    config.customXpOrbColor.red / 255f,
                    config.customXpOrbColor.green / 255f,
                    config.customXpOrbColor.blue / 255f,
                    config.customXpOrbColor.alpha / 255f
                )
                if (ImGui.colorEdit4("Custom XP orb color", xpOrbColor)) {
                    config.customXpOrbColor = Color(xpOrbColor[0], xpOrbColor[1], xpOrbColor[2], xpOrbColor[3])
                }

                // XP orb size
                if (ImGui.sliderFloat("Custom XP orb size", customXpOrbSizeSlider, 0f, 2f)) {
                    config.customXpOrbSize = customXpOrbSizeSlider[0]
                }
            }

            ImGui.treePop()
        }
    }

    private fun renderWorldSettings() {
        if (ImGui.treeNodeEx("World", ImGuiTreeNodeFlags.DefaultOpen)) {
            // Custom water tint (hidden in original)
            // if (ImGui.checkbox("Custom water tint", shouldColorWaterFlag)) {
            //     config.shouldColorWater = shouldColorWaterFlag.get()
            // }

            // Change time
            if (ImGui.checkbox("Change time", shouldChangeTimeFlag)) {
                config.shouldChangeTime = shouldChangeTimeFlag.get()
            }

            if (shouldChangeTimeFlag.get()) {
                if (ImGui.sliderFloat("User time", userTimeSlider, 0f, 24000f)) {
                    config.userTime = userTimeSlider[0]
                }
            }

            ImGui.treePop()
        }
    }

    private fun renderParticleSettings() {
        if (ImGui.treeNodeEx("Particles", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Disable block breaking particles", disableBlockBreakParticleFlag)) {
                config.disableBlockBreakParticle = disableBlockBreakParticleFlag.get()
            }
            ImGui.treePop()
        }
    }

    private fun renderUtilsSettings() {
        if (ImGui.treeNodeEx("Utils", ImGuiTreeNodeFlags.DefaultOpen)) {
            // Hidden HUD (commented out in new version)
            // if (ImGui.checkbox("Display HUD", displayHudFlag)) {
            //     config.displayHud = displayHudFlag.get()
            //     toggleRenderable(hud, displayHudFlag.get())
            // }

            if (ImGui.checkbox("Performance HUD", displayPerformanceHudFlag)) {
                config.displayPerformanceHud = displayPerformanceHudFlag.get()
                toggleRenderable(performanceHud, displayPerformanceHudFlag.get())
            }

            if (ImGui.checkbox("Networking HUD", displayNetworkingHudFlag)) {
                config.displayNetworkingHud = displayNetworkingHudFlag.get()
                toggleRenderable(networkingHud, displayNetworkingHudFlag.get())
            }

            if (ImGui.checkbox("Toggle fullbright", shouldUseFullbrightFlag)) {
                config.shouldUseFullbright = shouldUseFullbrightFlag.get()
                client?.options?.gamma?.value = if (shouldUseFullbrightFlag.get()) 100.0 else 1.0
            }

            ImGui.treePop()
        }
    }

    private fun renderMovementSettings() {
        if (ImGui.treeNodeEx("Movement", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Always sprint", shouldAlwaysSprintFlag)) {
                config.shouldAlwaysSprint = shouldAlwaysSprintFlag.get()
            }
            ImGui.treePop()
        }
    }

    private fun renderCameraSettings() {
        if (ImGui.treeNodeEx("Camera", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Remove selfie camera", removeSelfieCamera)) {
                config.removeSelfieCamera = removeSelfieCamera.get()
            }
            ImGui.treePop()
        }
    }

    private fun renderKutilsUISettings() {
        if (ImGui.treeNodeEx("Kutils UI", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.sliderFloat("Font scale", fontScaleSlider, 0.1f, 2f)) {
                config.fontScale = fontScaleSlider[0]
                ImGui.getIO().fontGlobalScale = config.fontScale
            }
            ImGui.sameLine()
            if (ImGui.button(FontAwesomeIcons.Undo)) {
                fontScaleSlider[0] = 1.0f
                config.fontScale = 1.0f
                ImGui.getIO().fontGlobalScale = 1.0f
            }

            if (ImGui.checkbox("Show weekly traffic", showWeeklyTrafficFlag)) {
                config.showWeeklyTraffic = showWeeklyTrafficFlag.get()
            }

            if (ImGui.checkbox("Show weekly average price", showWeeklyAveragePriceFlag)) {
                config.showWeeklyAveragePrice = showWeeklyAveragePriceFlag.get()
            }

            if (ImGui.sliderFloat("Inflated percent", shouldConsiderInflatedPercentSlider, 0f, 1f)) {
                config.shouldConsiderInflatedPercent = shouldConsiderInflatedPercentSlider[0]
            }

            if (ImGui.checkbox("HUD has background", hudHasBackgroundFlag)) {
                config.hudHasBackground = hudHasBackgroundFlag.get()
            }

            if (ImGui.checkbox("HUD is draggable", hudIsDraggableFlag)) {
                config.hudIsDraggable = hudIsDraggableFlag.get()
            }

            ImGui.treePop()
        }
    }

    private fun renderThemeSettings() {
        if (ImGui.treeNodeEx("Theme", ImGuiTreeNodeFlags.DefaultOpen)) {
            // Window background
            val windowBg = floatArrayOf(
                config.mainWindowBackground.red / 255f,
                config.mainWindowBackground.green / 255f,
                config.mainWindowBackground.blue / 255f,
                config.mainWindowBackground.alpha / 255f
            )
            if (ImGui.colorEdit4("Window background", windowBg)) {
                config.mainWindowBackground = Color(windowBg[0], windowBg[1], windowBg[2], windowBg[3])
            }

            if (ImGui.sliderFloat("Window rounding", windowRoundingSlider, 0f, 25f)) {
                config.windowRounding = windowRoundingSlider[0]
            }

            if (ImGui.sliderFloat("General opacity", wholeWindowAlphaSlider, 0f, 1f)) {
                config.wholeWindowAlpha = wholeWindowAlphaSlider[0]
            }

            // Color settings for bazaar display
            ImGui.separator()
            ImGui.text("Bazaar Colors:")

            val productIDColor = floatArrayOf(
                config.productIDColor.red / 255f,
                config.productIDColor.green / 255f,
                config.productIDColor.blue / 255f
            )
            if (ImGui.colorEdit3("Product ID color", productIDColor)) {
                config.productIDColor = Color(productIDColor[0], productIDColor[1], productIDColor[2])
            }

            val sellPriceColor = floatArrayOf(
                config.sellPriceColor.red / 255f,
                config.sellPriceColor.green / 255f,
                config.sellPriceColor.blue / 255f
            )
            if (ImGui.colorEdit3("Sell price color", sellPriceColor)) {
                config.sellPriceColor = Color(sellPriceColor[0], sellPriceColor[1], sellPriceColor[2])
            }

            val buyPriceColor = floatArrayOf(
                config.buyPriceColor.red / 255f,
                config.buyPriceColor.green / 255f,
                config.buyPriceColor.blue / 255f
            )
            if (ImGui.colorEdit3("Buy price color", buyPriceColor)) {
                config.buyPriceColor = Color(buyPriceColor[0], buyPriceColor[1], buyPriceColor[2])
            }

            val differenceColor = floatArrayOf(
                config.differenceColor.red / 255f,
                config.differenceColor.green / 255f,
                config.differenceColor.blue / 255f
            )
            if (ImGui.colorEdit3("Difference color", differenceColor)) {
                config.differenceColor = Color(differenceColor[0], differenceColor[1], differenceColor[2])
            }

            val weeklyTrafficColor = floatArrayOf(
                config.weeklyTrafficColor.red / 255f,
                config.weeklyTrafficColor.green / 255f,
                config.weeklyTrafficColor.blue / 255f
            )
            if (ImGui.colorEdit3("Weekly traffic color", weeklyTrafficColor)) {
                config.weeklyTrafficColor = Color(weeklyTrafficColor[0], weeklyTrafficColor[1], weeklyTrafficColor[2])
            }

            val averagesColor = floatArrayOf(
                config.averagesColor.red / 255f,
                config.averagesColor.green / 255f,
                config.averagesColor.blue / 255f
            )
            if (ImGui.colorEdit3("Averages color", averagesColor)) {
                config.averagesColor = Color(averagesColor[0], averagesColor[1], averagesColor[2])
            }

            val positivePredictionColor = floatArrayOf(
                config.positivePredictionColor.red / 255f,
                config.positivePredictionColor.green / 255f,
                config.positivePredictionColor.blue / 255f
            )
            if (ImGui.colorEdit3("Positive prediction color", positivePredictionColor)) {
                config.positivePredictionColor =
                    Color(positivePredictionColor[0], positivePredictionColor[1], positivePredictionColor[2])
            }

            val negativePredictionColor = floatArrayOf(
                config.negativePredictionColor.red / 255f,
                config.negativePredictionColor.green / 255f,
                config.negativePredictionColor.blue / 255f
            )
            if (ImGui.colorEdit3("Negative prediction color", negativePredictionColor)) {
                config.negativePredictionColor =
                    Color(negativePredictionColor[0], negativePredictionColor[1], negativePredictionColor[2])
            }

            val inflatedItemWarningColor = floatArrayOf(
                config.inflatedItemWarningColor.red / 255f,
                config.inflatedItemWarningColor.green / 255f,
                config.inflatedItemWarningColor.blue / 255f
            )
            if (ImGui.colorEdit3("Inflated item warning color", inflatedItemWarningColor)) {
                config.inflatedItemWarningColor =
                    Color(inflatedItemWarningColor[0], inflatedItemWarningColor[1], inflatedItemWarningColor[2])
            }

            ImGui.treePop()
        }
    }

    private fun renderSignSettings() {
        if (ImGui.treeNodeEx("Signs", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Submit signs with enter", shouldSubmitSignsWithEnterFlag)) {
                config.shouldSubmitSignsWithEnter = shouldSubmitSignsWithEnterFlag.get()
            }
            ImGui.treePop()
        }
    }

    private fun renderChatSettings() {
        if (ImGui.treeNodeEx("Addons", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Preview images in chat", shouldPreviewChatImagesFlag)) {
                config.shouldPreviewChatImages = shouldPreviewChatImagesFlag.get()
                if (shouldPreviewChatImagesFlag.get()) {
                    ChatImageUI.initialize()
                    Imguimc.pushRenderable(ChatImageUI)
                } else {
                    Imguimc.pullRenderable(ChatImageUI)
                }
            }
            ImGui.treePop()
        }
    }

    private fun renderMiscSettings() {
        if (ImGui.treeNodeEx("Window", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.inputText("Custom window title", customWindowTitleStr, ImGuiInputTextFlags.None)) {
                config.customWindowTitle = customWindowTitleStr.get()
                val title = if (config.customWindowTitle.isEmpty()) "" else config.customWindowTitle
                WindowTitleChangedEvent.publish(WindowTitleChangedEvent(title))
            }
            ImGui.treePop()
        }

        if (ImGui.treeNodeEx("???", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.checkbox("Saab Mode", saabModeFlag)) {
                config.saabMode = saabModeFlag.get()
                toggleRenderable(saab, saabModeFlag.get())
            }
            ImGui.treePop()
        }
    }

    private fun renderInternalSettings() {
        if (ImGui.treeNodeEx("Internal", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.button("Edit prediction weights")) {
                config.editWeights()
            }
            ImGui.treePop()
        }
    }

    private fun toggleRenderable(renderable: Renderable, enabled: Boolean) {
        if (enabled) {
            Imguimc.pushRenderableAfterRender(renderable)
        } else {
            Imguimc.pullRenderableAfterRender(renderable)
        }
    }

    override fun close() {
        config.save()
        super.close()
    }
}