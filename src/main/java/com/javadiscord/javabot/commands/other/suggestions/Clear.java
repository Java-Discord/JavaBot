package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.time.OffsetDateTime;

public class Clear implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

            Message msg;
            String messageID = event.getOption("message-id").getAsString();
            try { msg = event.getChannel().retrieveMessageById(messageID).complete(); }
            catch (IllegalArgumentException | ErrorResponseException e) { return Responses.error(event, e.getMessage()); }

            msg.clearReactions().queue();

            String name = msg.getEmbeds().get(0).getAuthor().getName();
            String iconUrl = msg.getEmbeds().get(0).getAuthor().getIconUrl();
            String description = msg.getEmbeds().get(0).getDescription();
            OffsetDateTime timestamp = msg.getEmbeds().get(0).getTimestamp();

            var eb = new EmbedBuilder()
                .setColor(Color.decode(
                        Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor()))
                .setAuthor(name, null, iconUrl)
                .setDescription(description)
                .setTimestamp(timestamp);

            var config = Bot.config.get(event.getGuild()).getEmote();
            msg.editMessageEmbeds(eb.build()).queue(message1 -> {
                message1.addReaction(config.getUpvoteReaction()).queue();
                message1.addReaction(config.getDownvoteReaction()).queue();
            });
            return event.reply("Done!").setEphemeral(true);
    }
}
