package net.javadiscord.javabot.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.command.data.CommandConfig;
import net.javadiscord.javabot.command.data.CommandDataLoader;
import net.javadiscord.javabot.systems.staff.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.util.Misc;
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
 * appropriate {@link SlashCommandHandler}.
 * <p>
 * The list of valid commands, and their associated handlers, are defined in
 * their corresponding YAML-file under the resources/commands directory.
 * </p>
 */
public class SlashCommands extends ListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

	/**
	 * Maps every command name and alias to an instance of the command, for
	 * constant-time lookup.
	 */
	private final Map<String, SlashCommandHandler> commandsIndex;

	public SlashCommands() {
		this.commandsIndex = new HashMap<>();
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) return;

		var command = this.commandsIndex.get(event.getName());
		if (command != null) {
			try {
				command.handle(event).queue();
			} catch (ResponseException e) {
				handleResponseException(e, event);
			}
		} else {
			handleCustomCommand(event).queue();
		}
	}

	private void handleResponseException(ResponseException e, SlashCommandEvent event) {
		switch (e.getType()) {
			case WARNING -> Responses.warning(event, e.getMessage()).queue();
			case ERROR -> Responses.error(event, e.getMessage()).queue();
		}
		if (e.getCause() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.getCause().printStackTrace(pw);
			Bot.config.get(event.getGuild()).getModeration().getLogChannel().sendMessageFormat(
					"An exception occurred when %s issued the **%s** slash command in %s:\n```%s```\n",
					event.getUser().getAsMention(),
					event.getName(),
					event.getTextChannel().getAsMention(),
					sw.toString()
			).queue();
		}
	}

	/**
	 * Registers all slash commands defined in the set YAML-files for the given guild
	 * so that users can see the commands when they type a "/".
	 * <p>
	 * It does this by attempting to add an entry to {@link SlashCommands#commandsIndex}
	 * whose key is the command name, and whose value is a new instance of
	 * the handler class which the command has specified.
	 * </p>
	 *
	 * @param guild The guild to update commands for.
	 */
	public void registerSlashCommands(Guild guild) {
		CommandConfig[] commandConfigs = CommandDataLoader.load(
				"commands/economy.yaml",
				"commands/help.yaml",
				"commands/jam.yaml",
				"commands/qotw.yaml",
				"commands/staff.yaml",
				"commands/user.yaml"
		);
		var commandUpdateAction = this.updateCommands(commandConfigs, guild);
		var customCommandNames = this.updateCustomCommands(commandUpdateAction, guild);

		commandUpdateAction.queue(commands -> {
			// Add privileges to the non-custom commands, after the commands have been registered.
			commands.removeIf(cmd -> customCommandNames.contains(cmd.getName()));
			this.addCommandPrivileges(commands, commandConfigs, guild);
		});
	}


	private CommandListUpdateAction updateCommands(CommandConfig[] commandConfigs, Guild guild) {
		log.info("{}[{}]{} Registering slash commands",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
		if (commandConfigs.length > 100) throw new IllegalArgumentException("Cannot add more than 100 commands.");
		CommandListUpdateAction commandUpdateAction = guild.updateCommands();
		for (CommandConfig config : commandConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					this.commandsIndex.put(config.getName(), (SlashCommandHandler) handlerClass.getConstructor().newInstance());
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Command \"{}\" does not have an associated handler class. It will be ignored.", config.getName());
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
						new CommandData(c.getName(), response).addOptions(
								new OptionData(OptionType.BOOLEAN, "reply", "Should the custom commands reply?"),
								new OptionData(OptionType.BOOLEAN, "embed", "Should the response be embedded?")));
				commandNames.add(c.getName());
			}
			return commandNames;
		} catch (SQLException e) {
			e.printStackTrace();
			return Set.of();
		}
	}

	private void addCommandPrivileges(List<Command> commands, CommandConfig[] commandConfigs, Guild guild) {
		log.info("{}[{}]{} Adding command privileges",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

		Map<String, Collection<? extends CommandPrivilege>> map = new HashMap<>();
		for (Command command : commands) {
			List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), commandConfigs));
			if (!privileges.isEmpty()) {
				map.put(command.getId(), privileges);
			}
		}

		guild.updateCommandPrivileges(map)
				.queue(success -> log.info("Commands updated successfully"), error -> log.info("Commands update failed"));
	}

	@NotNull
	private List<CommandPrivilege> getCommandPrivileges(Guild guild, CommandConfig config) {
		if (config == null || config.getPrivileges() == null) return Collections.emptyList();
		List<CommandPrivilege> privileges = new ArrayList<>();
		for (var privilegeConfig : config.getPrivileges()) {
			privileges.add(privilegeConfig.toData(guild, Bot.config));
			log.info("\t{}[{}]{} Registering privilege: {}",
					Constants.TEXT_WHITE, config.getName(), Constants.TEXT_RESET, privilegeConfig);
		}
		return privileges;
	}

	private CommandConfig findCommandConfig(String name, CommandConfig[] configs) {
		for (CommandConfig config : configs) {
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
	 * @param event The {@link SlashCommandEvent} that is fired.
	 * @return The {@link RestAction}.
	 */
	private RestAction<?> handleCustomCommand(SlashCommandEvent event) {
		var name = event.getName();
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new CustomCommandRepository(con);
			var optional = repo.findByName(event.getGuild().getIdLong(), name);
			if (optional.isEmpty()) return null;
			var command = optional.get();
			var responseText = Misc.replaceTextVariables(event.getGuild(), command.getResponse());
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
}
