package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Command that displays the bot's uptime.
 */
public class UptimeCommand extends SlashCommand {
	public UptimeCommand() {
		setSlashCommandData(Commands.slash("uptime", "Shows the bot's current uptime.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setAuthor(StringUtils.formatUptime(), null, event.getJDA().getSelfUser().getAvatarUrl()).build()
		).queue();
	}
}