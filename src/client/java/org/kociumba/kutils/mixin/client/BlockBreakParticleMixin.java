package org.kociumba.kutils.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

/**
 * this finally works, I don't know why particles are such a pain in the ass
 */
@Environment(EnvType.CLIENT)
@Mixin(ParticleManager.class)
public class BlockBreakParticleMixin {
    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void hideBlockBreakParticles(CallbackInfo ci) {
        if (getC().getDisableBlockBreakParticle()) ci.cancel();
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void hideBlockBreakingParticles(CallbackInfo ci) {
        if (getC().getDisableBlockBreakParticle()) ci.cancel();
    }
}
