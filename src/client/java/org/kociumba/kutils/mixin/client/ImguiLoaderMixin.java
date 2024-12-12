package org.kociumba.kutils.mixin.client;

import imgui.ImGuiIO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects custom fonts at init in the Imguimmc package
 * <br>
 * So this should work in theory, but the package is structured weirdly
 * and the class is just inaccessible
 * <br>
 * Without this there is no loading custom fonts,
 * so I'm probably gonna have to fork the repo and adjust it later
 * fuck this, makes my life difficult
 */
//@Mixin(xyz.breadloaf.imguimc.imgui.ImguiLoader.class)
//public class ImguiLoaderMixin {
//    @Inject(method = "initializeImGui", at = @At("HEAD"))
//    private static void injectCustomFont(long glHandle, CallbackInfo ci) {
//        ImGui.createContext();
//        ImGuiIO io = ImGui.getIO();
//        io.setIniFilename(null);
//        io.addConfigFlags(1);
//        io.addConfigFlags(64);
//        io.addConfigFlags(1024);
//        io.setConfigViewportsNoTaskBarIcon(true);
//        ImFontAtlas fontAtlas = io.getFonts();
//        ImFontConfig fontConfig = new ImFontConfig();
//        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic());
//
//        // load custom font
//        fontAtlas.addFontFromFileTTF("assets/fonts/CascadiaCode.ttf", 18.0f, fontConfig);
//
//        fontAtlas.addFontDefault();
//        fontConfig.setMergeMode(true);
//        fontConfig.setPixelSnapH(true);
//        fontConfig.destroy();
//        if (io.hasConfigFlags(1024)) {
//            ImGuiStyle style = ImGui.getStyle();
//            style.setWindowRounding(0.0F);
//            style.setColor(2, ImGui.getColorU32(2, 1.0F));
//        }
//    }
//}
