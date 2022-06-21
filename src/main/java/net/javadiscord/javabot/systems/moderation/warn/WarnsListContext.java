package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

import java.time.LocalDateTime;

/**
 * <h3>This class represents the "Show Warns" User Context Menu command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListContext extends ContextCommand.User {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 */
	public WarnsListContext() {
		setCommandData(Commands.user("Show Warns")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(UserContextInteractionEvent event) {
		if (Checks.checkGuild(event)) {
			Responses.error(event, "This command may only be used inside of a server.").queue();
			return;
		}
		event.deferReply(false).queue();
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Bot.config.get(event.getGuild()).getModeration().getWarnTimeoutDays());
		DbHelper.doDaoAction(WarnRepository::new, dao ->
				event.getHook().sendMessageEmbeds(WarnsListCommand.buildWarnsEmbed(dao.getWarnsByUserId(event.getTarget().getIdLong(), cutoff), event.getTarget())).queue());
	}

}
