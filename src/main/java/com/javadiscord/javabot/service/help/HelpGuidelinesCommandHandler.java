package com.javadiscord.javabot.service.help;

import com.javadiscord.javabot.commands.ResponseException;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.utils.StringResourceCache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesCommandHandler implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) throws ResponseException {
		var embed = new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setDescription(StringResourceCache.load("/help-guidelines.txt"))
				.setImage("https://cdn.discordapp.com/attachments/744899463591624815/895244046027608074/unknown.png");
		return event.replyEmbeds(embed.build());
	}
}
