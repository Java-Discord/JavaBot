package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.util.regex.Pattern;

public class RegexCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) throws ResponseException {

        var patternOption = event.getOption("regex");
        var stringOption = event.getOption("string");

        if (patternOption == null) return Responses.warning(event, "Missing required regex pattern.");
        if (stringOption == null) return Responses.warning(event, "Missing required string.");

        Pattern pattern = Pattern.compile(patternOption.getAsString());
        String string = stringOption.getAsString();

        return event.replyEmbeds(buildRegexEmbed(pattern.matcher(string).matches(), pattern, string, event.getGuild()).build());
    }

    private EmbedBuilder buildRegexEmbed(boolean matches, Pattern pattern, String string, Guild guild){
        EmbedBuilder eb = new EmbedBuilder()
                .addField("Regex:", "```" + pattern.toString() + "```", true)
                .addField("String:", "```" + string + "```", true);

        if (matches) {
            eb.setTitle("Regex Tester | ✓ Match");
            eb.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor());
        } else {
            eb.setTitle("Regex Tester | ✗ No Match");
            eb.setColor(Bot.config.get(guild).getSlashCommand().getErrorColor());
        }

        return eb;
    }

}
