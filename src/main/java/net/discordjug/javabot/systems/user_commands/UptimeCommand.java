package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /uptime command.</h3>
 */
public class UptimeCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public UptimeCommand() {
		setCommandData(Commands.slash("uptime", "Shows the bot's current uptime.")
				.setContexts(InteractionContextType.GUILD)
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