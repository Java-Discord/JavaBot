package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.IMessageContextCommand;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.command.interfaces.IUserContextCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows members to report other members.
 */
@Slf4j
public class ReportCommand implements ISlashCommand, IUserContextCommand, IMessageContextCommand {

	private static final String REASON_OPTION_NAME = "reason";

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		OptionMapping option = event.getOption(REASON_OPTION_NAME);
		String reason = option == null ? "None" : option.getAsString();
		Member member = event.getOption("user").getAsMember();
		if(member == null) {
			return Responses.error(event, "Cannot report a user who is not a member of this server");
		}
		var config = Bot.config.get(event.getGuild());
		MessageChannel reportChannel = config.getModeration().getReportChannel();
		var embed = buildReportEmbed(member.getUser(), reason, event.getUser(), event.getTextChannel(), config.getSlashCommand());
		reportChannel.sendMessage("@here").setEmbeds(embed.build())
				.setActionRows(setComponents(member.getIdLong()))
				.queue();
		embed.setDescription("Successfully reported " + "`" + member.getUser().getAsTag() + "`!\nYour report has been send to our Moderators");
		return event.replyEmbeds(embed.build()).setEphemeral(true);
	}


	@Override
	public InteractionCallbackAction<InteractionHook> handleMessageContextCommandInteraction(MessageContextInteractionEvent event) throws ResponseException {
		return event.replyModal(buildMessageReportModal(event));
	}

	@Override
	public InteractionCallbackAction<InteractionHook> handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException {
		return event.replyModal(buildUserReportModal(event));
	}

	/**
	 * Builds a {@link Modal} for reporting a user using a context menu.
	 * The modal is supposed to ask for report details.
	 *
	 * @param event The {@link UserContextInteractionEvent} fired when reporting the user
	 * @return the built {@link Modal}
	 */
	private Modal buildUserReportModal(UserContextInteractionEvent event) {//TODO use that
		TextInput messageInput = TextInput.create(REASON_OPTION_NAME, "Report description", TextInputStyle.PARAGRAPH).build();
		return Modal.create("report:user:" + event.getTarget().getId(), "Report " + event.getTarget().getAsTag())
				.addActionRows(ActionRow.of(messageInput))
				.build();
	}

	/**
	 * Builds a {@link Modal} for reporting a message using a context menu.
	 * The modal is supposed to ask for report details.
	 *
	 * @param event The {@link MessageContextInteractionEvent} fired when reporting the message
	 * @return the built {@link Modal}
	 */
	private Modal buildMessageReportModal(MessageContextInteractionEvent event) {
		var title = "Report message";
		var targetMember = event.getTarget().getMember();
		if(targetMember != null) {
			title += " from " + targetMember.getUser().getAsTag();
		}
		TextInput messageInput = TextInput.create(REASON_OPTION_NAME, "Report description", TextInputStyle.PARAGRAPH).build();
		return Modal.create("report:message:" + event.getTarget().getId(), title)
				.addActionRows(ActionRow.of(messageInput))
				.build();
	}

	/**
	 * Handles all Modal Submit Interactions regarding the Report System.
	 *
	 * @param event The {@link ModalInteractionEvent} that is fired upon submitting a Modal.
	 * @param args  The modal's id, split by ":".
	 */
	public void handleModalSubmit(ModalInteractionEvent event, String[] args) {
		event.deferReply(true).queue();
		switch(args[1]) {
			case "user" -> handleUserReport(event, args[2]);
			case "message" -> handleMessageReport(event, args[2]);
		}
	}

	private void handleMessageReport(ModalInteractionEvent event, String messageId) {
		String reason = event.getValue(REASON_OPTION_NAME).getAsString();
		if(reason.isBlank()) {
			Responses.error(event.getHook(), "No report reason was provided.").queue();
			return;
		}
		event.getMessageChannel().retrieveMessageById(messageId).queue(target -> {
			var config = Bot.config.get(event.getGuild());
			var embed = buildReportEmbed(target.getAuthor(), reason, event.getUser(), event.getTextChannel(), config.getSlashCommand());
			embed.addField("Message", String.format("[Jump to Message](%s)", target.getJumpUrl()), false);
			MessageChannel reportChannel = config.getModeration().getReportChannel();
			reportChannel.sendMessage("@here").setEmbeds(embed.build())
					.setActionRows(setComponents(target.getAuthor().getIdLong()))
					.queue();
			embed.setDescription("Successfully reported " + "`" + target.getAuthor().getAsTag() + "`!\nYour report has been send to our Moderators");
			event.getHook().sendMessageEmbeds(embed.build()).queue();
		}, failure -> {
			Responses.error(event.getHook(), "The author of the message to report seems not to exist any more.").queue();
			log.info("Cannot retrieve reported message {} in channel {} - the message might have been deleted", messageId, event.getChannel(), failure);
		});

	}

	private void handleUserReport(ModalInteractionEvent event, String userId) {
		String reason = event.getValue(REASON_OPTION_NAME).getAsString();
		if(reason.isBlank()) {
			Responses.error(event.getHook(), "No report reason was provided.").queue();
			return;
		}
		event.getJDA().retrieveUserById(userId).queue(target -> {
			var config = Bot.config.get(event.getGuild());
			var embed = buildReportEmbed(target, reason, event.getUser(), event.getTextChannel(), config.getSlashCommand());
			MessageChannel reportChannel = config.getModeration().getReportChannel();
			reportChannel.sendMessage("@here").setEmbeds(embed.build())
					.setActionRows(setComponents(target.getIdLong()))
					.queue();
			embed.setDescription("Successfully reported " + "`" + target.getAsTag() + "`!\nYour report has been send to our Moderators");
			event.getHook().sendMessageEmbeds(embed.build()).queue();
		}, failure -> {
			Responses.error(event.getHook(), "The user to report seems not to exist any more.").queue();
			log.warn("Cannot retrieve user {} when reporting them", userId, failure);
		});

	}

	private ActionRow setComponents(long userId) {
		return ActionRow.of(
				Button.danger("utils:ban:" + userId, "Ban"),
				Button.danger("utils:kick:" + userId, "Kick"),
				Button.secondary("utils:delete", "🗑️")
		);
	}

	private EmbedBuilder buildReportEmbed(User reported, String reason, User reportedBy, TextChannel channel, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(reported.getAsTag(), null, reported.getEffectiveAvatarUrl())
				.setTitle("Report")
				.setColor(config.getDefaultColor())
				.addField("Member", reported.getAsMention(), true)
				.addField("Reported by", reportedBy.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Reported on", String.format("<t:%s:F>", Instant.now().getEpochSecond()), false)
				.addField("ID", String.format("```%s```", reported.getId()), true)
				.addField("Reason", String.format("```%s```", reason), false)
				.setFooter(reportedBy.getAsTag(), reportedBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now());
	}
}


