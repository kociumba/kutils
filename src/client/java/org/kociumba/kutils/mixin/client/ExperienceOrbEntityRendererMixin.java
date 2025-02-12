package org.kociumba.kutils.mixin.client;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ExperienceOrbEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static org.kociumba.kutils.client.KutilsClientKt.getC;

@Mixin(ExperienceOrbEntityRenderer.class)
public abstract class ExperienceOrbEntityRendererMixin extends EntityRenderer {

    protected ExperienceOrbEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Accessor("TEXTURE")
    static Identifier getTexture() {
        throw new AssertionError();
    }

    @Unique
    private static final Identifier TEXTURE = getTexture();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(ExperienceOrbEntity experienceOrbEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (getC().getCustomXpOrbSize() == 0.0f) {
            ci.cancel(); // skip rendering 0 size orbs to improve performance
        }

        if (!getC().getShouldUsecustomXpOrbs()) {
            return;
        }

        matrixStack.push();

        float customSize = getC().getCustomXpOrbSize();
        matrixStack.scale(customSize, customSize, customSize);

        int j = experienceOrbEntity.getOrbSize();
        float uMin = (float) (j) / 64.0F;
        float uMax = (float) (j + 16) / 64.0F;
        float vMin = (float) (j) / 64.0F;
        float vMax = (float) (j + 16) / 64.0F;

        Color customColor = getC().getCustomXpOrbColor();
        int red = customColor.getRed();
        int green = customColor.getGreen();
        int blue = customColor.getBlue();
        int alpha = customColor.getAlpha();

        matrixStack.translate(0.0F, 0.1F, 0.0F);
        matrixStack.multiply(this.dispatcher.getRotation());
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        // fuck it we ball no emission, but we have alpha
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        MatrixStack.Entry entry = matrixStack.peek();

        renderVertex(vertexConsumer, entry, -0.5F, -0.25F, red, green, blue, alpha, uMin, vMax, light);
        renderVertex(vertexConsumer, entry, 0.5F, -0.25F, red, green, blue, alpha, uMax, vMax, light);
        renderVertex(vertexConsumer, entry, 0.5F, 0.75F, red, green, blue, alpha, uMax, vMin, light);
        renderVertex(vertexConsumer, entry, -0.5F, 0.75F, red, green, blue, alpha, uMin, vMin, light);

        matrixStack.pop();
        ci.cancel();
    }

    @Unique
    private void renderVertex(VertexConsumer vertexConsumer, MatrixStack.Entry entry, float x, float y, int red, int green, int blue, int alpha, float u, float v, int light) {
        vertexConsumer.vertex(entry.getPositionMatrix(), x, y, 0.0F)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0F, 1.0F, 0.0F);
    }
}