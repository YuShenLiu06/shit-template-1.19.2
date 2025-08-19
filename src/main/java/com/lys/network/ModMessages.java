package com.lys.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.lys.Shit;

public class ModMessages {
    public static final Identifier EXAMPLE_PACKET = new Identifier(Shit.MOD_ID, "example_packet");
    public static final Identifier JET_ACTION = new Identifier(Shit.MOD_ID, "jet_action");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(EXAMPLE_PACKET, (server, player, handler, buf, responseSender) -> {
            // 原有数据包处理逻辑
        });

        ServerPlayNetworking.registerGlobalReceiver(JET_ACTION, (server, player, handler, buf, responseSender) -> {
            JetActionPacket packet = JetActionPacket.read(buf);
            server.execute(() -> {
                // 处理喷射动作
                handleJetAction(server, packet);
            });
        });
    }

    public static void sendToServer(JetActionPacket packet) {
        ClientPlayNetworking.send(JET_ACTION, new PacketByteBuf(new PacketByteBuf(Unpooled.buffer())));
    }

    private static void handleJetAction(MinecraftServer server, JetActionPacket packet) {
        // 服务器端喷射动作处理
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(packet.getPlayerUuid());
        if (player != null && packet.isActive()) {
            // 验证权限和执行喷射逻辑
//            Shit.LOGGER.info("Player {} activated jet function", player.getName().getString());
        }
    }
}
