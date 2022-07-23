package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * <h3>This class represents the /move-conversation command.</h3>
 */
public class MoveConversationCommand extends SlashCommand {
	private static final String MOVE_TO_MESSAGE = "``` ```\n\uD83D\uDCE4 %s has requested to move **this conversation** over to %s\n%s\n\n``` ```";
	private static final String MOVED_FROM_MESSAGE = "\uD83D\uDCE5 Conversation moved **here** from %s by %s\n%s";
	private static final Permission[] REQUIRED_PERMISSIONS = new Permission[]{
			Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL
	};

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public MoveConversationCommand() {
		setSlashCommandData(Commands.slash("move-conversation", "Allows to move the current conversation into another channel!")
				.addOptions(
						new OptionData(OptionType.CHANNEL, "channel", "Where should the current conversation be continued?", true)
								.setChannelTypes(ChannelType.TEXT, ChannelType.GUILD_NEWS_THREAD, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.VOICE, ChannelType.STAGE)
				).setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping channelMapping = event.getOption("channel");
		if (channelMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		GuildMessageChannel channel = channelMapping.getAsChannel().asGuildMessageChannel();
		if (event.getChannel().getIdLong() == channel.getIdLong()) {
			Responses.warning(event, "Invalid Channel", "You cannot move the conversation to the same channel!").queue();
			return;
		}
		if (isInvalidChannel(event.getMember(), channel)) {
			Responses.warning(event, "Invalid Channel", "You're not allowed to move the conversation to %s", channel.getAsMention()).queue();
			return;
		}
		event.deferReply(true).queue();
		sendMoveToChannelMessage(event, channel).queue(movedFrom ->
				sendMovedFromChannelMessage(event, channel, movedFrom).queue(movedTo ->
						editMoveToChannelMessage(event, movedFrom, movedTo).queue(s ->
								event.getHook().sendMessage("Done!").queue()
						)
				)
		);
	}

	/**
	 * Checks if the specified {@link Member} does have the required permissions in order
	 * to move the conversation to the specified channel.
	 *
	 * @param member  The {@link Member} which executed the command.
	 * @param channel The {@link GuildMessageChannel} the conversation should be moved to.
	 * @return Whether the {@link GuildMessageChannel} provided by the {@link Member} is invalid.
	 */
	private boolean isInvalidChannel(@NotNull Member member, GuildMessageChannel channel) {
		ModerationConfig config = Bot.config.get(member.getGuild()).getModerationConfig();
		return !member.hasPermission(channel, REQUIRED_PERMISSIONS) ||
				// check thread permissions
				channel.getType().isThread() && !member.hasPermission(channel, Permission.MESSAGE_SEND_IN_THREADS) ||
				// check for suggestion channel                                 check for share knowledge channel
				channel.getIdLong() == config.getSuggestionChannelId() || channel.getIdLong() == config.getShareKnowledgeChannelId() ||
				// check for job channel                                        check for application channel
				channel.getIdLong() == config.getJobChannelId() || channel.getIdLong() == config.getApplicationChannelId() ||
				// check for log channel
				channel.getIdLong() == config.getLogChannelId();
	}

	private @NotNull MessageAction sendMoveToChannelMessage(@NotNull SlashCommandInteractionEvent event, @NotNull GuildMessageChannel channel) {
		return event.getChannel().sendMessageFormat(MOVE_TO_MESSAGE, event.getUser().getAsMention(), channel.getAsMention(), "Awaiting Link...")
				.allowedMentions(Collections.emptySet());
	}

	private @NotNull MessageAction sendMovedFromChannelMessage(@NotNull SlashCommandInteractionEvent event, @NotNull GuildMessageChannel channel, @NotNull Message movedFrom) {
		return channel.sendMessageFormat(MOVED_FROM_MESSAGE, event.getChannel().getAsMention(), event.getUser().getAsMention(), movedFrom.getJumpUrl())
				.allowedMentions(Collections.emptySet());
	}

	private @NotNull MessageAction editMoveToChannelMessage(@NotNull SlashCommandInteractionEvent event, @NotNull Message movedFrom, @NotNull Message movedTo) {
		return movedFrom.editMessageFormat(MOVE_TO_MESSAGE, event.getUser().getAsMention(), movedTo.getChannel().getAsMention(), movedTo.getJumpUrl())
				.allowedMentions(Collections.emptySet());
	}
}
