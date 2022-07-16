package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * <h3>This class represents the /server-lock status command.</h3>
 */
public class LockStatusSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public LockStatusSubcommand() {
		setSubcommandData(new SubcommandData("status", "Command for changing the current server lock status.")
				.addOption(OptionType.BOOLEAN, "locked", "Whether the server should be locked or not.", true));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping lockedMapping = event.getOption("locked");
		if (lockedMapping == null) {
			Responses.error(event, "Missing required arguments");
			return;
		}
		if (!event.isFromGuild()) {
			Responses.error(event, "This command may only be used inside servers.").queue();
		}
		try {
			GuildConfig config = Bot.config.get(event.getGuild());
			boolean locked = lockedMapping.getAsBoolean();
			config.set("serverLock.locked", String.valueOf(locked));
			if (locked) {
				Bot.serverLockManager.lockServer(event.getGuild(), Collections.emptyList(), event.getUser());
			} else {
				Bot.serverLockManager.unlockServer(event.getGuild(), event.getUser());
			}
			Responses.info(event, "Server Lock Status", String.format("Successfully %slocked the current server!", locked ? "" : "un")).queue();
		} catch (UnknownPropertyException e) {
			Responses.warning(event, "Unknown Property", "Could not lock/unlock the ")
					.queue();
		}
	}
}
