package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.time.OffsetDateTime;

public class Decline implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

            String messageID = event.getOption("message-id").getAsString();
            try {
                event.getChannel().retrieveMessageById(messageID).queue(msg->{
                    MessageEmbed msgEmbed = msg.getEmbeds().get(0);
                    msg.clearReactions().queue();

                    String name = msgEmbed.getAuthor().getName();
                    String iconUrl = msgEmbed.getAuthor().getIconUrl();
                    String description = msgEmbed.getDescription();
                    OffsetDateTime timestamp = msgEmbed.getTimestamp();

                    var eb = new EmbedBuilder()
                        .setColor(new Color(0xe74c3c))
                        .setAuthor(name, null, iconUrl);

                    try {
                        String responseFieldName = msgEmbed.getFields().get(0).getName();
                        String responseFieldValue = msgEmbed.getFields().get(0).getValue();

                        eb.addField(responseFieldName, responseFieldValue, false);

                    } catch (IndexOutOfBoundsException e) {}

                    eb.setDescription(description)
                        .setTimestamp(timestamp)
                        .setFooter("Declined by " + event.getUser().getAsTag());

                    msg.editMessageEmbeds(eb.build()).queue(message1 -> message1.addReaction(
                            Bot.config.get(event.getGuild()).getEmote().getFailureEmote()).queue());
                    event.reply("Done!").setEphemeral(true).queue();
                },e->Responses.error(event, e.getMessage()).queue());
                return event.deferReply();
            } catch (IllegalArgumentException e) { return Responses.error(event, e.getMessage()); }

    }
}