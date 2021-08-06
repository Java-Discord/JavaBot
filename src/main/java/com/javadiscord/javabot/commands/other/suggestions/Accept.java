package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.OffsetDateTime;

public class Accept implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            Message msg = null;
            String messageID = event.getOption("message-id").getAsString();
            try { msg = event.getChannel().retrieveMessageById(messageID).complete(); }
            catch (IllegalArgumentException | ErrorResponseException e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }

            MessageEmbed msgEmbed = msg.getEmbeds().get(0);
            msg.clearReactions().queue();

            String name = msg.getEmbeds().get(0).getAuthor().getName();
            String iconUrl = msg.getEmbeds().get(0).getAuthor().getIconUrl();
            String description = msg.getEmbeds().get(0).getDescription();
            OffsetDateTime timestamp = msg.getEmbeds().get(0).getTimestamp();

            EmbedBuilder eb = new EmbedBuilder()
                .setColor(Constants.GREEN)
                .setAuthor(name, null, iconUrl);

            try {
                String responseFieldName = msgEmbed.getFields().get(0).getName();
                String responseFieldValue = msgEmbed.getFields().get(0).getValue();

                eb.addField(responseFieldName, responseFieldValue, false);

            } catch (IndexOutOfBoundsException e) {}

            eb.setDescription(description)
                .setTimestamp(timestamp)
                .setFooter("Accepted by " + event.getUser().getAsTag());

            msg.editMessageEmbeds(eb.build()).queue(message1 -> message1.addReaction(Constants.REACTION_SUCCESS).queue());
            return event.reply("Done!").setEphemeral(true);

        } else {
            return event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }
}