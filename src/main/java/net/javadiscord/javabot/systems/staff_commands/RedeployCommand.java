package net.javadiscord.javabot.systems.staff_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCache;
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
	private final MessageCache messageCache;
	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param messageCache A service managing recent messages
	 * @param botConfig The main configuration of the bot
	 */
	public RedeployCommand(MessageCache messageCache, BotConfig botConfig) {
		this.messageCache = messageCache;
		this.botConfig=botConfig;
		setCommandData(Commands.slash("redeploy", "(ADMIN-ONLY) Makes the bot redeploy.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		setRequiredUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!Checks.hasAdminRole(botConfig, event.getMember())) {
			Responses.replyAdminOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		log.warn("Redeploying... Requested by: " + event.getUser().getAsTag());
		event.reply("**Redeploying...** This may take some time.").queue();
		messageCache.synchronize();
		System.exit(0);
	}
}