package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;

import java.sql.SQLException;

/**
 * Subcommand that allows staff-members to edit the QOTW-Point amount of any user.
 */
public class SetSubCommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var memberOption = event.getOption("user");
		var pointsOption = event.getOption("points");
		if (memberOption == null || pointsOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var member = memberOption.getAsMember();
		var memberId = member.getIdLong();
		var points = pointsOption.getAsLong();
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QuestionPointsRepository(con);
			repo.update(memberId, points);
			return Responses.success(event,
					"Set QOTW-Points",
					String.format("Successfully changed the points of %s to %s", member.getUser().getAsMention(), points));
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An Error occurred.");
		}

	}
}


