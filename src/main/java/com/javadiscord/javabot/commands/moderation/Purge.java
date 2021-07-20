package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.util.List;

public class Purge implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        int amount = (int) event.getOption("amount").getAsLong();
        boolean nuke;
        try {
            nuke = event.getOption("nuke-channel").getAsBoolean();
        } catch (NullPointerException e) {
            nuke = false;
        }

            try {
                if (nuke) {
                    event.getTextChannel().createCopy().queue();
                    event.getTextChannel().delete().queue();
                    return;
                }

                MessageHistory history = new MessageHistory(event.getChannel());
                List<Message> messages = history.retrievePast(amount + 1).complete();

                //for (int i = messages.size() - 1; i > 0; i--) messages.get(i).delete().queue();
                event.getTextChannel().deleteMessages(messages).complete();

                var e = new EmbedBuilder()
                    .setColor(new Color(0x2F3136))
                    .setTitle("Successfully deleted **" + amount + " messages** :broom:")
                    .build();

                event.replyEmbeds(e).queue();

            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                event.replyEmbeds(Embeds.purgeError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            }
        }
    }