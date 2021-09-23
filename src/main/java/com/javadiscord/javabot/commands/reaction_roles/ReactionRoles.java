package com.javadiscord.javabot.commands.reaction_roles;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReactionRoles implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        try {
            return switch (event.getSubcommandName()) {
                case "create" -> createReactionRole(event);
                case "delete" -> deleteReactionRole(event);
                default -> Responses.warning(event, "Unknown subcommand.");
            };
        } catch (Exception e) { return Responses.error(event, "```" + e.getMessage() + "```"); }
    }

    private String buttonId(Role role, boolean b) {
        return "reaction-role:" + role.getId() + ":" + b + ":" + Instant.now();
    }

    private ReplyAction createReactionRole(SlashCommandEvent event) {
        var message =  event.getChannel().retrieveMessageById(event.getOption("message-id").getAsString()).complete();
        var buttonLabel = event.getOption("label").getAsString();
        var role = event.getOption("role").getAsRole();

        boolean permanent = event.getOption("permanent") != null && event.getOption("permanent").getAsBoolean();
        String emote = event.getOption("emote") == null ? null : event.getOption("emote").getAsString();

        List<Button> buttons = new ArrayList<>(message.getButtons());
        if (emote != null) buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel, Emoji.fromMarkdown(emote)));
        else buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel));
        message.editMessageComponents(ActionRow.of(buttons)).queue();

        var e = new EmbedBuilder()
                .setTitle("Reaction Role created")
                .setColor(Constants.GRAY)
                .addField("Channel", "<#" + event.getChannel().getId() + ">", true)
                .addField("Role", role.getAsMention(), true)
                .addField("MessageID", "```" + message.getId() + "```", false);
        if (emote != null) e.addField("Emote", "```" + emote + "```", true);
                e.addField("Button Label", "```" + buttonLabel + "```", true)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());
        Misc.sendToLog(event.getGuild(), e.build());
        return event.replyEmbeds(e.build()).setEphemeral(true);
    }

    private ReplyAction deleteReactionRole(SlashCommandEvent event) {
        var message = event.getChannel().retrieveMessageById(event.getOption("message-id").getAsString()).complete();
        String buttonLabel = event.getOption("label").getAsString();

        List<Button> buttons = new ArrayList<>(message.getActionRows().get(0).getButtons());
        for (var ignored : message.getActionRows().get(0).getButtons()) buttons.removeIf(x -> x.getLabel().equals(buttonLabel));
        if (!buttons.isEmpty()) message.editMessageComponents(ActionRow.of(buttons)).queue();
        else message.editMessageComponents().queue();

        var e = new EmbedBuilder()
                .setTitle("Reaction Role removed")
                .addField("MessageID", "```" + message.getId() + "```", false)
                .addField("Button Label", "```" + buttonLabel + "```", true)
                .setColor(Constants.GRAY)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();
        Misc.sendToLog(event.getGuild(), e);
        return event.replyEmbeds(e).setEphemeral(true);
    }
}
