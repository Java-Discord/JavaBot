package net.javadiscord.javabot.systems.staff_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /redeploy command.</h3>
 * Command that lets staff-members redeploy the bot.
 * <p>
 * This only works if the way the bot is hosted is set up correctly, for example with a bash script that handles
 * compilation and a service set up with that bash script running before the bot gets started.
 * <p>
 * I have explained how we do it in https://github.com/Java-Discord/JavaBot/pull/195
 */
@Slf4j
public class RedeployCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public RedeployCommand() {
		setSlashCommandData(Commands.slash("redeploy", "(ADMIN-ONLY) Makes the bot redeploy.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		requireUsers(Bot.getConfig().getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!Checks.hasAdminRole(event.getGuild(), event.getMember())) {
			Responses.replyAdminOnly(event, event.getGuild()).queue();
			return;
		}
		log.warn("Redeploying... Requested by: " + event.getUser().getAsTag());
		event.reply("**Redeploying...** This may take some time.").queue();
		Bot.getMessageCache().synchronize();
		System.exit(0);
	}
}