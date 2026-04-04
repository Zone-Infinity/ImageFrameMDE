package me.isoham.imageframe.mde.listeners;

import me.isoham.imageframe.mde.config.Config;
import me.isoham.imageframe.mde.config.MessageConfig;
import me.isoham.imageframe.mde.discord.DiscordService;
import me.isoham.imageframe.mde.moderation.ModerationManager;
import me.isoham.imageframe.mde.storage.URLStatus;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


/**
 * Intercepts the following commands:
 * /imageframe create <name> <url> <width> <height> - Create a new image map
 * /imageframe create <name> <url> selection - Create a new image map and put it directly in your selected item frames
 * /imageframe create <name> <url> <width> <height> combined - Create a new image map and get its Combined ImageMap item
 * /imageframe overlay <name> <url> - Create a new image map that adds an overlay on a Minecraft Vanilla map you are holding
 * /imageframe overlay <name> <url> selection - Create a new image map that adds an overlay on a Minecraft Vanilla map in your selected item frames
 * <p>
 * If url present then sure:
 * /imageframe refresh [optional:image_name] [optional:new_url] - Refresh a map you've created from source url
 */

public class CommandInterceptor implements Listener {
    private final ModerationManager moderationManager;
    private final DiscordService discordService;
    private final Config config;

    public CommandInterceptor(ModerationManager moderationManager, DiscordService discordService, Config config) {
        this.moderationManager = moderationManager;
        this.discordService = discordService;
        this.config = config;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();
        Player player = event.getPlayer();

        if (!message.regionMatches(true, 0, "/imageframe", 0, 11)) {
            return;
        }

        String[] args = message.split("\\s+");

        if (args.length < 2) {
            return;
        }

        String url = getURL(args);
        if (url == null) {
            event.setCancelled(true);
            player.sendMessage(config.getMessages().get(MessageConfig.Message.INVALID_USAGE));
            return;
        }

        // TODO: Conditional image hashing for mutable CDN hosts.
        //
        // Some image hosts allow the image content to change while keeping the same URL.
        // Example exploit:
        //
        // 1. Player uploads an image and gets it approved.
        // 2. Player later replaces the image at the same URL.
        // 3. Running `/imageframe refresh` loads the new content without moderation.
        //
        // Known hosts that may allow this:
        //
        // - imgur CDN (i.imgur.com)
        // - Google Cloud Storage (storage.googleapis.com)
        // - Discord CDN (cdn.discordapp.com, media.discordapp.net)
        // - Mojang textures CDN (textures.minecraft.net)
        //
        // Planned solution:
        //
        // For URLs matching these hosts:
        //
        //   1. Download the image
        //   2. Compute SHA-256 hash of the image bytes
        //   3. Store and moderate based on the hash instead of the URL
        //
        // This prevents moderation bypass when image content changes.
        //
        String hash = hash(url);

        if (!isWhitelisted(url)) {
            event.setCancelled(true);
            player.sendMessage(config.getMessages().get(MessageConfig.Message.URL_NOT_WHITELISTED));
            return;
        }

        URLStatus status = moderationManager.check(hash);

        switch (status) {
            case APPROVED -> {
            }

            case REJECTED -> {
                event.setCancelled(true);
                player.sendMessage(config.getMessages().get(MessageConfig.Message.URL_REJECTED));
            }

            case PENDING -> {
                event.setCancelled(true);
                player.sendMessage(config.getMessages().get(MessageConfig.Message.REQUEST_ALREADY_PENDING));
            }

            case UNKNOWN -> {
                event.setCancelled(true);

                String requestId = moderationManager.createRequest(
                        player,
                        message,
                        url,
                        hash
                );

                if (requestId == null) {
                    player.sendMessage(config.getMessages().get(MessageConfig.Message.REQUEST_LIMIT_REACHED));
                    return;
                }

                discordService.sendModerationMessage(
                        requestId,
                        player.getName(),
                        message,
                        url
                );

                player.sendMessage(config.getMessages().get(MessageConfig.Message.REQUEST_SUBMITTED));
            }
        }
    }

    // Gets the url and also validates command partially
    private static @Nullable String getURL(String[] args) {
        if (args.length < 4) {
            return null;
        }

        String sub = args[1].toLowerCase();

        return switch (sub) {
            case "create" -> {
                // /imageframe create <name> <url> selection
                if (args.length == 5 && args[4].equalsIgnoreCase("selection")) {
                    yield args[3];
                }

                // /imageframe create <name> <url> <width> <height> [options]
                if (args.length >= 6) {
                    yield args[3];
                }

                yield null;
            }

            case "overlay", "refresh" -> args[3];
            default -> null;
        };
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isWhitelisted(String url) {
        for (String prefix : config.getImageURLWhitelist()) {
            if (url.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}