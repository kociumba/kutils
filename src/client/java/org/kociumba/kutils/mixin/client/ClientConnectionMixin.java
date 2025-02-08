package org.kociumba.kutils.mixin.client;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.kociumba.kutils.client.events.PacketReceiveEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        PacketReceiveEvent.Companion.publish(new PacketReceiveEvent(packet));
    }
}
