package net.javadiscord.javabot.systems.user_commands.search;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * <h3>This class represents the /search-web command.</h3>
 * This Command allows members to search the internet using the Bing API.
 */
public class SearchWebCommand extends SlashCommand {
	private final SystemsConfig systemsConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param systemsConfig Configuration for various systems
	 */
	public SearchWebCommand(SystemsConfig systemsConfig) {
		this.systemsConfig = systemsConfig;
		setCommandData(Commands.slash("search-web", "Searches the web by turning your text-input into a search query")
				.setGuildOnly(true)
				.addOption(OptionType.STRING, "query", "Text that will be converted into a search query", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping queryMapping = event.getOption("query");
		if (queryMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		event.deferReply().queue();
		SearchWebService service = new SearchWebService(systemsConfig);
		try {
			event.getHook().sendMessageEmbeds(service.buildSearchWebEmbed(queryMapping.getAsString())).queue();
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.warning(event.getHook(), "No Results", "There were no results for your search. " +
					"This might be due to safe-search or because your search was too complex. Please try again.").queue();
		}
	}
}
