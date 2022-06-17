package net.javadiscord.javabot.systems.custom_commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.List;

/**
 * Handler class for the "/customcommand"-slash commands.
 */
// TODO: Re-implement Custom Commands
public class CustomCommandHandler
//		extends SlashCommand
{
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
//	public CustomCommandHandler() {
//		addSubcommand("create", new CreateSubcommand());
//		addSubcommand("delete", new DeleteSubcommand());
//		addSubcommand("edit", new EditSubcommand());
//	}

	/**
	 * Cleans the given String by removing all whitespaces and slashes, so it can be used for custom commands.
	 *
	 * @param s The string that should be cleaned.
	 * @return The cleaned string.
	 */
	public static String cleanString(String s) {
		return s.trim()
				.replaceAll("\\s+", "")
				.replace("/", "");
	}

	/**
	 * Replies with all available custom commands.
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
//	public static List<Command.Choice> replyCustomCommands(CommandAutoCompleteInteractionEvent event) {
//		List<Command.Choice> choices = new ArrayList<>(25);
//		try (Connection con = Bot.dataSource.getConnection()) {
//			CustomCommandRepository repo = new CustomCommandRepository(con);
//			List<CustomCommand> commands = repo.getCustomCommandsByGuildId(event.getGuild().getIdLong()).stream().limit(25).toList();
//			commands.forEach(command -> choices.add(new Command.Choice("/" + command.getName(), command.getName())));
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return choices;
//	}

//	@Override
//	public AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
//		List<Command.Choice> choices = switch (event.getSubcommandName()) {
//			case "delete", "edit" -> CustomCommandHandler.replyCustomCommands(event);
//			default -> List.of();
//		};
//		return event.replyChoices(AutocompleteUtils.filterChoices(event, choices));
//	}
}