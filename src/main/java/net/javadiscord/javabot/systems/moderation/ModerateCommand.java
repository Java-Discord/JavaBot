package net.javadiscord.javabot.systems.moderation;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract class that represents a single moderation command.
 */
@RequiredArgsConstructor
public abstract class ModerateCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The main configuration of the bot.
	 */
	protected final BotConfig botConfig;
	private boolean requireStaff = true;

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		Member member = event.getMember();
		if (member == null) {
			Responses.replyMissingMember(event).queue();
			return;
		}
		if (requireStaff && !Checks.hasStaffRole(botConfig, member)) {
			Responses.replyStaffOnly(event, botConfig).queue();
			return;
		}
		if (event.getChannelType() != ChannelType.TEXT && event.getChannelType() != ChannelType.VOICE && !event.getChannelType().isThread()) {
			Responses.error(event, "This command can only be performed in a server text channel or thread.").queue();
			return;
		}
		handleModerationCommand(event, member).queue();
	}

	protected void setRequireStaff(boolean requireStaff) {
		this.requireStaff = requireStaff;
	}

	protected boolean isRequireStaff() {
		return requireStaff;
	}

	protected abstract ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator);
}
