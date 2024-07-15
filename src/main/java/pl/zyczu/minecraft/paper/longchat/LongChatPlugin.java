package pl.zyczu.minecraft.paper.longchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class LongChatPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, "longchat:cmdpart", new PartCommandMessageListener(this));
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

}
