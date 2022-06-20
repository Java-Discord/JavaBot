package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

/**
 * Command that displays the bot's uptime.
 */
public class UptimeCommand extends SlashCommand {
	public UptimeCommand() {
		setSlashCommandData(Commands.slash("uptime", "Shows the bot's current uptime.")
				.setGuildOnly(true)
		);
	}


	/**
	 * Calculates the Uptimes and returns a formatted String.
	 *
	 * @return The current Uptime as a String.
	 */
	public static String getUptime() {
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
	public void execute(SlashCommandInteractionEvent event) {
		event.replyEmbeds(new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setAuthor(getUptime(), null, event.getJDA().getSelfUser().getAvatarUrl()).build()
		).queue();
	}
}