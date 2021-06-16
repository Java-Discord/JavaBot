package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.awt.*;
import java.time.OffsetDateTime;

public class Respond {

    public static void execute(SlashCommandEvent event, String messageID, String text) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                Message msg = null;

                try { msg = event.getChannel().retrieveMessageById(messageID).complete(); }
                catch (IllegalArgumentException | ErrorResponseException e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }

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

                msg.editMessage(e).queue();
                event.reply("Done!").setEphemeral(true).queue();

            } else { event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
        }
    }
