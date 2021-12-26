package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.h2db.DbActions;

/**
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class ThanksCommandHandler implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) throws ResponseException {
		var userOption = event.getOption("user");
		User user = userOption == null ? event.getUser() : userOption.getAsUser();
		long totalThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		var embed = new EmbedBuilder()
				.setTitle("Thank you, " + user.getAsTag())
				.setThumbnail(user.getAvatarUrl())
				.setDescription("Here are some statistics about how you've helped others here.")
				.addField("Total Times Thanked", "**" + totalThanks + "**", false)
				.addField("Times Thanked This Week", "**" + weekThanks + "**", false);
		return event.replyEmbeds(embed.build());
	}
}
