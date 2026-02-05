package net.discordjug.javabot.systems.help.commands;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.h2db.DbActions;
import net.discordjug.javabot.systems.help.HelpManager;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
@AutoDetectableComponentHandler(UnreserveCommand.UNRESERVE_ID)
public class UnreserveCommand extends SlashCommand implements ModalHandler {
	static final String UNRESERVE_ID = "unreserve";
	private static final int MINIMUM_REASON_LENGTH = 11;
	private static final String REASON_ID = "reason";
	private final BotConfig botConfig;
	private final DbActions dbActions;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;
	private final UserPreferenceService preferenceService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param dbActions A utility object providing various operations on the main database
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTION SQL Table
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table
	 * @param preferenceService Service for user preferences
	 */
	public UnreserveCommand(BotConfig botConfig, DbActions dbActions, HelpTransactionRepository helpTransactionRepository, HelpAccountRepository helpAccountRepository, UserPreferenceService preferenceService) {
		this.botConfig = botConfig;
		this.dbActions = dbActions;
		this.helpAccountRepository = helpAccountRepository;
		this.helpTransactionRepository = helpTransactionRepository;
		this.preferenceService = preferenceService;
		setCommandData(Commands.slash(UNRESERVE_ID, "Unreserves this post marking your question/issue as resolved.")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, REASON_ID, "The reason why you're unreserving this channel", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		String reason = event.getOption(REASON_ID, null, OptionMapping::getAsString);
		onCloseRequest(event, event, event.getChannel(), reason, ()->{
			TextInput reasonInput = TextInput
				.create(REASON_ID, TextInputStyle.SHORT)
				.setRequiredRange(MINIMUM_REASON_LENGTH, 100)
				.setRequired(true)
				.setPlaceholder(reason == null ? "Please enter the reason you are closing this post here" : reason)
				.build();
			Modal modal = Modal
					.create(ComponentIdBuilder.build(UNRESERVE_ID), "Close post")
					.addComponents(Label.of("Reason", reasonInput))
					.build();
			event.replyModal(modal).queue();
		});
	}
	
	@Override
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
		values
			.stream()
			.filter(mapping -> REASON_ID.equals(mapping.getCustomId()))
			.map(mapping -> mapping.getAsString())
			.filter(reason -> !isReasonInvalid(reason))
			.findAny()
			.ifPresentOrElse(reason -> {
				onCloseRequest(event, event, event.getChannel(), reason, ()->{
					Responses.error(event, "The provided reason is missing or not valid").queue();
					ExceptionLogger.capture(new IllegalStateException("A reason was expected but not present"), getClass().getName());
				});
			}, () -> Responses.warning(event, "A valid reason must be provided").queue());
		
	}

	private void onCloseRequest(Interaction interaction, IReplyCallback replyCallback, MessageChannelUnion channel, String reason, Runnable noReasonHandler) {
		ChannelType channelType = channel.getType();
		// check whether the channel type is either text or thread (possible forum post?)
		if (channelType != ChannelType.TEXT && channelType != ChannelType.GUILD_PUBLIC_THREAD) {
			replyInvalidChannel(replyCallback);
			return;
		}
		ThreadChannel postThread = channel.asThreadChannel();
		if (postThread.getParentChannel().getType() != ChannelType.FORUM) {
			replyInvalidChannel(replyCallback);
			return;
		}
		HelpManager manager = new HelpManager(postThread, dbActions, botConfig, helpAccountRepository, helpTransactionRepository, preferenceService);
		if (manager.isForumEligibleToBeUnreserved(interaction)) {
			if (replyCallback.getUser().getIdLong() != postThread.getOwnerIdLong() && isReasonInvalid(reason)) {
				noReasonHandler.run();
				return;
			}
			manager.close(replyCallback,
					replyCallback.getUser().getIdLong() == manager.getPostThread().getOwnerIdLong(),
					reason);
		} else {
			Responses.warnin(replyCallback, "Could not close this post", "You're not allowed to close this post.").queue();
		}
	}

	private boolean isReasonInvalid(String reason) {
		return reason == null || reason.length() < MINIMUM_REASON_LENGTH;
	}

	private void replyInvalidChannel(IReplyCallback replyCallback) {
		Responses.warnin(replyCallback, "Invalid Channel",
						"This command may only be used in either the text-channel-based help system, or in our new forum help system.")
				.queue();
	}
}
