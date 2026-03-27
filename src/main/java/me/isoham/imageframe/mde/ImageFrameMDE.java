package me.isoham.imageframe.mde;

import me.isoham.imageframe.mde.config.Config;
import me.isoham.imageframe.mde.discord.DiscordService;
import me.isoham.imageframe.mde.listeners.CommandInterceptor;
import me.isoham.imageframe.mde.listeners.ReloadCommand;
import me.isoham.imageframe.mde.moderation.ModerationManager;
import me.isoham.imageframe.mde.storage.DatabaseManager;
import me.isoham.imageframe.mde.storage.RequestRepository;
import me.isoham.imageframe.mde.storage.URLRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class ImageFrameMDE extends JavaPlugin {
    private DiscordService discordService;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String token = getConfig().getString("discord.token");
        long channelId = getConfig().getLong("discord.channel-id");

        Config config = new Config(this);

        try {
            // Database Service
            databaseManager = new DatabaseManager(this);
            URLRepository urlRepository = new URLRepository(databaseManager.getConnection());
            RequestRepository requestRepository = new RequestRepository(databaseManager.getConnection());
            getLogger().info("Database initialized");

            // Moderation Manager
            ModerationManager moderationManager = new ModerationManager(this, urlRepository, requestRepository, config);
            moderationManager.loadStartupCaches();

            long cutoff = System.currentTimeMillis() - Duration.ofDays(30).toMillis(); // delete pending requests 30 days old
            requestRepository.deleteOldRequests(cutoff);

            // Discord Service
            discordService = new DiscordService(token, channelId, moderationManager);
            getLogger().info("Discord bot started.");

            // Register event listeners for new/updated images
            // getServer().getPluginManager().registerEvents(new ImageMapListener(), this);
            getServer().getPluginManager().registerEvents(new CommandInterceptor(moderationManager, discordService, config), this);

            getCommand("mdereload").setExecutor(new ReloadCommand(config));
        } catch (Exception e) {
            getLogger().severe("Failed to start Plugin: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
    }

    @Override
    public void onDisable() {
        if (discordService != null) {
            discordService.shutdown();
        }

        try {
            databaseManager.close();
        } catch (Exception e) {
            getLogger().severe("Error while closing database : " + e.getLocalizedMessage());
        }
    }

}
