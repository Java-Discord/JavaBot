package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;

public class Embed {

    public static void execute(SlashCommandEvent event, String title, String description, String autorname, String authorurl, String authoriconurl, String thumb, String img, String color) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            try {

                var eb = new EmbedBuilder();

                eb.setTitle(title);
                eb.setDescription(description);
                eb.setAuthor(autorname, authorurl, authoriconurl);
                eb.setImage(img);
                eb.setThumbnail(thumb);

                if (!(color == null)) {
                    try {
                        eb.setColor(Color.decode(color));
                    } catch (Exception e) {
                        eb.setColor(Constants.GRAY);
                    }
                }

                event.replyEmbeds(eb.build()).queue();

            } catch (Exception e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
            } else { event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }

            }
        }