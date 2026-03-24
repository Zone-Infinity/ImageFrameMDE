package me.isoham.imageframe.mde.discord;

import me.isoham.imageframe.mde.moderation.ModerationManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class ModerationButtonListener extends ListenerAdapter {

    private final ModerationManager moderationManager;

    public ModerationButtonListener(ModerationManager moderationManager) {
        this.moderationManager = moderationManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        String moderator = event.getUser().getName();

        if (componentId.startsWith("approve:")) {
            String requestId = componentId.substring(8);
            if (moderationManager.approve(requestId, moderator)) {
                updateMessage(event, "✅ Approved by " + moderator, Color.GREEN);
                event.reply("Request approved.").setEphemeral(true).queue();
            } else {
                event.reply("⚠️ This moderation request is no longer valid.")
                        .setEphemeral(true)
                        .queue();
            }

            return;
        }

        if (componentId.startsWith("reject:")) {
            String requestId = componentId.substring(7);
            if (!moderationManager.reject(requestId, moderator)) {
                updateMessage(event, "❌ Rejected by " + moderator, Color.RED);
                event.reply("Request rejected.").setEphemeral(true).queue();
            } else {
                event.reply("⚠️ This moderation request is no longer valid.")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    private void updateMessage(ButtonInteractionEvent event, String title, Color color) {
        var embed = event.getMessage().getEmbeds().getFirst();
        var builder = new EmbedBuilder(embed);

        builder.setTitle(title);
        builder.setColor(color);

        event.getMessage()
                .editMessageEmbeds(builder.build())
                .queue();
    }
}