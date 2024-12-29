package org.kociumba.kutils.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.collection.ArrayListDeque;
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
import java.util.stream.Collectors;
import java.util.ArrayList;

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

    @Final
    @Shadow
    private ArrayListDeque<String> messageHistory;

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
        // Get the visual line index from the mouse position
        int visualLineIndex = this.getMessageLineIndex(this.toChatLineX(x), this.toChatLineY(y));

        if (visualLineIndex < 0 || visualLineIndex >= this.visibleMessages.size()) {
            return Text.empty();
        }

        // Get the creation tick of the visible line
        int targetTick = this.visibleMessages.get(visualLineIndex).addedTime();

        // Count how many wrapped lines came before this one
        int wrappedLineCount = 0;
        for (int i = 0; i < visualLineIndex; i++) {
            ChatHudLine.Visible line = this.visibleMessages.get(i);
            if (line.addedTime() == targetTick) {
                wrappedLineCount++;
            }
        }

        // Find the original message
        for (ChatHudLine message : messages) {
            if (message.creationTick() == targetTick) {
                return message.content();
            }
        }

        KutilsLogger.INSTANCE.warn("Failed to find message at position " + x + ", " + y);
        return Text.empty();
    }

    @Unique
    private List<String> getMessagesString() {
        return messages.stream()
                .map(msg -> msg.content().getString())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Inject(method = "getTextStyleAt", at = @At("HEAD"))
    private void onGetTextStyleAt(double x, double y, CallbackInfoReturnable<Style> cir) {
        Text content = this.getMessageAt(x, y);
        GetMessageAtEvent.Companion.publish(new GetMessageAtEvent(content, x, y));
    }
}