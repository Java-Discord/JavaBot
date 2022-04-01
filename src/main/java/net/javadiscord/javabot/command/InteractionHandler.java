package net.javadiscord.javabot.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.command.data.CommandDataLoader;
import net.javadiscord.javabot.command.data.context_commands.ContextCommandConfig;
import net.javadiscord.javabot.command.data.slash_commands.SlashCommandConfig;
import net.javadiscord.javabot.command.data.slash_commands.SlashOptionConfig;
import net.javadiscord.javabot.command.data.slash_commands.SlashSubCommandConfig;
import net.javadiscord.javabot.command.data.slash_commands.SlashSubCommandGroupConfig;
import net.javadiscord.javabot.command.interfaces.Autocompletable;
import net.javadiscord.javabot.command.interfaces.MessageContextCommand;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.command.interfaces.UserContextCommand;
import net.javadiscord.javabot.systems.staff.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.systems.staff.custom_commands.model.CustomCommand;
import net.javadiscord.javabot.util.GuildUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

/**
 * This listener is responsible for handling slash commands sent by users in
 * guilds where the bot is active, and responding to them by calling the
 * appropriate {@link SlashCommand}.
 * <p>
 * The list of valid commands, and their associated handlers, are defined in
 * their corresponding YAML-file under the resources/commands directory.
 * </p>
 */
public class InteractionHandler extends ListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(InteractionHandler.class);

	/**
	 * Maps every command name and alias to an instance of the command, for
	 * constant-time lookup.
	 */
	private final Map<String, SlashCommand> slashCommandIndex;

	private final Map<String, UserContextCommand> userContextCommandIndex;
	private final Map<String, MessageContextCommand> messageContextCommandIndex;
	private final Map<SlashCommand, Autocompletable> autocompleteIndex;

	private SlashCommandConfig[] slashCommandConfigs;
	private ContextCommandConfig[] contextCommandConfigs;
	
	/**
	 * Constructor of this class.
	 */
	public InteractionHandler() {
		this.slashCommandIndex = new HashMap<>();
		this.userContextCommandIndex = new HashMap<>();
		this.messageContextCommandIndex = new HashMap<>();
		this.autocompleteIndex = new HashMap<>();
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) return;
		SlashCommand command = this.slashCommandIndex.get(event.getName());
		try {
			if (command != null) {
				command.handleSlashCommandInteraction(event).queue();
			} else {
				this.handleCustomCommand(event).queue();
			}
		} catch (ResponseException e) {
			this.handleResponseException(e, event);
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
		if (event.getGuild() == null) return;
		SlashCommand command = this.slashCommandIndex.get(event.getName());
		if (command == null) return;
		Autocompletable autocomplete = this.autocompleteIndex.get(command);
		autocomplete.handleAutocomplete(event).queue();
	}

	/**
	 * Checks whether a slash command exists.
	 * @param name the name of the command
	 * @param guild the {@link Guild} the command may exist in
	 * @return <code>true</code> if the command exists, else <code>false</code>
	 */
	public boolean doesSlashCommandExist(String name, Guild guild){
		try {
			return this.slashCommandIndex.containsKey(name) || this.getCustomCommand(name, guild).isPresent();
		} catch(SQLException e) {
			return false;
		}
	}

	@Override
	public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
		if (event.getGuild() == null) return;
		var command = this.userContextCommandIndex.get(event.getName());
		if (command != null) {
			try {
				command.handleUserContextCommandInteraction(event).queue();
			} catch (ResponseException e) {
				this.handleResponseException(e, event.getInteraction());
			}
		}
	}

	@Override
	public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
		if (event.getGuild() == null) return;
		var command = this.messageContextCommandIndex.get(event.getName());
		if (command != null) {
			try {
				command.handleMessageContextCommandInteraction(event).queue();
			} catch (ResponseException e) {
				this.handleResponseException(e, event.getInteraction());
			}
		}
	}

	private void handleResponseException(ResponseException e, CommandInteraction interaction) {
		switch (e.getType()) {
			case WARNING -> Responses.warning(interaction, e.getMessage()).queue();
			case ERROR -> Responses.error(interaction, e.getMessage()).queue();
		}
		if (e.getCause() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.getCause().printStackTrace(pw);
			GuildUtils.getLogChannel(interaction.getGuild()).sendMessageFormat(
					"An exception occurred when %s issued the **%s** command in %s:\n```%s```\n",
					interaction.getUser().getAsMention(),
					interaction.getName(),
					interaction.getTextChannel().getAsMention(),
					sw.toString()
			).queue();
		}
	}

	/**
	 * Registers all slash commands defined in the set YAML-files for the given guild
	 * so that users can see the commands when they type a "/".
	 * <p>
	 * It does this by attempting to add an entry to {@link InteractionHandler#slashCommandIndex}
	 * whose key is the command name, and whose value is a new instance of
	 * the handler class which the command has specified.
	 * </p>
	 *
	 * @param guild The guild to update commands for.
	 */
	public void registerCommands(Guild guild) {
		this.slashCommandConfigs = CommandDataLoader.loadSlashCommandConfig(
				"commands/slash/help.yaml",
				"commands/slash/jam.yaml",
				"commands/slash/qotw.yaml",
				"commands/slash/staff.yaml",
				"commands/slash/user.yaml"
		);
		this.contextCommandConfigs = CommandDataLoader.loadContextCommandConfig(
				"commands/context/message.yaml",
				"commands/context/user.yaml"
		);
		CommandListUpdateAction commandUpdateAction = this.updateCommands(guild);
		Set<String> customCommandNames = this.updateCustomCommands(commandUpdateAction, guild);
		commandUpdateAction.queue(commands -> {
			// Add privileges to the non-custom commands, after the commands have been registered.
			commands.removeIf(cmd -> customCommandNames.contains(cmd.getName()));
			commands.removeIf(cmd -> cmd.getType() != Command.Type.SLASH);
			this.addCommandPrivileges(commands, guild);
		});
	}
	
	private CommandListUpdateAction updateCommands(Guild guild) {
		log.info("{}[{}]{} Registering commands", Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
		if (this.slashCommandConfigs.length > Commands.MAX_SLASH_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s commands.", Commands.MAX_SLASH_COMMANDS));
		}
		if (Arrays.stream(this.contextCommandConfigs).filter(p -> p.getEnumType() == Command.Type.USER).count() > Commands.MAX_USER_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s User Context Commands", Commands.MAX_USER_COMMANDS));
		}
		if (Arrays.stream(this.contextCommandConfigs).filter(p -> p.getEnumType() == Command.Type.MESSAGE).count() > Commands.MAX_MESSAGE_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s Message Context Commands", Commands.MAX_MESSAGE_COMMANDS));
		}
		CommandListUpdateAction commandUpdateAction = guild.updateCommands();
		for (var config : slashCommandConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					Object instance = handlerClass.getConstructor().newInstance();
					this.slashCommandIndex.put(config.getName(), (SlashCommand) instance);
					if (this.hasAutocomplete(config)) {
						this.autocompleteIndex.put((SlashCommand) instance, (Autocompletable) instance);
					}
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Slash Command \"{}\" does not have an associated handler class. It will be ignored.", config.getName());
			}
			commandUpdateAction.addCommands(config.toData());
		}
		for (var config : this.contextCommandConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					if (config.getEnumType() == Command.Type.USER) {
						this.userContextCommandIndex.put(config.getName(), (UserContextCommand) handlerClass.getConstructor().newInstance());
					} else if (config.getEnumType() == Command.Type.MESSAGE) {
						this.messageContextCommandIndex.put(config.getName(), (MessageContextCommand) handlerClass.getConstructor().newInstance());
					} else {
						log.warn("Unknown Context Command Type.");
					}
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Context Command ({}) \"{}\" does not have an associated handler class. It will be ignored.", config.getEnumType(), config.getName());
			}
			commandUpdateAction.addCommands(config.toData());
		}
		return commandUpdateAction;
	}

	/**
	 * Attempts to update and register all Custom Commands.
	 *
	 * @param commandUpdateAction The {@link CommandListUpdateAction}.
	 * @param guild               The current guild.
	 * @return A {@link Set} with all Custom Command names.
	 */
	private Set<String> updateCustomCommands(CommandListUpdateAction commandUpdateAction, Guild guild) {
		log.info("{}[{}]{} Registering custom commands", Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new CustomCommandRepository(con);
			var commands = repo.getCustomCommandsByGuildId(guild.getIdLong());
			Set<String> commandNames = new HashSet<>();
			for (var c : commands) {
				var response = c.getResponse();
				if (response.length() > 100) response = response.substring(0, 97).concat("...");
				commandUpdateAction.addCommands(
						Commands.slash(c.getName(), response)
								.addOption(OptionType.BOOLEAN, "reply", "Should the custom commands reply?")
								.addOption(OptionType.BOOLEAN, "embed", "Should the response be embedded?"));
				commandNames.add(c.getName());
			}
			return commandNames;
		} catch (SQLException e) {
			e.printStackTrace();
			return Set.of();
		}
	}

	private void addCommandPrivileges(List<Command> commands, Guild guild) {
		log.info("{}[{}]{} Adding command privileges",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

		Map<String, List<CommandPrivilege>> map = new HashMap<>();
		for (Command command : commands) {
			List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), slashCommandConfigs));
			if (!privileges.isEmpty()) {
				map.put(command.getId(), privileges);
			}
		}

		guild.updateCommandPrivileges(map)
				.queue(success -> log.info("Commands updated successfully"), error -> log.info("Commands update failed"));
	}

	@NotNull
	private List<CommandPrivilege> getCommandPrivileges(Guild guild, SlashCommandConfig config) {
		if (config == null || config.getPrivileges() == null) return Collections.emptyList();
		List<CommandPrivilege> privileges = new ArrayList<>();
		for (var privilegeConfig : config.getPrivileges()) {
			privileges.add(privilegeConfig.toData(guild, Bot.config));
			log.info("\t{}[{}]{} Registering privilege: {}",
					Constants.TEXT_WHITE, config.getName(), Constants.TEXT_RESET, privilegeConfig);
		}
		return privileges;
	}

	private SlashCommandConfig findCommandConfig(String name, SlashCommandConfig[] configs) {
		for (SlashCommandConfig config : configs) {
			if (name.equals(config.getName())) {
				return config;
			}
		}
		log.warn("Could not find CommandConfig for command: {}", name);
		return null;
	}

	/**
	 * Handles a Custom Slash Command.
	 *
	 * @param event The {@link SlashCommandInteractionEvent} that is fired.
	 * @return The {@link RestAction}.
	 */
	private RestAction<?> handleCustomCommand(SlashCommandInteractionEvent event) {
		var name = event.getName();
		try {
			var optional = getCustomCommand(name, event.getGuild());
			if (optional.isEmpty()) return Responses.error(event,"Unknown Command.");
			var command = optional.get();
			var responseText = GuildUtils.replaceTextVariables(event.getGuild(), command.getResponse());
			var replyOption = event.getOption("reply");
			boolean reply = replyOption == null ? command.isReply() : replyOption.getAsBoolean();
			var embedOption = event.getOption("embed");
			boolean embed = embedOption == null ? command.isEmbed() : embedOption.getAsBoolean();
			if (embed) {
				var e = new EmbedBuilder()
						.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
						.setDescription(responseText)
						.build();
				if (reply) {
					return event.replyEmbeds(e);
				} else {
					return RestAction.allOf(event.getChannel().sendMessageEmbeds(e), event.reply("Done!").setEphemeral(true));
				}
			} else {
				if (reply) {
					return event.reply(responseText);
				} else {
					return RestAction.allOf(event.getChannel().sendMessage(responseText), event.reply("Done!").setEphemeral(true));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "Unknown Command.");
		}
	}

	private Optional<CustomCommand> getCustomCommand(String name, Guild guild) throws SQLException {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new CustomCommandRepository(con);
			return repo.findByName(guild.getIdLong(), name);
		}
	}

	private boolean hasAutocomplete(SlashCommandConfig config) {
		return config.getOptions() != null && Arrays.stream(config.getOptions()).anyMatch(SlashOptionConfig::isAutocomplete) ||
				config.getSubCommandGroups() != null && Arrays.stream(config.getSubCommandGroups()).anyMatch(this::hasAutocomplete) ||
				config.getSubCommands() != null && Arrays.stream(config.getSubCommands()).anyMatch(this::hasAutocomplete);
	}

	private boolean hasAutocomplete(SlashSubCommandGroupConfig config) {
		return config.getSubCommands() != null && Arrays.stream(config.getSubCommands()).anyMatch(this::hasAutocomplete);
	}

	private boolean hasAutocomplete(SlashSubCommandConfig config) {
		return config.getOptions() != null && Arrays.stream(config.getOptions()).anyMatch(SlashOptionConfig::isAutocomplete);
	}
}
