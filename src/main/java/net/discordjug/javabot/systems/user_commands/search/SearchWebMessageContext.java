package net.discordjug.javabot.systems.user_commands.search;

import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * <h3>This class represents the "Search the Web" Message Context Menu command.</h3>
 * This Context Command allows members to search the internet using the Bing API.
 */
public class SearchWebMessageContext extends ContextCommand.Message {
	private final SystemsConfig systemsConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 * @param systemsConfig Configuration for various systems
	 */
	public SearchWebMessageContext(SystemsConfig systemsConfig) {
		this.systemsConfig = systemsConfig;
		setCommandData(Commands.message("Search the Web")
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		String query = event.getTarget().getContentDisplay();
		if (query.isEmpty() || query.isBlank()) {
			Responses.warnin(event, "No Content", "The message doesn't have any content to search for").queue();
			return;
		}
		event.deferReply().queue();
		SearchWebService service = new SearchWebService(systemsConfig);
		try {
			event.getHook().sendMessageEmbeds(service.buildSearchWebEmbed(query)).queue();
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.warning(event.getHook(), "No Results", "There were no results for your search. " +
					"This might be due to safe-search or because your search was too complex. Please try again.").queue();
		}
	}
}
