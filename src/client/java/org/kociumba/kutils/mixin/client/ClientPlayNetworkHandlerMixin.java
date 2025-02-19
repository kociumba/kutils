package org.kociumba.kutils.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.kociumba.kutils.client.events.GameJoinEvent;
import org.kociumba.kutils.client.events.NewTabPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Get world join events
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(at = @At("TAIL"), method = "onGameJoin")
    private void triggerJoinEvent(GameJoinS2CPacket packet, CallbackInfo ci) {
        GameJoinEvent.Companion.publish(new GameJoinEvent(packet));
    }

    @Inject(method = "onPlayerListHeader", at = @At("HEAD"))
    private void onHeaderFooterPacket(PlayerListHeaderS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }

    @Inject(method = "onScoreboardObjectiveUpdate", at = @At("HEAD"))
    private void onScoreboardObjectiveUpdate(ScoreboardObjectiveUpdateS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }

    @Inject(method = "onScoreboardScoreUpdate", at = @At("HEAD"))
    private void onScoreboardScoreUpdate(ScoreboardScoreUpdateS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }

    @Inject(method = "onScoreboardScoreReset", at = @At("HEAD"))
    private void onScoreboardScoreReset(ScoreboardScoreResetS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }

    @Inject(method = "onScoreboardDisplay", at = @At("HEAD"))
    private void onScoreboardDisplay(ScoreboardDisplayS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }

    @Inject(method = "onTeam", at = @At("HEAD"))
    private void onTeam(TeamS2CPacket pkt, CallbackInfo ci) {
        NewTabPacket.Companion.publish(new NewTabPacket(pkt));
    }
}
