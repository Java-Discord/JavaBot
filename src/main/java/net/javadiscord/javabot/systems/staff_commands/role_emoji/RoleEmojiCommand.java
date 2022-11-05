package net.javadiscord.javabot.systems.staff_commands.role_emoji;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.javadiscord.javabot.data.config.BotConfig;

/**
 * This class represents the /emoji-admin command.
 * This command allows managing emojis which are usable only be members with certain roles.
 */
public class RoleEmojiCommand extends SlashCommand {

	/**
	 * The constructor of this class, which sets the corresponding {@link SlashCommandData}s.
	 * @param botConfig The main configuration of the bot
	 * @param addRoleEmojiSubcommand A subcommand allowing to add role-exclusive emojis
	 */
	public RoleEmojiCommand(BotConfig botConfig, AddRoleEmojiSubcommand addRoleEmojiSubcommand) {
		SlashCommandData slashCommandData = Commands.slash("emoji-admin", "Administrative command for managing server emojis")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
				.setGuildOnly(true);
		setSlashCommandData(slashCommandData);
		addSubcommands(addRoleEmojiSubcommand);
		requireUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}
}
