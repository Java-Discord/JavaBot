package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class EditEmbed implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        String mID = event.getOption("messageid").getAsString();
        String title = event.getOption("title").getAsString();
        String description = event.getOption("description").getAsString();
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            Message message;
            try {
                message = event.getChannel().retrieveMessageById(mID).complete();
            } catch (Exception e) {
                event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                return;
            }

            EmbedBuilder eb = new EmbedBuilder()
                .setColor(message.getEmbeds().get(0).getColor())
                .setTitle(title)
                .setDescription(description);

            message.editMessage(eb.build()).queue();
            event.reply("Done!").setEphemeral(true).queue();

        } else {
            event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}