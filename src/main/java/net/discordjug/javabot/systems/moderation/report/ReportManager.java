package net.discordjug.javabot.systems.moderation.report;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.util.InteractionUtils;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.discordjug.javabot.util.WebhookUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages all interactions regarding the report-system.
 */
@Slf4j
@RequiredArgsConstructor
@AutoDetectableComponentHandler({"resolve-report", ReportManager.REPORT_INTERACTION_NAME})
public class ReportManager implements ButtonHandler, ModalHandler {
	static final String REPORT_INTERACTION_NAME = "report";
	private final BotConfig botConfig;

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if ("resolve-report".equals(id[0])) {
			handleResolveReportButton(event, id);
		} else if (REPORT_INTERACTION_NAME.equals(id[0])&&"create-thread".equals(id[1])) {
			createReportUserThread(event, id);
		}else {
			Responses.error(event, "Unexpected button").queue();
		}
	}

	private void createReportUserThread(ButtonInteractionEvent event, String[] id) {
		TextChannel reportUserChannel = botConfig.get(event.getGuild())
			.getModerationConfig()
			.getReportUserThreadHolder();
		ThreadChannel reportThread = event.getGuild().getThreadChannelById(id[2]);
		if(reportThread==null) {
			Responses.error(event, "This report has been handled already.").queue();
			return;
		}
		List<MessageEmbed> reportEmbeds = event.getMessage().getEmbeds();
		String title;
		if (reportEmbeds.isEmpty()) {
			title = "report information";
		} else {
			title = reportEmbeds.get(0).getTitle();
		}
		reportUserChannel
			.createThreadChannel(title, true)
			.queue(reporterThread -> {
				reporterThread.getManager().setInvitable(false).queue();
				reporterThread
					.sendMessage(event.getUser().getAsMention() + 
							"\nYou can provide additional information regarding your report here.\n"
							+ "Messages sent in this thread can be seen by staff members but not other users.")
					.addEmbeds(reportEmbeds)
					.queue();
				reporterThread.addThreadMember(event.getUser()).queue(success ->
					reporterThread.getManager().setInvitable(false).queue());
				reportThread
					.sendMessageEmbeds(
							new EmbedBuilder()
							.setTitle("Additional information from reporter")
							.setDescription("The reporter created a thread for additional information: " + reporterThread.getAsMention() + "\n\n[thread link](" + reporterThread.getJumpUrl() + ")")
						.build())
					.queue();
				event.editComponents(ActionRow.of(event.getComponent().asDisabled())).queue(success -> {
					Responses.info(event.getHook(), "Information thread created", "The thread "+reporterThread.getAsMention()+" has been created for you. You can provide additional details related to your report there.").queue();
				});
				
			});
	}

	private void handleResolveReportButton(ButtonInteractionEvent event, String[] id) {
		event.deferReply(true).queue();
		ThreadChannel thread = event.getGuild().getThreadChannelById(id[1]);
		if (thread == null) {
			Responses.error(event.getHook(), "Could not find the corresponding thread channel.").queue();
			return;
		}
		Responses.info(event.getHook(), "Report resolved", "Successfully resolved this report!").queue();
		event.getMessage().editMessageComponents(ActionRow.of(Button.secondary("report-resolved", "Resolved by " + UserUtils.getUserTag(event.getUser())).asDisabled())).queue();
		resolveReport(event.getUser(), thread);
	}

	/**
	 * Resolves a report thread.
	 * This closes the current thread.
	 * This method does not check whether the current thread is actually a report thread.
	 * @param resolver the {@link User} responsible for resolving the report
	 * @param reportThread the thread of the report to resolve
	 */
	public void resolveReport(User resolver, ThreadChannel reportThread) {
		reportThread.sendMessage("This thread was resolved by " + resolver.getAsMention()).queue(
				success -> reportThread.getManager()
						.setName(String.format("[Resolved] %s", reportThread.getName()))
						.setArchived(true)
						.queue()
		);
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, List<ModalMapping> values) {
		event.deferReply(true).queue();
		String[] id = ComponentIdBuilder.split(event.getModalId());
		switch (id[1]) {
			case "user" -> handleUserReport(event.getHook(), event.getValue("reason").getAsString(), id[2]);
			case "message" -> handleMessageReport(event, id[2]);
		}
	}

	/**
	 * Builds a {@link Modal} for reporting a user using a context menu.
	 * The modal is supposed to ask for report details.
	 *
	 * @param event The {@link UserContextInteractionEvent} fired when reporting the user
	 * @return the built {@link Modal}
	 */
	protected Modal buildUserReportModal(@NotNull UserContextInteractionEvent event) {
		TextInput messageInput = TextInput.create("reason", TextInputStyle.PARAGRAPH)
				.setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
				.build();
		String title = "Report " + UserUtils.getUserTag(event.getTarget());
		return Modal.create(ComponentIdBuilder.build(REPORT_INTERACTION_NAME, "user", event.getTarget().getId()), title.substring(0, Math.min(title.length(), Modal.MAX_TITLE_LENGTH)))
				.addComponents(Label.of("Report Description", messageInput))
				.build();
	}

	/**
	 * Builds a {@link Modal} for reporting a message using a context menu.
	 * The modal is supposed to ask for report details.
	 *
	 * @param event The {@link MessageContextInteractionEvent} fired when reporting the message
	 * @return the built {@link Modal}
	 */
	protected Modal buildMessageReportModal(@NotNull MessageContextInteractionEvent event) {
		String title = "Report message";
		Member targetMember = event.getTarget().getMember();
		if (targetMember != null) {
			title += " from " + UserUtils.getUserTag(targetMember.getUser());
		}
		TextInput messageInput = TextInput.create("reason", TextInputStyle.PARAGRAPH)
				.setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
				.build();
		return Modal.create(ComponentIdBuilder.build(REPORT_INTERACTION_NAME, "message", event.getTarget().getId()), title.substring(0, Math.min(title.length(), Modal.MAX_TITLE_LENGTH)))
				.addComponents(Label.of("Report Description", messageInput))
				.build();
	}

	/**
	 * Handles a single user report.
	 *
	 * @param hook The {@link InteractionHook} to respond to.
	 * @param reason The reason for reporting this user.
	 * @param targetId The targeted user's id.
	 * @return The {@link WebhookMessageCreateAction}.
	 */
	WebhookMessageCreateAction<Message> handleUserReport(InteractionHook hook, @NotNull String reason, String targetId) {
		if (reason.isBlank()) {
			return Responses.error(hook, "No report reason was provided.");
		}
		hook.getJDA().retrieveUserById(targetId).queue(target -> {
			GuildConfig config = botConfig.get(hook.getInteraction().getGuild());
			EmbedBuilder embed = buildReportEmbed(target, hook.getInteraction().getUser(), reason, hook.getInteraction().getChannel());
			embed.setTitle(String.format("%s reported %s", hook.getInteraction().getUser().getName(), target.getName()));
			MessageChannel reportChannel = config.getModerationConfig().getReportChannel();
			if (reportChannel == null) {
				Responses.error(hook, "I could not find the report channel. Please ask the administrators of this server to set one!").queue();
				return;
			}
			reportChannel.sendMessageEmbeds(embed.build())
					.queue(m -> this.createReportThread(m, target.getIdLong(), config.getModerationConfig(),
							reportThread -> {
								sendReportResponse(hook, target, embed, reportThread);
							}));
		}, failure -> {
			Responses.error(hook, "The user to report seems not to exist any more.").queue();
			log.warn("Cannot retrieve user {} when reporting them", targetId, failure);
		});
		return null;
	}

	private void sendReportResponse(InteractionHook hook, User targetUser, EmbedBuilder reportEmbed, ThreadChannel reportThread) {
		reportEmbed.setDescription("Successfully reported " + "`" + UserUtils.getUserTag(targetUser) + "`!\nYour report has been send to our Moderators.\nIn case you want to supply additional details, please use the \"Create thread\" button below.");
		hook.sendMessageEmbeds(reportEmbed.build())
			.addComponents(ActionRow.of(
					Button.secondary(
					ComponentIdBuilder.build(REPORT_INTERACTION_NAME, "create-thread", reportThread.getId()),
					"Create thread for providing further details"
			))).queue();
	}

	private void handleMessageReport(ModalInteractionEvent event, String messageId) {
		String reason = event.getValue("reason").getAsString();
		if (reason.isBlank()) {
			Responses.error(event.getHook(), "No report reason was provided.").queue();
			return;
		}
		event.getMessageChannel().retrieveMessageById(messageId).queue(target -> {
			GuildConfig config = botConfig.get(event.getGuild());
			EmbedBuilder embed = buildReportEmbed(target.getAuthor(), event.getUser(), reason, event.getChannel());
			embed.setTitle(String.format("%s reported a Message from %s", event.getUser().getName(), target.getAuthor().getName()));
			embed.addField("Message", String.format("[Jump to Message](%s)", target.getJumpUrl()), false);
			MessageChannel reportChannel = config.getModerationConfig().getReportChannel();
			reportChannel.sendMessageEmbeds(embed.build()).queue(m -> createReportThread(m, target.getAuthor().getIdLong(), config.getModerationConfig(), thread->{
				sendReportResponse(event.getHook(), target.getAuthor(), embed, thread);
				WebhookUtil.ensureWebhookExists(thread.getParentChannel().asStandardGuildMessageChannel(), wh->{
					WebhookUtil.mirrorMessageToWebhook(wh, target, target.getContentRaw(), thread.getIdLong(), null, null);
				});
			}));
		}, failure -> {
			Responses.error(event.getHook(), "The author of the message to report seems not to exist any more.").queue();
			log.info("Cannot retrieve reported message {} in channel {} - the message might have been deleted", messageId, event.getChannel(), failure);
		});

	}

	private ActionRow setComponents(long targetId, long threadId) {
		return ActionRow.of(
				Button.secondary(ComponentIdBuilder.build("resolve-report", threadId), "Mark as resolved"),
				Button.danger(String.format(InteractionUtils.BAN_TEMPLATE, targetId), "Ban"),
				Button.danger(String.format(InteractionUtils.KICK_TEMPLATE, targetId), "Kick"),
				Button.primary(String.format(InteractionUtils.WARN_TEMPLATE, targetId), "Warn")
		);
	}

	private void createReportThread(Message message, long targetId, ModerationConfig config, Consumer<ThreadChannel> onSuccess) {
		message.createThreadChannel(message.getEmbeds().get(0).getTitle()).queue(
				thread -> {
					thread.sendMessage(config.getStaffRole().getAsMention())
						.setComponents(setComponents(targetId, thread.getIdLong()))
						.queue();
					onSuccess.accept(thread);
				}
		);
	}

	private EmbedBuilder buildReportEmbed(User reported, User reportedBy, String reason, Channel channel) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(reported), null, reported.getEffectiveAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Member", reported.getAsMention(), true)
				.addField("Reported by", reportedBy.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Reported on", String.format("<t:%s:F>", Instant.now().getEpochSecond()), false)
				.addField("ID", String.format("``` %s ```", reported.getId()), true)
				.addField("Reason", String.format("``` %s ```", reason), false)
				.setFooter(UserUtils.getUserTag(reportedBy), reportedBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now());
	}
}
