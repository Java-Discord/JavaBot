package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /regex command.</h3>
 */
public class RegexCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public RegexCommand() {
		setSlashCommandData(Commands.slash("regex", "Checks if the given string matches the regex pattern")
				.addOption(OptionType.STRING, "regex", "The regex pattern", true)
				.addOption(OptionType.STRING, "string", "The string which is tested", true)
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping patternOption = event.getOption("regex");
		OptionMapping stringOption = event.getOption("string");
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
		event.replyEmbeds(buildRegexEmbed(pattern.matcher(string).matches(), pattern, string).build())
				.queue();
	}

	private @NotNull EmbedBuilder buildRegexEmbed(boolean matches, @NotNull Pattern pattern, String string) {
		EmbedBuilder eb = new EmbedBuilder()
				.addField("Regex:", String.format("```%s```", pattern.toString()), true)
				.addField("String:", String.format("```%s```", string), true);
		if (matches) {
			eb.setTitle("Regex Tester | ✓ Match");
			eb.setColor(Responses.Type.SUCCESS.getColor());
		} else {
			eb.setTitle("Regex Tester | ✗ No Match");
			eb.setColor(Responses.Type.ERROR.getColor());
		}
		return eb;
	}

}
