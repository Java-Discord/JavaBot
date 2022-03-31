package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that allows jam-admin to list all jam submissions.
 */
public class ListSubmissionsSubcommand extends ActiveJamSubcommand {
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

	/**
	 * Replies with all jam submissions.
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return The {@link AutoCompleteCallbackAction}.
	 */
	public static AutoCompleteCallbackAction replySubmissions(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			JamRepository jamRepo = new JamRepository(con);
			Jam activeJam = jamRepo.getActiveJam(event.getGuild().getIdLong());
			if (activeJam != null) {
				JamSubmissionRepository submissionRepo = new JamSubmissionRepository(con);
				List<JamSubmission> submissions = submissionRepo.getSubmissions(activeJam).stream().limit(25).toList();
				submissions.forEach(submission ->
						choices.add(new Command.Choice(String.format("Submission by %s", event.getJDA().getUserById(submission.getUserId()).getAsTag()), submission.getId())));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return event.replyChoices(choices);
	}
}
