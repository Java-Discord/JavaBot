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

import java.time.OffsetDateTime;

public class Accept extends Command {

    public Accept () {
        this.name = "accept";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "accepts the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Emote check = event.getGuild().getEmotesByName("check", false).get(0);

                Message msg = event.getChannel().retrieveMessageById(args[0]).complete();
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

                try {

                    eb.setImage(msgEmbed.getImage().getUrl());

                } catch (IndexOutOfBoundsException e) {}

                eb.setDescription(description)
                        .setTimestamp(timestamp)
                        .setFooter("Accepted by " + event.getAuthor().getAsTag());

                msg.editMessage(eb.build()).queue(message1 -> message1.addReaction(check).queue());

                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("accept MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
        }
    }