package org.kociumba.kutils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Final;
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
