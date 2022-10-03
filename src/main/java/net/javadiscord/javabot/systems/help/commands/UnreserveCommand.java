package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.systems.help.forum.ForumHelpManager;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
public class UnreserveCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public UnreserveCommand() {
		setSlashCommandData(Commands.slash("unreserve", "Unreserves this help channel so that others can use it.")
				.setGuildOnly(true)
				.addOption(OptionType.STRING, "reason", "The reason why you're unreserving this channel", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		// check whether the channel type is either text or thread (possible forum post?)
		if (event.getChannelType() != ChannelType.TEXT && event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
			replyInvalidChannel(event);
			return;
		}
		// handle forum-based help system
		if (event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD) {
			handleForumBasedHelp(event, event.getChannel().asThreadChannel());
		}
		// handle text-based help system
		if (event.getChannelType() == ChannelType.TEXT) {
			handleTextBasedHelp(event, event.getChannel().asTextChannel());
		}
	}

	private void handleForumBasedHelp(SlashCommandInteractionEvent event, @NotNull ThreadChannel postThread) {
		if (postThread.getParentChannel().getType() != ChannelType.FORUM) {
			replyInvalidChannel(event);
		}
		ForumHelpManager manager = new ForumHelpManager(postThread);
		if (manager.isForumEligibleToBeUnreserved(event)) {
			manager.close(event, event.getUser().getIdLong() == postThread.getOwnerIdLong(),
					event.getOption("reason", null, OptionMapping::getAsString)
			);
		} else {
			Responses.warning(event, "Could not close this post", "You're not allowed to close this post.").queue();
		}
	}

	private void handleTextBasedHelp(@NotNull SlashCommandInteractionEvent event, TextChannel channel) {
		HelpConfig config = Bot.getConfig().get(event.getGuild()).getHelpConfig();
		HelpChannelManager channelManager = new HelpChannelManager(config);
		User owner = channelManager.getReservedChannelOwner(channel);
		if (isTextEligibleToBeUnreserved(event, channel, config, owner)) {
			String reason = event.getOption("reason", null, OptionMapping::getAsString);
			event.deferReply(true).queue();
			channelManager.unreserveChannelByOwner(channel, owner, reason, event);
		} else {
			Responses.warning(event, "Could not unreserve this channel. This command only works in help channels you've reserved.").queue();
		}
	}

	private void replyInvalidChannel(CommandInteraction interaction) {
		Responses.warning(interaction, "Invalid Channel",
						"This command may only be used in either the text-channel-based help system, or in our new forum help system.")
				.queue();
	}

	private boolean isTextEligibleToBeUnreserved(SlashCommandInteractionEvent event, TextChannel channel, HelpConfig config, User owner) {
		return channelIsInReservedCategory(channel, config) &&
				(isUserWhoReservedChannel(event, owner) || memberHasHelperRole(event) || memberHasStaffRole(event));
	}

	private boolean channelIsInReservedCategory(@NotNull TextChannel channel, @NotNull HelpConfig config) {
		return config.getReservedChannelCategory().equals(channel.getParentCategory());
	}

	private boolean isUserWhoReservedChannel(SlashCommandInteractionEvent event, User owner) {
		return owner != null && event.getUser().equals(owner);
	}

	private boolean memberHasStaffRole(@NotNull SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(Bot.getConfig().get(event.getGuild()).getModerationConfig().getStaffRole());
	}

	private boolean memberHasHelperRole(@NotNull SlashCommandInteractionEvent event) {
		return event.getMember() != null &&
				event.getMember().getRoles().contains(Bot.getConfig().get(event.getGuild()).getHelpConfig().getHelperRole());
	}
}
