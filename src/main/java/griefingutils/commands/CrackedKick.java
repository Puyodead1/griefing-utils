package griefingutils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.s2c.login.*;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CrackedKick extends BetterCommand {
    public CrackedKick() {
        super("cracked-kick", "kicks players on cracked servers", "ckick");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            literal("*")
                .executes(ctx -> kick(null))
        ).then(
            argument("player", PlayerListEntryArgumentType.create())
                .executes(ctx -> kick(PlayerListEntryArgumentType.get(ctx)))
        );
    }

    private int kick(@Nullable PlayerListEntry entry) {
        InetSocketAddress address = (InetSocketAddress) networkHandler().getConnection().getAddress();
        if (entry == null) {
            info("Kicking everyone");
            for (PlayerListEntry entry2 : mc.getNetworkHandler().getPlayerList()) {
                kickInternal(address, entry2);
            }
        } else {
            if (kickInternal(address, entry)) info("Kicked %s", entry.getProfile().getName());
            else info("Why?");
        }
        return SINGLE_SUCCESS;
    }

    private boolean kickInternal(InetSocketAddress address, PlayerListEntry entry) {
        if (entry.getProfile().equals(mc.player.getGameProfile())) return false;
        if (Friends.get().isFriend(entry)) return false;

        ClientConnection connection = ClientConnection.connect(
            address,
            mc.options.shouldUseNativeTransport(),
            mc.getDebugHud().getPacketSizeLog()
        );

        connection.send(new HandshakeC2SPacket(SharedConstants.getProtocolVersion(), address.getHostName(), address.getPort(), ConnectionIntent.LOGIN));
        connection.setS2CPacketHandler(ConnectionIntent.LOGIN);
        connection.setPacketListener(new ClientLoginPacketListener() {
            @Override
            public void onHello(LoginHelloS2CPacket packet) {
                connection.disconnect(null);
            }

            @Override
            public void onSuccess(LoginSuccessS2CPacket packet) {
            }

            @Override
            public void onDisconnect(LoginDisconnectS2CPacket packet) {
            }

            @Override
            public void onCompression(LoginCompressionS2CPacket packet) {
            }

            @Override
            public void onQueryRequest(LoginQueryRequestS2CPacket packet) {
            }

            @Override
            public void onDisconnected(Text reason) {
            }

            @Override
            public boolean isConnectionOpen() {
                return connection.isOpen();
            }
        });

        connection.send(new LoginHelloC2SPacket(entry.getProfile().getName(), entry.getProfile().getId()));
        return true;
    }
}