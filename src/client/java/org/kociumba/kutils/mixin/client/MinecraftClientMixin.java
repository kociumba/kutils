package org.kociumba.kutils.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.kociumba.kutils.KutilsLogger;
import org.kociumba.kutils.client.WindowTitleListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private String lastTitle = "";

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(RunArgs args, CallbackInfo ci) {
        WindowTitleListener.Companion.register(newTitle -> lastTitle = newTitle);
        KutilsLogger.INSTANCE.info("window title listener initialized");
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getTitle(CallbackInfoReturnable<String> cir) {
        if (getC().getShouldUseCustomWindowTitle()) {
            cir.setReturnValue(lastTitle);
        }
    }
}
