package net.javadiscord.javabot.systems.qotw.commands.view;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/qotw-view` command.
 * It allows to view previous QOTWs and their answers.
 */
public class QOTWViewCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 */
	public QOTWViewCommand() {
		setSlashCommandData(Commands.slash("qotw-view", "Query 'Questions of the Week' and their answers")
				.setDefaultPermissions(DefaultMemberPermissions.ENABLED)
				.setGuildOnly(true));
		addSubcommands(new QOTWQuerySubcommand(), new QOTWListAnswersSubcommand(), new QOTWViewAnswerSubcommand());

	}
}
