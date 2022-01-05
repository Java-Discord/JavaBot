package net.javadiscord.javabot.systems.staff.reaction_roles.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.Misc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DeleteReactionRole implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        event.getChannel().retrieveMessageById(event.getOption("message-id").getAsString()).queue(message->{
            String buttonLabel = event.getOption("label").getAsString();

            List<Button> buttons = new ArrayList<>(message.getActionRows().get(0).getButtons());
            buttons.removeIf(button -> button.getLabel().equals(buttonLabel));
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
                    .setTimestamp(Instant.now())
                    .build();
            Misc.sendToLog(event.getGuild(), e);
            event.replyEmbeds(e).queue();
        });
        return event.deferReply(true);
    }
}
