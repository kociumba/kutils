package org.kociumba.kutils.mixin.client;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.text.Text;

import java.util.List;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Invoker("collectPlayerEntries")
    List<PlayerListEntry> invokeCollectPlayerEntries();

    @Accessor
    Text getHeader();

    @Accessor
    Text getFooter();
}
