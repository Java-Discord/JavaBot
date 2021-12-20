package net.javadiscord.javabot.systems.commands.staff.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

/**
 * Subcommand that lets staff members decline suggestions.
 */
public class DeclineSubcommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        OptionMapping messageIdOption = event.getOption("message-id");
        if (messageIdOption == null) {
            return Responses.error(event, "Missing required arguments.");
        }

        String messageId = messageIdOption.getAsString();
        Message message;
        try {
            message = event.getChannel().retrieveMessageById(messageId).complete();
        } catch (Exception exception) {
            return Responses.error(event, exception.getMessage());
        }

        MessageEmbed embed = message.getEmbeds().get(0);
        message.clearReactions().queue();

        var e = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor())
                .setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
                .setDescription(embed.getDescription())
                .setTimestamp(embed.getTimestamp())
                .setFooter("Declined by " + event.getUser().getAsTag());

        if (!embed.getFields().isEmpty()) {
            for (var field : embed.getFields()) {
                e.addField(field.getName(), field.getValue(), field.isInline());
            }
        }

        message.editMessageEmbeds(e.build()).queue(
                m -> m.addReaction(Bot.config.get(event.getGuild()).getEmote().getFailureEmote()).queue(),
                error -> Responses.error(event, error.getMessage()).queue());

        return Responses.success(event, "Suggestion declined",
                String.format("Successfully declined suggestion with id `%s`", message.getId()));
    }
}