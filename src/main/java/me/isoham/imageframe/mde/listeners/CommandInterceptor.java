package me.isoham.imageframe.mde.listeners;

import me.isoham.imageframe.mde.discord.DiscordService;
import me.isoham.imageframe.mde.moderation.ModerationManager;
import me.isoham.imageframe.mde.storage.UrlStatus;

import org.bukkit.Color;
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

    public CommandInterceptor(ModerationManager moderationManager, DiscordService discordService) {
        this.moderationManager = moderationManager;
        this.discordService = discordService;
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

        String url = getUrl(args);
        if (url == null) {
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

        UrlStatus status = moderationManager.check(hash);

        switch (status) {
            case APPROVED -> {
            }

            case REJECTED -> {
                event.setCancelled(true);
                player.sendMessage(Color.RED + "This image URL has been rejected by moderators.");
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
                    player.sendMessage(Color.RED + "You have too many pending moderation requests.");
                    return;
                }

                discordService.sendModerationMessage(
                        requestId,
                        player.getName(),
                        message,
                        url
                );

                player.sendMessage(Color.YELLOW + "Your image request was sent for moderation.");
            }
        }
    }

    private static @Nullable String getUrl(String[] args) {
        String sub = args[1].toLowerCase();

        String url = null;

        switch (sub) {
            case "create" -> {
                if (args.length == 6 || args.length == 7) {
                    url = args[3];
                } else if (args.length == 5 && args[4].equalsIgnoreCase("selection")) {
                    url = args[3];
                }
            }

            case "overlay" -> {
                if (args.length == 4) {
                    url = args[3];
                } else if (args.length == 5 && args[4].equalsIgnoreCase("selection")) {
                    url = args[3];
                }
            }

            case "refresh" -> {
                if (args.length >= 4) {
                    url = args[3];
                }
            }
        }
        return url;
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
}