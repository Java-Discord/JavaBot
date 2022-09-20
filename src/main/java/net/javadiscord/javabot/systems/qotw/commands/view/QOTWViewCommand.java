package net.javadiscord.javabot.systems.qotw.commands.view;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.DbHelper;

/**
 * Represents the `/qotw-view` command.
 * It allows to view previous QOTWs and their answers.
 */
public class QOTWViewCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 * @param dbActions A utility object providing various operations on the main database
	 */
	public QOTWViewCommand(BotConfig botConfig, DbHelper dbHelper, DbActions dbActions) {
		setSlashCommandData(Commands.slash("qotw-view", "Query 'Questions of the Week' and their answers")
				.setDefaultPermissions(DefaultMemberPermissions.ENABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new QOTWQuerySubcommand(dbHelper, dbActions), new QOTWListAnswersSubcommand(botConfig, dbActions), new QOTWViewAnswerSubcommand(botConfig, dbActions));

	}
}
