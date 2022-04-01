package net.javadiscord.javabot.systems.commands;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

/**
 * Command that allows members to test regex patterns.
 */
public class RegexCommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		var patternOption = event.getOption("regex");
		var stringOption = event.getOption("string");
		if (patternOption == null) return Responses.warning(event, "Missing required regex pattern.");
		if (stringOption == null) return Responses.warning(event, "Missing required string.");

		Pattern pattern;
		try {
			pattern = Pattern.compile(patternOption.getAsString());
		} catch (PatternSyntaxException e) {
			return Responses.error(event, "Invalid Regex-Pattern.");
		}
		String string = stringOption.getAsString();
		if (patternOption.getAsString().length() > 1018 || string.length() > 1018) {
			return Responses.warning(event, "Pattern and String cannot be longer than 1018 Characters each.");
		}
		return event.replyEmbeds(buildRegexEmbed(pattern.matcher(string).matches(), pattern, string, event.getGuild()).build());
	}

	private EmbedBuilder buildRegexEmbed(boolean matches, Pattern pattern, String string, Guild guild) {
		EmbedBuilder eb = new EmbedBuilder()
				.addField("Regex:", String.format("```%s```", pattern.toString()), true)
				.addField("String:", String.format("```%s```", string), true);
		var config = Bot.config.get(guild).getSlashCommand();
		if (matches) {
			eb.setTitle("Regex Tester | ✓ Match");
			eb.setColor(config.getSuccessColor());
		} else {
			eb.setTitle("Regex Tester | ✗ No Match");
			eb.setColor(config.getErrorColor());
		}
		return eb;
	}

}
