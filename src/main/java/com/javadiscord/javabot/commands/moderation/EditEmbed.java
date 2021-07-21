package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.Arrays;

public class EditEmbed implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "edit": editEmbed(event); break;
            case "from-message": editEmbedFromLink(event); break;
        }
    }

    void editEmbedFromLink(SlashCommandEvent event) {
        String oriLink = event.getOption("original-link").getAsString();
        String newLink = event.getOption("new-link").getAsString();

        String[] oriValue = oriLink.split("/");
        String[] newValue = newLink.split("/");

        Message oriMessage, newMessage;
        try {
            TextChannel oriChannel = event.getGuild().getTextChannelById(oriValue[5]);
            oriMessage = oriChannel.retrieveMessageById(oriValue[6]).complete();
        } catch (Exception e) {
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        try {
            TextChannel newChannel = event.getGuild().getTextChannelById(newValue[5]);
            newMessage = newChannel.retrieveMessageById(newValue[6]).complete();
        } catch (Exception e) {
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(oriMessage.getEmbeds().get(0).getColor())
                    .setDescription(newMessage.getContentRaw());

            oriMessage.editMessageEmbeds(eb.build()).queue();
            event.reply("Done!").setEphemeral(true).queue();
    }

    void editEmbed(SlashCommandEvent event) {

        String link = event.getOption("link").getAsString();
        String[] value = link.split("/");

        Message message;
        try {
            TextChannel channel = event.getGuild().getTextChannelById(value[5]);
            message = channel.retrieveMessageById(value[6]).complete();
        } catch (Exception e) {
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        String title = event.getOption("title").getAsString();
        String description = event.getOption("description").getAsString();

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(message.getEmbeds().get(0).getColor())
                .setTitle(title)
                .setDescription(description);

        message.editMessageEmbeds(eb.build()).queue();
        event.reply("Done!").setEphemeral(true).queue();
    }
}