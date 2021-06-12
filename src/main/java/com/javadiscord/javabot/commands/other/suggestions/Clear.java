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

public class Clear extends Command {

    public Clear () {
        this.name = "clear";
        this.category = new Category("OTHER");
        this.arguments = "<ID>";
        this.help = "clears the given submission";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            try {

                Message msg = event.getChannel().retrieveMessageById(args[0]).complete();
                MessageEmbed msgEmbed = msg.getEmbeds().get(0);
                msg.clearReactions().queue();

                String name = msg.getEmbeds().get(0).getAuthor().getName();
                String iconUrl = msg.getEmbeds().get(0).getAuthor().getIconUrl();
                String description = msg.getEmbeds().get(0).getDescription();
                OffsetDateTime timestamp = msg.getEmbeds().get(0).getTimestamp();

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(new Color(0x2F3136))
                        .setAuthor(name, null, iconUrl)
                        .setDescription(description)
                        .setTimestamp(timestamp);

                try {

                    eb.setImage(msgEmbed.getImage().getUrl());

                } catch (IndexOutOfBoundsException e) {}

                msg.editMessage(eb.build()).queue(message1 -> {
                    message1.addReaction(Constants.REACTION_UPVOTE).queue();
                    message1.addReaction(Constants.REACTION_DOWNVOTE).queue();
                });

                event.getMessage().delete().queue();

            } catch (Exception e){
                event.reply(Embeds.syntaxError("clear MessageID", event));
            }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }
    }
}
