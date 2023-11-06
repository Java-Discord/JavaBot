package net.discordjug.javabot.systems.moderation.server_lock;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.ServerLockConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /serverlock-admin check-status command.</h3>
 */
public class CheckLockStatusSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public CheckLockStatusSubcommand(BotConfig botConfig) {
		this.botConfig = botConfig;
		setCommandData(new SubcommandData("check-status", "Command for checking the current server lock status."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		ServerLockConfig config = botConfig.get(event.getGuild()).getServerLockConfig();
		Responses.info(event, "Server Lock Status", "The Server Lock is currently **%s**", config.isLocked() ? "ENABLED" : "DISABLED").queue();
	}
}
