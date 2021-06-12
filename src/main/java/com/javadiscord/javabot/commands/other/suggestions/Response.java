package com.javadiscord.javabot.commands.other.suggestions;

import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Arrays;

public class Response extends Command {

    public Response () {
        this.name = "response";
        this.aliases = new String[]{ "respond" };
        this.category = new Category("OTHER");
        this.arguments = "<ID> <Text>";
        this.help = "adds a response to the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                String[] MessageArg = Arrays.copyOfRange(args, 1, args.length);
                StringBuilder builder = new StringBuilder();
                for (String value : MessageArg) {
                    builder.append(value + " ");
                }
                
                String text = builder.substring(0, builder.toString().length() - 1);

                Message msg = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed msgEmbed = msg.getEmbeds().get(0);

                String name = msgEmbed.getAuthor().getName();
                String iconUrl = msgEmbed.getAuthor().getIconUrl();
                String description = msgEmbed.getDescription();
                Color color = msgEmbed.getColor();
                OffsetDateTime timestamp = msgEmbed.getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(color)
                        .setAuthor(name, null, iconUrl)
                        .setDescription(description)
                        .addField("→ Response from " + event.getAuthor().getAsTag(), text, false)
                        .setTimestamp(timestamp);

                try {

                    eb.setImage(msgEmbed.getImage().getUrl());

                } catch (IndexOutOfBoundsException e) {}

                msg.editMessage(eb.build()).queue();
                event.getMessage().delete().queue();

            } catch (Exception e) {
                event.reply(Embeds.syntaxError("response MessageID Text", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
        }
    }
