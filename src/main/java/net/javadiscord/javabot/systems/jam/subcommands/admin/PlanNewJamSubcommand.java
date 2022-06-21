package net.javadiscord.javabot.systems.jam.subcommands.admin;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * <h3>This class represents the /jam-admin plan-new-jam command.</h3>
 * This subcommand allows users to plan a new Jam with some basic starter
 * information.
 */
public class PlanNewJamSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public PlanNewJamSubcommand() {
		setSubcommandData(new SubcommandData("plan-new-jam", "Creates a new Java Jam for the future.")
				.addOption(OptionType.STRING, "start-date", "The date at which the Jam should start. Format as DD-MM-YYYY.", true)
				.addOption(OptionType.STRING, "name", "A name for this Jam. Typical usage is `'{name} Jam'`.", false));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping startOption = event.getOption("start-date");
		if (startOption == null) {
			Responses.warning(event, "Missing start date.").queue();
			return;
		}
		LocalDate startsAt;
		try {
			startsAt = LocalDate.parse(startOption.getAsString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		} catch (DateTimeParseException e) {
			Responses.warning(event, "Invalid start date. Must be formatted as `dd-MM-yyyy`. For example, June 5th, 2017 is formatted as `05-06-2017`.").queue();
			return;
		}
		if (startsAt.isBefore(LocalDate.now())) {
			Responses.warning(event, "Invalid start date. The Jam cannot start in the past.").queue();
			return;
		}
		long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
		String name = null;
		OptionMapping nameOption = event.getOption("name");
		if (nameOption != null) {
			name = nameOption.getAsString();
		}
		final String nameFinal = name; // So we can pass the variable in the lambda expression.
		event.deferReply().queue();
		Bot.asyncPool.submit(() -> createNewJam(event.getHook(), guildId, nameFinal, startsAt));
	}

	private void createNewJam(InteractionHook hook, long guildId, String name, LocalDate startsAt) {
		try (Connection con = Bot.dataSource.getConnection()) {
			JamRepository jamRepository = new JamRepository(con);

			Jam activeJam = jamRepository.getActiveJam(guildId);
			if (activeJam != null) {
				Responses.warning(hook, "There is already an active Jam (id = `" + activeJam.getId() + "`). Complete that Jam before planning a new one.").queue();
				return;
			}
			Jam jam = new Jam();
			jam.setGuildId(guildId);
			jam.setName(name);
			jam.setStartedBy(hook.getInteraction().getUser().getIdLong());
			jam.setStartsAt(startsAt);
			jam.setCompleted(false);
			jamRepository.saveNewJam(jam);
			Responses.success(hook, "Jam Created", "Jam has been created! *Jam ID = `" + jam.getId() + "`*. Use `/jam info` for more info.").queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(hook, "Error occurred while creating the Jam: " + e.getMessage()).queue();
		}
	}
}
