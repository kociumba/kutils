package org.kociumba.kutils.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.kociumba.kutils.client.events.ChatMessageEvent;
import org.kociumba.kutils.client.events.GetMessageAtEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kociumba.kutils.KutilsLogger;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        ChatMessageEvent.Companion.publish(new ChatMessageEvent(message.getString()));
    }

    @Final
    @Shadow
    private List<ChatHudLine.Visible> visibleMessages;

    @Final
    @Shadow
    private List<ChatHudLine> messages;

    @Shadow
    protected abstract double toChatLineX(double x);

    @Shadow
    protected abstract double toChatLineY(double y);

    @Shadow
    protected abstract int getMessageLineIndex(double x, double y);

    @Shadow
    protected abstract int getMessageIndex(double x, double y);

    @Unique
    private Text getMessageAt(double x, double y) {
        int n = this.getMessageIndex(this.toChatLineX(x), this.toChatLineY(y));
        if (n < 0 || n >= this.messages.size()) {
            return Text.empty(); // Return empty Text if index is invalid
        }
        return this.messages.get(n).content();

//        if (i >= 0 && i < this.visibleMessages.size()) {
//            KutilsLogger.INSTANCE.info("Message content: " + this.visibleMessages.get(i).toString());
//            return this.visibleMessages.get(i).content();
//        }
//        return null;
    }

    @Inject(method = "getTextStyleAt", at = @At("HEAD"))
    private void onGetTextStyleAt(double x, double y, CallbackInfoReturnable<Style> cir) {
        Text content = this.getMessageAt(x, y);
        GetMessageAtEvent.Companion.publish(new GetMessageAtEvent(content, x, y));
    }
}