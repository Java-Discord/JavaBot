package net.javadiscord.javabot.systems.custom_commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.systems.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.systems.custom_commands.model.CustomCommand;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to manage the so-called "Custom Commands", which are
 * basically customizable Slash Commands.
 */
@Slf4j
public class CustomCommandManager extends ListenerAdapter {
	private static final Map<String, CustomCommand> loadedCommands;

	static {
		loadedCommands = new HashMap<>();
	}

	private final JDA jda;
	private final DataSource dataSource;
	private final Map<Long, List<Command>> existingCommands;

	/**
	 * The constructor of this class.
	 *
	 * @param jda        The {@link JDA} instance.
	 * @param dataSource The {@link DataSource} which is used to make connections to the database.
	 */
	public CustomCommandManager(@NotNull JDA jda, @NotNull DataSource dataSource) {
		this.jda = jda;
		this.dataSource = dataSource;
		this.existingCommands = new HashMap<>();
	}

	/**
	 * Cleans the given String by removing all whitespaces and slashes, so it can be used for custom commands.
	 *
	 * @param s The string that should be cleaned.
	 * @return The cleaned string.
	 */
	public static @NotNull String cleanString(@NotNull String s) {
		return s.trim()
				.replaceAll("\\s+", "")
				.replace("/", "");
	}

	/**
	 * Checks if the given command already exists in the provided guild.
	 *
	 * @param existing A {@link List} that contains the guild's commands.
	 * @param name     The command's name. (without the "/")
	 * @return Whether the command exists.
	 */
	public static boolean doesSlashCommandExist(@NotNull List<Command> existing, String name) {
		return existing.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name));
	}

	/**
	 * Replies with all available custom commands.
	 *
	 * @return A {@link List} with all Option Choices.
	 */
	public static @NotNull List<Command.Choice> replyCustomCommands() {
		List<Command.Choice> choices = new ArrayList<>(25);
		for (CustomCommand command : loadedCommands.values()) {
			choices.add(new Command.Choice("/" + command.getName(), command.getName()));
		}
		return choices;
	}

	/**
	 * Iterates through all guilds and loads their corresponding {@link CustomCommand}s.
	 *
	 * @throws SQLException If an error occurs.
	 */
	public void init() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			List<CustomCommand> commands = getCustomCommands(guild);
			guild.retrieveCommands().queue(retrieved -> {
				existingCommands.put(guild.getIdLong(), retrieved);
				commands.forEach(c ->
						guild.upsertCommand(c.toSlashCommandData()).queue(
								s -> {
									loadedCommands.put(c.getName(), c);
									commands.remove(c);
								}, err -> log.error("Could not upsert \"/{}\": ", c.getName())));
				log.info("Loaded {} Custom Commands for Guild \"{}\": {}", commands.size(), guild.getName(),
						commands.stream().map(CustomCommand::getName).collect(Collectors.joining(", ")));
			}, err -> log.error("Could not retrieve Commands in guild \"{}\"", guild.getName()));
		}
	}

	/**
	 * Gets all {@link CustomCommand}s (from the database) for the specified {@link Guild}.
	 *
	 * @param guild The {@link Guild}.
	 * @return An unmodifiable {@link List} of all {@link CustomCommand}s for the current guild.
	 * @throws SQLException If an error occurs.
	 */
	public List<CustomCommand> getCustomCommands(@NotNull Guild guild) throws SQLException {
		try (Connection con = dataSource.getConnection()) {
			CustomCommandRepository repo = new CustomCommandRepository(con);
			return repo.getCustomCommandsByGuildId(guild.getIdLong());
		}
	}

	/**
	 * Gets all loaded {@link CustomCommand}s for the specified {@link Guild}.
	 *
	 * @param guildId The {@link Guild}s id.
	 * @return An unmodifiable {@link List} of all {@link CustomCommand}s for the current guild.
	 */
	public List<CustomCommand> getLoadedCommands(@NotNull Long guildId) {
		Map<String, CustomCommand> commands = new HashMap<>(loadedCommands);
		return commands.values()
				.stream()
				.filter(c -> Objects.equals(c.getGuildId(), guildId))
				.toList();
	}

	/**
	 * Creates a new {@link CustomCommand} for the specified guild.
	 *
	 * @param guild   The {@link Guild}.
	 * @param command The {@link CustomCommand} to create.
	 * @return Whether the command was successfully created/added.
	 * @throws SQLException If an error occurs.
	 */
	public boolean addCommand(@NotNull Guild guild, @NotNull CustomCommand command) throws SQLException {
		if (doesSlashCommandExist(existingCommands.get(guild.getIdLong()), command.getName())) {
			System.out.println("exists");
			return false;
		}
		try (Connection con = dataSource.getConnection()) {
			CustomCommandRepository repo = new CustomCommandRepository(con);
			guild.upsertCommand(command.toSlashCommandData()).queue();
			log.info("Created Custom Command in guild \"{}\": /{}", guild.getName(), command.getName());
			loadedCommands.put(command.getName(), command);
			return repo.insert(command) != null;
		}
	}

	/**
	 * Removes a single {@link CustomCommand} from the specified guild.
	 *
	 * @param guild   The {@link Guild}.
	 * @param command The {@link CustomCommand} to delete.
	 * @return Whether the command was successfully deleted.
	 * @throws SQLException If an error occurs.
	 */
	public boolean removeCommand(@NotNull Guild guild, @NotNull CustomCommand command) throws SQLException {
		if (!doesSlashCommandExist(existingCommands.get(guild.getIdLong()), command.getName())) {
			return false;
		}
		try (Connection con = dataSource.getConnection()) {
			CustomCommandRepository repo = new CustomCommandRepository(con);
			repo.delete(command);
			loadedCommands.remove(command.getName());
			for (Command c : existingCommands.get(guild.getIdLong())) {
				if (c.getName().equals(command.getName())) {
					guild.deleteCommandById(c.getId()).queue();
					log.info("Deleted Custom Command in guild \"{}\": /{}", guild.getName(), command.getName());
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Edits and updates a single {@link CustomCommand}.
	 *
	 * @param guild  The {@link Guild}.
	 * @param old    The "old" {@link CustomCommand}.
	 * @param update The "new" and updated {@link CustomCommand} object.
	 * @return Whether the command was successfully edited.
	 * @throws SQLException If an error occurs.
	 */
	public boolean editCommand(@NotNull Guild guild, @NotNull CustomCommand old, @NotNull CustomCommand update) throws SQLException {
		if (!doesSlashCommandExist(existingCommands.get(guild.getIdLong()), old.getName())) {
			return false;
		}
		try (Connection con = dataSource.getConnection()) {
			CustomCommandRepository repo = new CustomCommandRepository(con);
			repo.edit(old, update);
			loadedCommands.put(old.getName(), update);
			guild.upsertCommand(update.toSlashCommandData()).queue();
			log.info("Edited Custom Command in guild \"{}\": {} -> {}", guild.getName(), old, update);
			return true;
		}
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		if (loadedCommands.containsKey(cleanString(event.getName()))) {
			CustomCommand command = loadedCommands.get(cleanString(event.getName()));
			handleCustomCommand(event, command).queue();
		}
	}

	@Contract(pure = true)
	private @NotNull RestAction<?> handleCustomCommand(@NotNull SlashCommandInteractionEvent event, @NotNull CustomCommand command) {
		Set<RestAction<?>> actions = new HashSet<>();
		if (command.isEmbed()) {
			MessageEmbed embed = command.toEmbed();
			if (command.isReply()) {
				actions.add(event.replyEmbeds(embed));
			} else {
				actions.add(event.getChannel().sendMessageEmbeds(embed));
			}
		} else {
			if (command.isReply()) {
				actions.add(event.reply(command.getResponse()).allowedMentions(List.of()));
			} else {
				actions.add(event.getChannel().sendMessage(command.getResponse()).allowedMentions(List.of()));
			}
		}
		if (!command.isReply()) {
			actions.add(event.reply("Done!").setEphemeral(true));
		}
		return RestAction.allOf(actions);
	}
}
