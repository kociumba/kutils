package org.kociumba.kutils.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.kociumba.kutils.client.OverlayTextureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.Color;
import net.minecraft.util.math.ColorHelper;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

/**
 * Mixin for {@link OverlayTexture} to add custom tinting to the overlay texture.
 * The mixin adds a new listener for the overlay texture and updates the overlay color
 * when the damage tint is changed in the config.
 */
@Mixin(OverlayTexture.class)
public abstract class OverlayTextureMixin implements OverlayTextureListener {

    @Shadow
    private NativeImageBackedTexture texture = new NativeImageBackedTexture(16, 16, false);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initCustomOverlay(CallbackInfo ci) {
        this.updateOverlayColor(getC().getDamageTintColor());
        OverlayTextureListener.Companion.register(this);
    }

    public void onColorChanged(Color newColor) {
        updateOverlayColor(newColor);
    }

    /**
     * Updates the overlay to a new color, outside of init of the {@link OverlayTexture} class
     * <br>
     * <br>
     * So turns out this uses abgr, not argb
     * I have no idea why, and the mod I was referencing is also using argb,
     * so I guess it's also broken ? 🤷
     */
    @Unique
    public void updateOverlayColor(Color color) {
        NativeImage nativeImage = this.texture.getImage();

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                if (i < 8) {
                    assert nativeImage != null;
                    nativeImage.setColor(j, i, getColorInt(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
                }
            }
        }

        RenderSystem.activeTexture(33985);
        this.texture.bindTexture();
        nativeImage.upload(0, 0, 0, 0, 0, nativeImage.getWidth(), nativeImage.getHeight(), false, true, false, false);
        RenderSystem.activeTexture(33984);
    }

    @Unique
    private static int getColorInt(int red, int green, int blue, int alpha) {
        alpha = 255 - alpha;
//        int value = ((alpha & 0xFF) << 24) | ((red & 0xff) << 16) | ((green & 0xff) << 8) | ((blue & 0xff));
//        UChat.chat("Color: " + Integer.toString(value));
        return ColorHelper.Abgr.getAbgr(alpha, blue, green, red);
    }
}
