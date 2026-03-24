package me.isoham.imageframe.mde.discord;

import me.isoham.imageframe.mde.moderation.ModerationManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Instant;

public class DiscordService {
    private final JDA jda;
    private final long channelId;

    public DiscordService(String token, long channelId, ModerationManager moderationManager) throws Exception {
        this.channelId = channelId;
        this.jda = JDABuilder.createDefault(token)
                .addEventListeners(new ModerationButtonListener(moderationManager))
                .disableIntents(GatewayIntent.GUILD_PRESENCES)
                .build()
                .awaitReady();
    }

    public void sendModerationMessage(
            String requestId,
            String playerName,
            String command,
            String url
    ) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("ImageFrame Moderation Request")
                .addField("Player", playerName, false)
                .addField("URL", url, false)
                .addField("Command", command, false)
                .setImage(url)
                .setTimestamp(Instant.now())
                .build();

        channel.sendMessageEmbeds(embed)
                .addComponents(
                        ActionRow.of(
                                Button.success("approve:" + requestId, "Approve"),
                                Button.danger("reject:" + requestId, "Reject")
                        )
                )
                .queue();
    }

    public void shutdown() {
        jda.shutdown();
    }
}