package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import java.util.concurrent.ExecutorService;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;
import net.javadiscord.javabot.systems.staff_commands.tags.CustomTagManager;
import net.javadiscord.javabot.systems.staff_commands.tags.dao.CustomTagRepository;

/**
 * Represents the `/tag-admin` command. This holds administrative commands for managing "Custom Tags".
 */
public class TagsAdminCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param tagManager The {@link CustomTagManager}
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param customTagRepository Dao object that represents the CUSTOM_COMMANDS SQL Table.
	 */
	public TagsAdminCommand(CustomTagManager tagManager, BotConfig botConfig, DbHelper dbHelper, ExecutorService asyncPool, CustomTagRepository customTagRepository) {
		setModerationSlashCommandData(Commands.slash("tag-admin", "Administrative commands for managing \"Custom Tags\"."));
		addSubcommands(new CreateCustomTagSubcommand(tagManager, botConfig), new DeleteCustomTagSubcommand(tagManager, botConfig, asyncPool, customTagRepository), new EditCustomTagSubcommand(tagManager, botConfig, customTagRepository, asyncPool));
	}
}