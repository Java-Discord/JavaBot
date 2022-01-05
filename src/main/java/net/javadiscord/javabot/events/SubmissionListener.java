package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubCommand;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Contains methods and events used for the QOTW-Submission system.
 */
@Slf4j
public class SubmissionListener extends ListenerAdapter {

	/**
	 * Gets called when the user presses the "Send Submission" button.
	 * <p>
	 * Sends the submission to the {@link StartupListener#preferredGuild}
	 * </p>
	 *
	 * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
	 */
	public void dmSubmissionSend(ButtonClickEvent event) {
		Guild guild = StartupListener.preferredGuild;
		var config = Bot.config.get(guild);

		var e = new EmbedBuilder()
				.setColor(config.getSlashCommand().getDefaultColor())
				.setAuthor("Submission by " + event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
				.setDescription(event.getMessage().getEmbeds().get(0).getDescription())
				.setFooter("ID: " + event.getUser().getId())
				.setTimestamp(Instant.now())
				.build();

		config.getQotw().getSubmissionChannel().sendMessageEmbeds(e)
				.setActionRows(
						ActionRow.of(
								Button.success("submission:approve:" + event.getUser().getId(), "Approve"),
								Button.danger("submission:decline:" + event.getUser().getId(), "Decline"),
								Button.secondary("submission:getraw:" + event.getUser().getId(), "Get Raw"),
								Button.secondary("utils:delete", "🗑️")))
				.queue();

		event.getHook().editOriginalComponents()
				.setActionRows(ActionRow.of(Button.success("dm-submission:send:" + event.getUser().getId(),
						"Submission sent").asDisabled())).queue();

		log.info("{}[{}]{} User {} sent a submission.",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET,
				event.getUser().getAsTag());
	}

	/**
	 * Gets called when the user presses the "Cancel" button.
	 * <p>
	 * Cancels the current process.
	 * </p>
	 *
	 * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
	 */
	public void dmSubmissionCancel(ButtonClickEvent event) {
		event.getHook().editOriginalComponents()
				.setActionRows(ActionRow.of(Button.danger("dm-submission:cancel:" + event.getUser().getId(),
						"Process canceled").asDisabled())).queue();
	}

	/**
	 * Gets called when a moderator presses the "Approve" button on a submission.
	 * <p>
	 * Approves the corresponding submission and grants 1 QOTW-Point.
	 * </p>
	 *
	 * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
	 */
	public void submissionApprove(ButtonClickEvent event) {
		var userId = event.getMessage().getEmbeds().get(0)
				.getFooter().getText().replace("ID: ", "");
		event.getGuild().retrieveMemberById(userId).queue(member -> {
			new IncrementSubCommand().correct(member);
			log.info("{}[{}]{} Submission by User {} was approved by {}",
					Constants.TEXT_WHITE, event.getGuild().getName(), Constants.TEXT_RESET,
					member.getUser().getAsTag(), event.getUser().getAsTag());

			event.getHook().editOriginalComponents()
					.setActionRows(ActionRow.of(
							Button.success("submission:approve:" + userId,
									"Approved by " + event.getMember().getUser().getAsTag()).asDisabled(),
							Button.secondary("submission:getraw:" + event.getUser().getId(), "Get Raw")
					)).queue();
		});

	}

	/**
	 * Gets called when a moderator presses the "Decline" button on a submission.
	 * <p>
	 * Declines the corresponding submission.
	 * </p>
	 *
	 * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
	 */
	public void submissionDecline(ButtonClickEvent event) {
		event.getHook().editOriginalComponents()
				.setActionRows(ActionRow.of(
						Button.danger("submission:decline:" + event.getUser().getId(),
								"Declined by " + event.getMember().getUser().getAsTag()).asDisabled(),
						Button.secondary("submission:getraw:" + event.getUser().getId(), "Get Raw")
				)).queue();
	}


	/**
	 * Gets called when a moderator presses the "Get Raw" button on a submission.
	 * <p>
	 * Sends a file that contains the Raw Message/Submission content
	 * </p>
	 *
	 * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
	 */
	public void submissionGetRaw(ButtonClickEvent event) {
		var description = event.getMessage().getEmbeds().get(0).getDescription();
		event.getHook()
				.sendFile(new ByteArrayInputStream(description.getBytes(StandardCharsets.UTF_8)), event.getUser().getId() + ".txt")
				.addActionRow(Button.secondary("utils:delete", "🗑️"))
				.queue();
	}

	/**
	 * Gets triggered when a user sends a direct message to the bot.
	 * <p>
	 * If set by the {@link StartupListener#preferredGuild}, this will accept submissions
	 * </p>
	 */
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getChannelType() != ChannelType.PRIVATE || event.getAuthor().isBot() || event.getAuthor().isSystem())
			return;

		Guild guild = StartupListener.preferredGuild;
		var config = Bot.config.get(guild);
		if (!config.getQotw().isDmEnabled()) return;

		String content = event.getMessage().getContentDisplay();

		var e = new EmbedBuilder()
				.setColor(config.getSlashCommand().getDefaultColor())
				.setAuthor("Question of the Week | Submission", null, event.getAuthor().getEffectiveAvatarUrl())
				.setDescription(content)
				.setFooter("NOTE: spamming submissions may result in a warn")
				.setTimestamp(Instant.now());

		event.getChannel().sendMessageEmbeds(e.build()).setActionRows(
						ActionRow.of(
								Button.success("dm-submission:send:" + event.getAuthor().getId(), "Send Submission"),
								Button.danger("dm-submission:cancel:" + event.getAuthor().getId(), "Cancel")))
				.queue();
	}
}
