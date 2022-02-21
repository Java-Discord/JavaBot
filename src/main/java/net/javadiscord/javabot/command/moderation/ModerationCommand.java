package net.javadiscord.javabot.command.moderation;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

/**
 * Basic moderation command.
 */
public abstract class ModerationCommand implements ISlashCommand {
	private boolean allowThreads = true;

	@Override
	public final ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		if (event.getGuild() == null) {
			return Responses.error(event, "Moderation commands can only be used inside guilds.");
		}

		if (allowThreads) {
			if (event.getChannelType() == ChannelType.TEXT || event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD || event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD) {
				return Responses.error(event, "This command can only be performed in a server text channel or thread.");
			}
		} else {
			if (event.getChannelType() == ChannelType.TEXT) {
				return Responses.error(event, "This command can only be performed in a server text channel.");
			}
		}

		Member member = event.getMember();

		if (member == null) {
			return Responses.error(event, "Unexpected error has occurred, Slash Command member was null.");
		}

		return handleModerationCommand(event, member);
	}

	protected void setAllowThreads(boolean allowThreads) {
		this.allowThreads = allowThreads;
	}

	protected abstract ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) throws ResponseException;
}
