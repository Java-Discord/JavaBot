package com.javadiscord.javabot.commands.reaction_roles.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeleteReactionRole implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var message = event.getChannel().retrieveMessageById(event.getOption("message-id").getAsString()).complete();
        String buttonLabel = event.getOption("label").getAsString();

        List<Button> buttons = new ArrayList<>(message.getActionRows().get(0).getButtons());
        for (var ignored : message.getActionRows().get(0).getButtons()) buttons.removeIf(x -> x.getLabel().equals(buttonLabel));
        if (!buttons.isEmpty()) {
            message.editMessageComponents(ActionRow.of(buttons)).queue();
        } else {
            message.editMessageComponents().queue();
        }

        var e = new EmbedBuilder()
                .setTitle("Reaction Role removed")
                .addField("MessageID", "```" + message.getId() + "```", false)
                .addField("Button Label", "```" + buttonLabel + "```", true)
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();
        Misc.sendToLog(event.getGuild(), e);
        return event.replyEmbeds(e).setEphemeral(true);
    }
}
