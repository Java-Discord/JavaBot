package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.util.Responses;

import java.time.LocalDateTime;

/**
 * <h3>This class represents the "Show Warns" User Context Menu command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListContext extends ContextCommand.User {
	private final BotConfig botConfig;
	private final DbHelper dbHelper;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 */
	public WarnsListContext(BotConfig botConfig, DbHelper dbHelper) {
		this.botConfig = botConfig;
		this.dbHelper = dbHelper;
		setCommandData(Commands.user("Show Warns")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(UserContextInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		event.deferReply(false).queue();
		LocalDateTime cutoff = LocalDateTime.now().minusDays(botConfig.get(event.getGuild()).getModerationConfig().getWarnTimeoutDays());
		dbHelper.doDaoAction(WarnRepository::new, dao ->
				event.getHook().sendMessageEmbeds(WarnsListCommand.buildWarnsEmbed(dao.getWarnsByUserId(event.getTarget().getIdLong(), cutoff), event.getTarget())).queue());
	}

}
