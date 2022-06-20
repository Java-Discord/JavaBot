package net.javadiscord.javabot.systems.qotw.commands.qotw_points;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Subcommand that allows staff-members to edit the QOTW-Point amount of any user.
 */
public class SetPointsSubcommand extends SlashCommand.Subcommand {
	public SetPointsSubcommand() {
		setSubcommandData(new SubcommandData("set", "Allows to modify the QOTW-Points of a single user.")
				.addOption(OptionType.USER, "user", "The user whose points should be changed.", true)
				.addOption(OptionType.INTEGER, "points", "The amount of points.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping memberMapping = event.getOption("user");
		OptionMapping pointsMapping = event.getOption("points");
		if (memberMapping == null || pointsMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		Member member = memberMapping.getAsMember();
		long points = pointsMapping.getAsLong();
		try (Connection con = Bot.dataSource.getConnection()) {
			QOTWPointsService service = new QOTWPointsService(Bot.dataSource);
			QOTWAccount account = service.getOrCreateAccount(member.getIdLong());
			account.setPoints(points);
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			repo.update(account);
			Responses.success(event, "Set QOTW-Points",
					String.format("Successfully changed the points of %s to %s", member.getUser().getAsMention(), points)).queue();
		} catch (SQLException e) {
			Sentry.captureException(e);
			Responses.error(event, "An Error occurred. Please try again.").queue();
		}

	}
}


