package net.javadiscord.javabot.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.command.data.CommandConfig;
import net.javadiscord.javabot.command.data.CommandDataLoader;
import net.javadiscord.javabot.events.StartupListener;
import net.javadiscord.javabot.util.Misc;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

/**
 * This listener is responsible for handling slash commands sent by users in
 * guilds where the bot is active, and responding to them by calling the
 * appropriate {@link SlashCommandHandler}.
 * <p>
 *     The list of valid commands, and their associated handlers, are defined in
 *     their corresponding YAML-file under the resources/commands directory.
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
	 *     It does this by attempting to add an entry to {@link SlashCommands#commandsIndex}
	 *     whose key is the command name, and whose value is a new instance of
	 *     the handler class which the command has specified.
	 * </p>
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
		var customCommandNames=this.updateCustomCommands(commandUpdateAction, guild);

		commandUpdateAction.queue(commands ->{
			// Add privileges to the non-custom commands, after the commands have been registered.
			commands.removeIf(cmd->customCommandNames.contains(cmd.getName()));
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

	private Set<String> updateCustomCommands(CommandListUpdateAction commandUpdateAction, Guild guild) {
		log.info("{}[{}]{} Registering custom commands",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
		MongoDatabase database = StartupListener.mongoClient.getDatabase("other");
		MongoCollection<Document> collection = database.getCollection("customcommands");

		Set<String> customCommandNames = new HashSet<>();
		for (Document document : collection.find(eq("guildId", guild.getId()))) {
			JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
			String commandName = root.get("commandName").getAsString();
			String value = root.get("value").getAsString();
			if (value.length() > 100) value = value.substring(0, 97) + "...";
			commandUpdateAction.addCommands(
					new CommandData(commandName, value)
							.addOption(OptionType.BOOLEAN, "reply", "If set to True, will reply on use", false)
							.addOption(OptionType.BOOLEAN, "embed", "If set to True, will send the content in an embed", false));
			customCommandNames.add(commandName);
		}
		return customCommandNames;
	}

	private void addCommandPrivileges(List<Command> commands, CommandConfig[] commandConfigs, Guild guild) {
		log.info("{}[{}]{} Adding command privileges",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

		Map<String, Collection<? extends CommandPrivilege>> map = new HashMap<>();

		for(Command command : commands) {
			List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), commandConfigs));
			if(!privileges.isEmpty()) {
				map.put(command.getId(), privileges);
			}
		}

		guild.updateCommandPrivileges(map)
				.queue(success -> log.info("Commands updated successfully"), error -> log.info("Commands update failed"));
	}

	@NotNull
	private List<CommandPrivilege> getCommandPrivileges(Guild guild, CommandConfig config) {
		if(config == null || config.getPrivileges() == null) return Collections.emptyList();
		List<CommandPrivilege> privileges = new ArrayList<>();
		for (var privilegeConfig : config.getPrivileges()) {
			try {
				privileges.add(privilegeConfig.toData(guild, Bot.config));
				log.info("\t{}[{}]{} Registering privilege: {}",
						Constants.TEXT_WHITE, config.getName(), Constants.TEXT_RESET, Objects.toString(privilegeConfig));
			} catch (Exception e) {
				log.warn("Could not register privileges for command {}: {}", config.getName(), e.getMessage());
			}
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

	private RestAction<?> handleCustomCommand(SlashCommandEvent event) {
		String json = StartupListener.mongoClient
				.getDatabase("other")
				.getCollection("customcommands")
				.find(
						new BasicDBObject()
								.append("guildId", event.getGuild().getId())
								.append("commandName", event.getName()))
				.first()
				.toJson();

		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		String value = Misc.replaceTextVariables(event.getGuild(), root.get("value").getAsString());
		boolean embed = root.get("embed").getAsBoolean();
		boolean reply = root.get("reply").getAsBoolean();

		OptionMapping replyOption = event.getOption("reply");
		if (replyOption != null) reply = replyOption.getAsBoolean();

		OptionMapping embedOption = event.getOption("embed");
		if (embedOption != null) embed = embedOption.getAsBoolean();

		if (embed) {
			var e = new EmbedBuilder()
					.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
					.setDescription(value)
					.build();

			if (reply) return event.replyEmbeds(e);
			else {
				event.reply("Done!").setEphemeral(true).queue();
				return event.getChannel().sendMessageEmbeds(e);
			}
		} else {
			if (reply) return event.reply(value);
			else {
				event.reply("Done!").setEphemeral(true).queue();
				return event.getChannel().sendMessage(value);
			}
		}
	}
}
