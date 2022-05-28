package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.Responses;

/**
 * Command that allows members to test regex patterns.
 */
public class RegexCommand extends SlashCommand {
	public RegexCommand() {
		setCommandData(Commands.slash("regex", "Checks if the given string matches the regex pattern")
				.addOption(OptionType.STRING, "regex", "The regex pattern", true)
				.addOption(OptionType.STRING, "string", "The string which is tested", true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		var patternOption = event.getOption("regex");
		var stringOption = event.getOption("string");
		if (patternOption == null || stringOption == null) {
			Responses.error(event, "Missing required arguments").queue();
			return;
		}
		Pattern pattern;
		try {
			pattern = Pattern.compile(patternOption.getAsString());
		} catch (PatternSyntaxException e) {
			Responses.error(event, "Please provide a valid regex pattern!");
			return;
		}
		String string = stringOption.getAsString();
		if (patternOption.getAsString().length() > 1018 || string.length() > 1018) {
			Responses.warning(event, "Pattern and String cannot be longer than 1018 Characters each.").queue();
			return;
		}
		event.replyEmbeds(buildRegexEmbed(pattern.matcher(string).matches(), pattern, string, event.getGuild()).build())
				.queue();
	}

	private EmbedBuilder buildRegexEmbed(boolean matches, Pattern pattern, String string, Guild guild) {
		EmbedBuilder eb = new EmbedBuilder()
				.addField("Regex:", String.format("```%s```", pattern.toString()), true)
				.addField("String:", String.format("```%s```", string), true);
		SlashCommandConfig config = Bot.config.get(guild).getSlashCommand();
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
