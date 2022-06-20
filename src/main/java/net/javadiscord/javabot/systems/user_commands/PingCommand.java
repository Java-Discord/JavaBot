package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;

/**
 * Command that displays the current Gateway ping.
 */
public class PingCommand extends SlashCommand {
	public PingCommand() {
		setSlashCommandData(Commands.slash("ping", "Shows the bot's gateway ping.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.replyEmbeds(new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.build()
		).queue();
	}
}