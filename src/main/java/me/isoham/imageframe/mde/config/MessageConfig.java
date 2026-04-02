package me.isoham.imageframe.mde.config;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageConfig {

    private final JavaPlugin plugin;

    public MessageConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(Message message) {
        String raw = plugin.getConfig().getString(message.path, message.defaultMessage);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public enum Message {

        REQUEST_SUBMITTED(
                "messages.request.submitted",
                "&eYour image request was sent for moderation."
        ),

        REQUEST_ALREADY_PENDING(
                "messages.request.already_pending",
                "&6This image is already awaiting moderation. Please wait."
        ),

        REQUEST_LIMIT_REACHED(
                "messages.request.limit_reached",
                "&cYou have too many pending moderation requests."
        ),

        URL_REJECTED(
                "messages.url.rejected",
                "&cThis image URL has been rejected by moderators."
        ),

        MODERATION_APPROVED(
                "messages.moderation.approved",
                "&aYour image request has been approved by a moderator."
        ),

        MODERATION_REJECTED(
                "messages.moderation.rejected",
                "&cYour image request was rejected by a moderator."
        ),

        URL_NOT_WHITELISTED(
                "messages.moderation.not_whitelisted",
                "&cYour url type isn't whitelisted. Use a discord url."
        );

        public final String path;
        public final String defaultMessage;

        Message(String path, String defaultMessage) {
            this.path = path;
            this.defaultMessage = defaultMessage;
        }
    }
}
