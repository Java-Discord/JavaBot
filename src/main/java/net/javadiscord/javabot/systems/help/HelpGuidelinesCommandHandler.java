package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.util.StringResourceCache;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesCommandHandler implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		StringBuilder sb = new StringBuilder();
		for (GuildChannel channel : Bot.config.get(event.getGuild()).getHelp().getOpenChannelCategory().getChannels()) {
			sb.append(channel.getAsMention() + "\n");
		}
		var embed = new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setDescription(StringResourceCache.load("/help-guidelines.txt"))
				.addField("Available Help Channels", sb.toString(), false)
				.setImage("https://cdn.discordapp.com/attachments/744899463591624815/895244046027608074/unknown.png");
		return event.replyEmbeds(embed.build());
	}
}
