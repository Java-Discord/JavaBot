package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Subcommand that lets staff members decline suggestions.
 */
@Slf4j
public class DeclineSubcommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var messageIdOption = event.getOption("message-id");
        if (messageIdOption == null) {
            return Responses.error(event, "Missing required arguments.");
        }
        var messageId = messageIdOption.getAsString();
        var config = Bot.config.get(event.getGuild());
        config.getModeration().getSuggestionChannel().retrieveMessageById(messageId).queue(m -> {
            var embed = m.getEmbeds().get(0);
            m.clearReactions().queue();
            var clearEmbed = buildSuggestionClearEmbed(event.getUser(), embed, config);
            m.editMessageEmbeds(clearEmbed).queue(
                    message -> message.addReaction(config.getEmote().getFailureEmote()).queue(),
                    error -> Responses.error(event, error.getMessage()).queue());
        }, e -> log.error("Could not find suggestion message with id {}", messageId));
        return Responses.success(event, "Suggestion declined",
                String.format("Successfully declined suggestion with id `%s`", messageId));
    }

    private MessageEmbed buildSuggestionClearEmbed(User user, MessageEmbed embed, GuildConfig config) {
        return new EmbedBuilder()
                .setColor(config.getSlashCommand().getDefaultColor())
                .setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
                .setTitle("Suggestion Declined")
                .setDescription(embed.getDescription())
                .setTimestamp(embed.getTimestamp())
                .setFooter("Declined by " + user.getAsTag())
                .build();
    }
}