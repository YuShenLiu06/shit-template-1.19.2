package com.lys.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import java.util.UUID;

public class JetActionPacket {
    public static final Identifier ID = new Identifier("shitmod", "jet_action");

    private final UUID playerUuid;
    private final boolean isActive;

    public JetActionPacket(UUID playerUuid, boolean isActive) {
        this.playerUuid = playerUuid;
        this.isActive = isActive;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isActive() {
        return isActive;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(playerUuid);
        buf.writeBoolean(isActive);
    }

    public static JetActionPacket read(PacketByteBuf buf) {
        return new JetActionPacket(buf.readUuid(), buf.readBoolean());
    }
}
