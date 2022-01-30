package net.javadiscord.javabot.systems.jam.subcommands;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Shows some basic information about the current Java Jam.
 */
@RequiredArgsConstructor
public class JamInfoSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		Jam jam = this.fetchJam(event);
		if (jam == null) {
			return Responses.warning(event, "No Jam was found.");
		}

		event.getJDA().retrieveUserById(jam.getStartedBy()).queue(user -> {
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Jam Information")
					.setColor(Bot.config.get(event.getGuild()).getJam().getJamEmbedColor())
					.addField("Id", Long.toString(jam.getId()), true)
					.addField("Name", jam.getFullName(), true)
					.addField("Created at", jam.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy 'at' kk:mm:ss 'UTC'")), true)
					.addField("Started by", user.getAsTag(), true)
					.addField("Starts at", jam.getStartsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")), true)
					.addField("Current phase", jam.getCurrentPhase(), true);
			if (jam.getEndsAt() != null) {
				embedBuilder.addField("Ends at", jam.getEndsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")), true);
			}

			event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
		});
		return event.deferReply();
	}

	private Jam fetchJam(SlashCommandInteractionEvent event) {
		Jam jam;
		try (Connection con = Bot.dataSource.getConnection()) {
			JamRepository jamRepository = new JamRepository(con);
			OptionMapping idOption = event.getOption("id");
			if (idOption == null) {
				if (event.getGuild() == null) {
					throw new RuntimeException("Cannot find active Jam without Guild context.");
				}
				jam = jamRepository.getActiveJam(event.getGuild().getIdLong());
			} else {
				jam = jamRepository.getJam(idOption.getAsLong());
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Error occurred while fetching the Jam info.", e);
		}
		return jam;
	}
}
