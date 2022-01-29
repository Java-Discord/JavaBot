package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.StringResourceCache;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesCommandHandler implements SlashCommandHandler {
	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) throws ResponseException {
		var embed = new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setDescription(StringResourceCache.load("/help-guidelines.txt"))
				.setImage("https://cdn.discordapp.com/attachments/744899463591624815/895244046027608074/unknown.png");
		return event.replyEmbeds(embed.build());
	}
}
