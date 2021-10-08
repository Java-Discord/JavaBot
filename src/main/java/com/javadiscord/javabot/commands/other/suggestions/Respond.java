package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.time.OffsetDateTime;

public class Respond implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

            Message msg;
            String messageID = event.getOption("message-id").getAsString();
            String text = event.getOption("text").getAsString();
            try { msg = event.getChannel().retrieveMessageById(messageID).complete(); }
            catch (IllegalArgumentException | ErrorResponseException e) { return Responses.error(event, e.getMessage()); }

            MessageEmbed msgEmbed = msg.getEmbeds().get(0);

            String name = msgEmbed.getAuthor().getName();
            String iconUrl = msgEmbed.getAuthor().getIconUrl();
            String description = msgEmbed.getDescription();
            Color color = msgEmbed.getColor();
            OffsetDateTime timestamp = msgEmbed.getTimestamp();

            var e = new EmbedBuilder()
                .setColor(color)
                .setAuthor(name, null, iconUrl)
                .setDescription(description)
                .addField("→ Response from " + event.getUser().getAsTag(), text, false)
                .setTimestamp(timestamp)
                .build();

            msg.editMessageEmbeds(e).queue();
            return event.reply("Done!").setEphemeral(true);
    }
}
