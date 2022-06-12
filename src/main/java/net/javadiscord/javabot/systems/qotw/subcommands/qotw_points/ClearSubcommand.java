package net.javadiscord.javabot.systems.qotw.subcommands.qotw_points;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;

import java.util.Optional;

/**
 * Subcommand that allows staff-members to clear a user's QOTW-Account.
 */
public class ClearSubcommand extends SlashCommand.Subcommand {
	public ClearSubcommand() {
		setSubcommandData(new SubcommandData("clear", "Clears all QOTW-Points of the provided user.")
				.addOption(OptionType.USER, "user", "The user whose points should be cleared.", true)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping memberMapping = event.getOption("user");
		if (memberMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		Member member = memberMapping.getAsMember();
		if (member == null) {
			Responses.error(event, "The user must be part of this server!").queue();
			return;
		}
		event.deferReply(true).queue();
		DbHelper.doDaoAction(QuestionPointsRepository::new, dao -> {
			Optional<QOTWAccount> accountOptional = dao.getByUserId(member.getIdLong());
			if (accountOptional.isEmpty()) {
				Responses.error(event.getHook(), "Could not find QOTW Account for user: " + member.getAsMention()).queue();
				return;
			}
			QOTWAccount account = accountOptional.get();
			account.setPoints(0);
			dao.update(account);
			Responses.success(event.getHook(), "QOTW-Points cleared",
					"Successfully cleared all QOTW-Points from user " + member.getAsMention())
					.queue();
		});
	}
}


