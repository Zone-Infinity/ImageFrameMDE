package me.isoham.imageframe.mde.config;

import org.bukkit.plugin.java.JavaPlugin;

public class Config {

    private final JavaPlugin plugin;
    private final MessageConfig messages;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new MessageConfig(plugin);

        load();
    }

    private void load() {
        // FileConfiguration config = plugin.getConfig();

        // Nothing right now :)
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public MessageConfig getMessages() {
        return messages;
    }

    public int getMaxPendingRequestsPerPlayer() {
        return plugin.getConfig().getInt("moderation.max-pending-requests-per-player", 3);
    }
}