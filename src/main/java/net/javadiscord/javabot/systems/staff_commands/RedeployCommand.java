package net.javadiscord.javabot.systems.staff_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

/**
 * Command that lets staff-members redeploy the bot.
 * <p>
 * This only works if the way the bot is hosted is set up correctly, for example with a bash script that handles
 * compilation and a service set up with that bash script running before the bot gets started.
 * <p>
 * I have explained how we do it in https://github.com/Java-Discord/JavaBot/pull/195
 */
@Slf4j
public class RedeployCommand extends SlashCommand {
	public RedeployCommand() {
		setSlashCommandData(Commands.slash("redeploy", "(ADMIN-ONLY) Makes the bot redeploy.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		requireUsers(Bot.config.getSystems().getAdminUsers());
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		log.warn("Redeploying... Requested by: " + event.getUser().getAsTag());
		event.reply("Redeploying... this can take up to 2 Minutes.").queue();
		Bot.messageCache.synchronize();
		System.exit(0);
	}
}