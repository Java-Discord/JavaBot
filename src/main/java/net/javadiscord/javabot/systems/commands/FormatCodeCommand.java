package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows members to format messages.
 */
public class FormatCodeCommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event)  {
        var idOption = event.getOption("message-id");
        var formatOption = event.getOption("format");
        String format = formatOption == null ? "java" : formatOption.getAsString();
        long id;
        if (idOption == null) {
            if (event.getChannel().hasLatestMessage()) id = event.getChannel().getLatestMessageIdLong();
            else return Responses.error(event, "Missing required arguments.");
        } else id = idOption.getAsLong();
        var slashConfig = Bot.config.get(event.getGuild()).getSlashCommand();
        event.getTextChannel().retrieveMessageById(id).queue(
                m -> event.getHook().sendMessageEmbeds(buildFormatCodeEmbed(m, m.getAuthor(), format, slashConfig)).queue(),
                e -> Responses.error(event.getHook(), "Could not retrieve message.").queue());
        return event.deferReply();
    }

    private MessageEmbed buildFormatCodeEmbed(Message message, User author, String format, SlashCommandConfig config) {
        return new EmbedBuilder()
                .setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
                .setTitle("Original Message", message.getJumpUrl())
                .setColor(config.getDefaultColor())
                .setDescription(String.format("```%s\n%s\n```", format, message.getContentRaw()))
                .setFooter("Formatted as: " + format)
                .setTimestamp(Instant.now())
                .build();
    }
}
