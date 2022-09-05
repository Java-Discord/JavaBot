package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.RegistrationType;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCache;

import java.util.Map;
import java.util.Set;

/**
 * Represents the `/db-admin` command. This holds administrative commands for managing the bot's database.
 */
public class DbAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s & {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param messageCache A service managing recent messages
	 */
	public DbAdminCommand(MessageCache messageCache) {
		setRegistrationType(RegistrationType.GUILD);
		setSlashCommandData(Commands.slash("db-admin", "(ADMIN ONLY) Administrative Commands for managing the bot's database.")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
				.setGuildOnly(true)
		);
		addSubcommands(new ExportSchemaSubcommand(), new ExportTableSubcommand(), new MigrationsListSubcommand(), new MigrateSubcommand(), new QuickMigrateSubcommand());
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("message-cache", "Administrative tools for managing the Message Cache."), Set.of(new MessageCacheInfoSubcommand(messageCache))
		));
		requireUsers(Bot.getConfig().getSystems().getAdminConfig().getAdminUsers());
	}
}
