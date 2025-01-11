package org.kociumba.kutils.mixin.client;

import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

/**
 * Allows for submitting signs with enter
 */
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Shadow
    protected abstract void finishEditing();

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (getC().getShouldSubmitSignsWithEnter()) {
            boolean isShiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && !isShiftPressed) {
                finishEditing();
                cir.cancel();
            }
        }
    }
}