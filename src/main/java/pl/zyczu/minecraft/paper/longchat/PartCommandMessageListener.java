package pl.zyczu.minecraft.paper.longchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class PartCommandMessageListener implements PluginMessageListener, Listener {

    private final LongChatPlugin plugin;
    private final Map<UUID, ByteArrayOutputStream> buffers = new HashMap<>();
    private final Method performChatCommand;

    public PartCommandMessageListener(LongChatPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.performChatCommand = findMethod();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        buffers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        boolean allowed = player.hasPermission("longchat.command");
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message));
            int len = dis.readShort();
            if (len == 0) {
                if (!allowed) {
                    player.sendMessage(Component.text("You are not allowed to use long commands!").color(NamedTextColor.RED));
                    return;
                }
                ByteArrayOutputStream bos = buffers.remove(player.getUniqueId());
                if (bos == null) return;

                performCommand(player, new String(bos.toByteArray(), StandardCharsets.UTF_8));
            } else if (allowed) {
                ByteArrayOutputStream bos = buffers.computeIfAbsent(player.getUniqueId(), __ -> new ByteArrayOutputStream());
                if (bos.size() > 1000000) {
                    buffers.remove(player.getUniqueId());
                    plugin.getLogger().warning("Player " + player + " sent too long command!");
                } else {
                    byte[] data = new byte[len];
                    dis.readFully(data);
                    bos.write(data);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read longchat command", e);
        }
    }

    private Method findMethod() {
        try {
            for (Method m : ServerGamePacketListenerImpl.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 2 && m.getReturnType().equals(void.class)) {
                    var args = m.getParameterTypes();
                    if (args[0].equals(ServerboundChatCommandPacket.class) && args[1].equals(LastSeenMessages.class)) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find \"performChatCommand\" method", t);
        }
        return null;
    }

    private void performCommand(Player player, String command) throws Exception {
        if (ServerGamePacketListenerImpl.isChatMessageIllegal(command)) {
            player.kick(Component.translatable("multiplayer.disconnect.illegal_characters"));
            return;
        }
        command = command + " \0"; // collectSignedArguments walkaround
        var packet = new ServerboundChatCommandPacket(command, Instant.now(), 0L, ArgumentSignatures.EMPTY, new LastSeenMessages.Update(0, new BitSet()));
        performChatCommand.invoke(((CraftPlayer) player).getHandle().connection, packet, new LastSeenMessages(Collections.emptyList()));
    }

    /**
     * A walkaround to skip calling collectSignedArguments in {@link ServerGamePacketListenerImpl} performChatCommand
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().endsWith(" \0")) {
            event.setMessage(event.getMessage().substring(0, event.getMessage().length()-2));
        }
    }

}
