package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn discard-by-id command.</h3>
 */
public class DiscardWarnByIdSubCommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public DiscardWarnByIdSubCommand(BotConfig botConfig, ModerationService moderationService) {
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		setCommandData(new SubcommandData("discard-by-id", "Discards a single warn, based on its id.")
				.addOption(OptionType.INTEGER, "id", "The warn's unique identifier.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("id");
		if (idMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if(!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		int id = idMapping.getAsInt();
		if (moderationService.discardWarnById(id, event.getMember())) {
			Responses.success(event, "Warn Discarded", "Successfully discarded the specified warn with id `%s`", id).queue();
		} else {
			Responses.error(event, "Could not find and/or discard warn with id `%s`", id).queue();
		}
	}
}

