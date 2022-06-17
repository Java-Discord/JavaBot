package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that allows staff-members to discard any warn by their id.
 */
public class DiscardWarnByIdSubCommand extends SlashCommand.Subcommand {
	public DiscardWarnByIdSubCommand() {
		setSubcommandData(new SubcommandData("discard-by-id", "Discards a single warn, based on its id.")
				.addOption(OptionType.INTEGER, "id", "The warn's unique identifier.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("id");
		if (idMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		if (Checks.checkGuild(event)) {
			Responses.error(event, "This command may only be used inside of a server.").queue();
			return;
		}
		int id = idMapping.getAsInt();
		ModerationService service = new ModerationService(event);
		if (service.discardWarnById(id, event.getUser())) {
			Responses.success(event, "Warn Discarded", String.format("Successfully discarded the specified warn with id `%s`", id)).queue();
		} else {
			Responses.error(event, String.format("Could not find and/or discard warn with id `%s`", id)).queue();
		}
	}
}

