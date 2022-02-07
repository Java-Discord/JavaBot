package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

/**
 * Command that displays the bot's uptime.
 */
public class UptimeCommand implements ISlashCommand {

	/**
	 * Calculates the Uptimes and returns a formatted String.
	 *
	 * @return The current Uptime as a String.
	 */
	public String getUptime() {
		RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		long uptimeMS = rb.getUptime();

		long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
		uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
		long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
		uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
		long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
		uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
		long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

		return String.format("%sd %sh %smin %ss",
				uptimeDAYS, uptimeHRS, uptimeMIN, uptimeSEC);
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String botImage = event.getJDA().getSelfUser().getAvatarUrl();
		var e = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setAuthor(getUptime(), null, botImage);

		return event.replyEmbeds(e.build());
	}
}