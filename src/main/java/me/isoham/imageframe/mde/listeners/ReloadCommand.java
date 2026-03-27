package me.isoham.imageframe.mde.listeners;

import me.isoham.imageframe.mde.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class ReloadCommand implements CommandExecutor {

    private final Config config;

    public ReloadCommand(Config config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        if (!sender.hasPermission("imageframemde.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have permission to run this command."));
            return true;
        }

        config.reload();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aImageFrameMDE configuration reloaded."));

        return true;
    }
}
