package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.StringResourceCache;

import java.util.stream.Collectors;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesCommand extends SlashCommand {
	public HelpGuidelinesCommand() {
		setSlashCommandData(Commands.slash("help-guidelines", "Show the server's help guidelines in a simple format."));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String channels = Bot.config.get(event.getGuild()).getHelp().getOpenChannelCategory().getChannels()
				.stream()
				.map(GuildChannel::getAsMention)
				.collect(Collectors.joining("\n"));
		event.replyEmbeds(new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setDescription(StringResourceCache.load("/help-guidelines.txt"))
				.addField("Available Help Channels", channels, false)
				.setImage("https://cdn.discordapp.com/attachments/744899463591624815/895244046027608074/unknown.png")
				.build()
		).queue();
	}
}
