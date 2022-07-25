package net.javadiscord.javabot.systems.moderation.timeout;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * An abstraction of {@link com.dynxsty.dih4jda.interactions.commands.SlashCommand.Subcommand} which handles all
 * timeout-related commands.
 */
public abstract class TimeoutSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasPermission(event.getGuild(), Permission.MODERATE_MEMBERS)) {
			Responses.replyInsufficientPermissions(event, Permission.MODERATE_MEMBERS).queue();
			return;
		}
		OptionMapping memberMapping = event.getOption("member");
		if (memberMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		Member member = memberMapping.getAsMember();
		if (member == null) {
			Responses.replyMissingMember(event).queue();
			return;
		}
		if (!event.getGuild().getSelfMember().canInteract(member)) {
			Responses.replyCannotInteract(event, member).queue();
			return;
		}
		handleTimeoutCommand(event, member).queue();
	}

	protected abstract ReplyCallbackAction handleTimeoutCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member member);
}
