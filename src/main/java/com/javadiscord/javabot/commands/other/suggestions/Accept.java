package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.OffsetDateTime;

public class Accept implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

            Message msg;
            String messageID = event.getOption("message-id").getAsString();
            try { msg = event.getChannel().retrieveMessageById(messageID).complete(); }
            catch (IllegalArgumentException | ErrorResponseException e) { return Responses.error(event, e.getMessage()); }

            MessageEmbed msgEmbed = msg.getEmbeds().get(0);
            msg.clearReactions().queue();

            String name = msg.getEmbeds().get(0).getAuthor().getName();
            String iconUrl = msg.getEmbeds().get(0).getAuthor().getIconUrl();
            String description = msg.getEmbeds().get(0).getDescription();
            OffsetDateTime timestamp = msg.getEmbeds().get(0).getTimestamp();

            var eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getSuccessColor())
                .setAuthor(name, null, iconUrl);

            try {
                String responseFieldName = msgEmbed.getFields().get(0).getName();
                String responseFieldValue = msgEmbed.getFields().get(0).getValue();

                eb.addField(responseFieldName, responseFieldValue, false);

            } catch (IndexOutOfBoundsException e) {}

            eb.setDescription(description)
                .setTimestamp(timestamp)
                .setFooter("Accepted by " + event.getUser().getAsTag());

            msg.editMessageEmbeds(eb.build()).queue(message1 -> message1.addReaction(Bot.config.get(event.getGuild())
                    .getEmote().getSuccessEmote()).queue());
            return event.reply("Done!").setEphemeral(true);
    }
}