package org.kociumba.kmod.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.kociumba.kmod.client.KmodClientKt.getC;

@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {

    @Inject(at = @At("RETURN"), method = "getTimeOfDay", cancellable = true)
    @Environment(EnvType.CLIENT)
    public void getTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (getC().getShouldChangeTime()) {
            cir.setReturnValue((long) getC().getUserTime());
        }
        else cir.cancel();
    }
}
