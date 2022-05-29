package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Subcommand that allows jam-admin to list all jam submissions.
 */
public class ListSubmissionsSubcommand extends ActiveJamSubcommand {
	public ListSubmissionsSubcommand() {
		setSubcommandData(new SubcommandData("list-submissions", "Shows a list of all submissions that have been made so far.")
				.addOption(OptionType.INTEGER, "page", "The page of results to show. By default it starts at 1.", false)
				.addOption(OptionType.USER, "user", "The user to get submissions for.", false));
	}

	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
		int page = event.getOption("page", 1, OptionMapping::getAsInt);
		OptionMapping userOption = event.getOption("user");
		Long userId = null;
		if (userOption != null) {
			userId = userOption.getAsUser().getIdLong();
		}
		List<JamSubmission> submissions = new JamSubmissionRepository(con).getSubmissions(activeJam, page, userId);

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Submissions")
				.setColor(config.getJamEmbedColor());
		for (JamSubmission sub : submissions) {
			User user = event.getJDA().getUserById(sub.getUserId());
			String userName = user == null ? "Unknown user" : user.getAsTag();
			String timestamp = sub.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm:ss 'UTC'"));
			embedBuilder.addField(
					String.format("`%d` %s at %s", sub.getId(), userName, timestamp),
					"Link: *" + sub.getSourceLink() + "*\n> " + sub.getDescription(),
					false
			);
		}
		embedBuilder.setFooter("Page " + page + ", up to 10 items per page");
		return event.replyEmbeds(embedBuilder.build()).setEphemeral(true);
	}
}
