package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;

public class Decline extends Command {

    public Decline () {
        this.name = "decline";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "declines the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Message msg = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed msgEmbed = msg.getEmbeds().get(0);
                msg.clearReactions().queue();

                String name = msgEmbed.getAuthor().getName();
                String iconUrl = msgEmbed.getAuthor().getIconUrl();
                String description = msgEmbed.getDescription();
                OffsetDateTime timestamp = msgEmbed.getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(new Color(0xe74c3c))
                        .setAuthor(name, null, iconUrl);

                try {
                    String ResponseFieldName = msgEmbed.getFields().get(0).getName();
                    String ResponseFieldValue = msgEmbed.getFields().get(0).getValue();

                    eb.addField(ResponseFieldName, ResponseFieldValue, false);

                } catch (IndexOutOfBoundsException e) {}

                try {

                    eb.setImage(msgEmbed.getImage().getUrl());

                } catch (IndexOutOfBoundsException e) {}

                eb.setDescription(description)
                        .setTimestamp(timestamp)
                        .setFooter("Declined by " + event.getAuthor().getAsTag());

                msg.editMessage(eb.build()).queue(message1 -> message1.addReaction(Constants.REACTION_FAILURE).queue());

                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("decline MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
    }
}