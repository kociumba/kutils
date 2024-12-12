package org.kociumba.kutils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.biome.BiomeEffects;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.*;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

@Environment(EnvType.CLIENT)
@Mixin(BiomeEffects.class)
public class BiomeEffectsMixin {

    @ModifyReturnValue(method = "getWaterColor", at = @At("RETURN"))
    private int getWaterColor(int original) {
        Color c = getC().getWaterColor();
        int customColor = ColorHelper.Argb.getArgb(c.getAlpha(), c.getBlue(), c.getGreen(), c.getRed());
        if (getC().getShouldColorWater()) return customColor;
        return original;
    }

}
