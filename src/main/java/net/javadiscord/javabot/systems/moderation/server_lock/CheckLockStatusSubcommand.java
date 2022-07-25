package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ServerLockConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /serverlock-admin check-status command.</h3>
 */
public class CheckLockStatusSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public CheckLockStatusSubcommand() {
		setSubcommandData(new SubcommandData("check-status", "Command for checking the current server lock status."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasStaffRole(event.getGuild(), event.getMember())) {
			Responses.replyStaffOnly(event, event.getGuild()).queue();
			return;
		}
		ServerLockConfig config = Bot.config.get(event.getGuild()).getServerLockConfig();
		Responses.info(event, "Server Lock Status", "The Server Lock is currently **%s**", config.isLocked() ? "ENABLED" : "DISABLED").queue();
	}
}
