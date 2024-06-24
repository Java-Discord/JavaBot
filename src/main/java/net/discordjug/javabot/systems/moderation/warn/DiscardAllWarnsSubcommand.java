package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn discard-all command.</h3>
 */
public class DiscardAllWarnsSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public DiscardAllWarnsSubcommand(BotConfig botConfig, ModerationService moderationService) {
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		setCommandData(new SubcommandData("discard-all", "Discards all warns from a single user.")
				.addOption(OptionType.USER, "user", "The user which warns should be discarded.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userMapping = event.getOption("user");
		if (userMapping == null) {
			Responses.error(event, "Please provide a valid user.").queue();
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
		User target = userMapping.getAsUser();
		moderationService.discardAllWarns(target, event.getMember());
		Responses.success(event, "Warns Discarded", "Successfully discarded all warns from **%s**.", UserUtils.getUserTag(target)).queue();
	}
}

