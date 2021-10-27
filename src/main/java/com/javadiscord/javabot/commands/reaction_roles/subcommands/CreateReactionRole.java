package com.javadiscord.javabot.commands.reaction_roles.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.utils.Misc;
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
import java.util.List;

public class CreateReactionRole implements SlashCommandHandler {

    /**
     * Represents the Button id for a Reaction Role Button.
     * The id consists of:
     <ol>
     *     <li>"reaction-role:" - specifies the Button Type (see {@link com.javadiscord.javabot.events.InteractionListener})</li>
     *     <li>"role.getID():" - The ID of the Role the Bot should give upon Interaction</li>
     *     <li>"permanent:" - Specifies if the Role is permanent (see {@link com.javadiscord.javabot.events.InteractionListener})</li>
     *     <li>"Instant.now();" - Value to avoid Buttons with the same ID.</li>
     * </ol>
     *
     * (This may be improved in the future.)
     *
     * @param role The Role the Bot should give upon Interaction
     * @param permanent Specifies if the Role is permanent
     */
    private String buttonId(Role role, boolean permanent) {
        return "reaction-role:" + role.getId() + ":" + permanent + ":" + Instant.now();
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var buttonLabel = event.getOption("label").getAsString();
        var role = event.getOption("role").getAsRole();

        boolean permanent = event.getOption("permanent") != null && event.getOption("permanent").getAsBoolean();
        String emote = event.getOption("emote") == null ? null : event.getOption("emote").getAsString();

        event.getChannel().retrieveMessageById(event.getOption("message-id").getAsString()).queue(message->{
            List<Button> buttons = new ArrayList<>(message.getButtons());
            if (emote != null) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel, Emoji.fromMarkdown(emote)));
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel));
            }

            message.editMessageComponents(ActionRow.of(buttons)).queue();

            var e = new EmbedBuilder()
                    .setTitle("Reaction Role created")
                    .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                    .addField("Channel", "<#" + event.getChannel().getId() + ">", true)
                    .addField("Role", role.getAsMention(), true)
                    .addField("MessageID", "```" + message.getId() + "```", false);
            if (emote != null) e.addField("Emote", "```" + emote + "```", true);
            e.addField("Button Label", "```" + buttonLabel + "```", true)
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());
            Misc.sendToLog(event.getGuild(), e.build());
            event.replyEmbeds(e.build()).queue();
        });

        return event.deferReply(true);
    }
}
