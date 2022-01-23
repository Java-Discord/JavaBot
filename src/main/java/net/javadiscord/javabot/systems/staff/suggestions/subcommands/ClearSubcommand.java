package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Subcommand that lets staff members clear suggestions.
 */
@Slf4j
public class ClearSubcommand implements SlashCommandHandler {
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
            var clearEmbed = buildSuggestionClearEmbed(embed, config);
            m.editMessageEmbeds(clearEmbed).queue(
                    message -> {
                        message.addReaction(config.getEmote().getUpvoteEmote()).queue();
                        message.addReaction(config.getEmote().getDownvoteEmote()).queue();
                    },
                    error -> Responses.error(event, error.getMessage()).queue());
        }, e -> log.error("Could not find suggestion message with id {}", messageId));
        return Responses.success(event, "Suggestion cleared",
                String.format("Successfully cleared suggestion with id `%s`", messageId));
    }

    private MessageEmbed buildSuggestionClearEmbed(MessageEmbed embed, GuildConfig config) {
        return new EmbedBuilder()
                .setColor(config.getSlashCommand().getDefaultColor())
                .setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
                .setTitle("Suggestion")
                .setDescription(embed.getDescription())
                .setTimestamp(embed.getTimestamp())
                .setFooter(null)
                .build();
    }
}
