package net.javadiscord.javabot.systems.help.commands;

import java.util.concurrent.ScheduledExecutorService;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.util.Responses;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
public class UnreserveCommand extends SlashCommand {
	private final BotConfig botConfig;
	private final ScheduledExecutorService asyncPool;
	private final DbActions dbActions;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 * @param dbActions A utility object providing various operations on the main database
	 */
	public UnreserveCommand(BotConfig botConfig, ScheduledExecutorService asyncPool, DbActions dbActions) {
		this.botConfig = botConfig;
		this.asyncPool = asyncPool;
		this.dbActions = dbActions;
		setSlashCommandData(Commands.slash("unreserve", "Unreserves this help channel so that others can use it.")
				.setGuildOnly(true)
				.addOption(OptionType.STRING, "reason", "The reason why you're unreserving this channel", false)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		TextChannel channel = event.getChannel().asTextChannel();
		HelpChannelManager channelManager = new HelpChannelManager(botConfig, event.getGuild(), dbActions, asyncPool);
		User owner = channelManager.getReservedChannelOwner(channel);
		if (isEligibleToBeUnreserved(event, channel, botConfig.get(event.getGuild()).getHelpConfig(), owner)) {
			String reason = event.getOption("reason", null, OptionMapping::getAsString);
			event.deferReply(true).queue();
			channelManager.unreserveChannelByOwner(channel, owner, reason, event);
		} else {
			Responses.warning(event, "Could not unreserve this channel. This command only works in help channels you've reserved.").queue();
		}
	}

	private boolean isEligibleToBeUnreserved(SlashCommandInteractionEvent event, TextChannel channel, HelpConfig config, User owner) {
		return channelIsInReservedCategory(channel, config) &&
				(isUserWhoReservedChannel(event, owner) || memberHasHelperRole(event) || memberHasStaffRole(event));
	}

	private boolean channelIsInReservedCategory(TextChannel channel, HelpConfig config) {
		return config.getReservedChannelCategory().equals(channel.getParentCategory());
	}

	private boolean isUserWhoReservedChannel(SlashCommandInteractionEvent event, User owner) {
		return owner != null && event.getUser().equals(owner);
	}

	private boolean memberHasStaffRole(SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(botConfig.get(event.getGuild()).getModerationConfig().getStaffRole());
	}

	private boolean memberHasHelperRole(SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(botConfig.get(event.getGuild()).getHelpConfig().getHelperRole());
	}
}
