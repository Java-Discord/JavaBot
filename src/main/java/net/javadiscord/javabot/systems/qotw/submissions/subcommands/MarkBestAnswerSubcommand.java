package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubcommand;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.AutocompleteUtils;
import net.javadiscord.javabot.util.MessageActionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Allows members of the QOTW Review Team to mark a single submission as the "Best Answer" of the current Week.
 */
public class MarkBestAnswerSubcommand implements SlashCommand {

	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		OptionMapping threadIdOption = event.getOption("thread-id");
		if (threadIdOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		long threadId = Long.parseLong(threadIdOption.getAsString());
		GuildConfig config = Bot.config.get(event.getGuild());
		ThreadChannel submissionThread = event.getGuild().getThreadChannelById(threadId);
		if (submissionThread == null) {
			return Responses.error(event, String.format("Could not find thread with id: `%s`", threadId));
		}
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> {
			Optional<QOTWSubmission> submissionOptional = dao.getSubmissionByThreadId(threadId);
			if (submissionOptional.isEmpty()) {
				Responses.error(event.getHook(), String.format("Could not find submission with thread id: `%s`", threadId)).queue();
				return;
			}
			QOTWSubmission submission = submissionOptional.get();
			if (!submission.isReviewed() || !submission.isAccepted()) {
				Responses.error(event.getHook(), String.format("Submission must be reviewed and accepted!", threadId)).queue();
				return;
			}
			if (config.getQotw().getQuestionChannel().getThreadChannels().stream().anyMatch(thread -> thread.getName().equals(submissionThread.getName()))) {
				Responses.error(event.getHook(), String.format("Submission was already marked as the best answer.", threadId)).queue();
				return;
			}
			List<Message> messages = this.getSubmissionContent(submissionThread);
			event.getGuild().retrieveMemberById(submission.getAuthorId()).queue(
					member -> {
						if (member == null) {
							Responses.error(event.getHook(), String.format("Could not find member with id: `%s`", submission.getAuthorId())).queue();
							return;
						}
						IncrementSubcommand.correct(member, true, true);
						this.sendBestAnswer(event.getHook(), messages, member, submissionThread);
					}
			);
		});
		return event.deferReply(true);
	}

	private List<Message> getSubmissionContent(ThreadChannel thread) {
		List<Message> messages = new ArrayList<>();
		int count = thread.getMessageCount();
		while (count > 0) {
			List<Message> retrieved = thread.getHistory().retrievePast(Math.min(count, 100)).complete()
					.stream()
					.filter(m -> !m.getAuthor().isBot())
					.toList();
			messages.addAll(retrieved);
			count -= Math.min(count, 100);
		}
		Collections.reverse(messages);
		return messages;
	}

	private MessageEmbed buildBestAnswerEmbed(Member member) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setTitle("Best Answer")
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getDefaultColor())
				.setDescription(String.format("%s's submission was marked as the **best answer.**", member.getAsMention()))
				.build();
	}

	/**
	 * Sends the {@link MarkBestAnswerSubcommand#buildBestAnswerEmbed} embeds into the Question Channel and creates a new
	 * Thread on it, which contains messages of the original submission.
	 *
	 * @param hook The {@link InteractionHook} that is used to respond to the Slash Command.
	 * @param messages The submission's messages.
	 * @param member The submission's author.
	 * @param submissionThread The submission's thread.
	 */
	private void sendBestAnswer(InteractionHook hook, List<Message> messages, Member member, ThreadChannel submissionThread) {
		Bot.config.get(member.getGuild()).getQotw().getQuestionChannel().sendMessageEmbeds(this.buildBestAnswerEmbed(member)).queue(
				message -> message.createThreadChannel(submissionThread.getName()).queue(
						thread -> {
							messages.forEach(m -> {
								String messageContent = m.getContentRaw();
								if (messageContent.trim().length() == 0) messageContent = "[attachment]";
								MessageActionUtils.addAttachmentsAndSend(m, thread.sendMessage(messageContent)
										.allowedMentions(EnumSet.of(Message.MentionType.EMOTE, Message.MentionType.CHANNEL)));
							});
							Responses.success(hook, "Best Answer", String.format("Successfully marked %s as the best answer", submissionThread.getAsMention())).queue();
						}
				));
	}

	/**
	 * Replies with all accepted Question of the Week Submissions..
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public static List<Command.Choice> replyAcceptedSubmissions(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			QOTWConfig config = Bot.config.get(event.getGuild()).getQotw();
			List<QOTWSubmission> submissions = repo.getSubmissionByQuestionNumber(repo.getCurrentQuestionNumber())
					.stream()
					.filter(submission -> submission.isAccepted() && submission.isReviewed())
					.toList();
			submissions.forEach(submission -> {
				ThreadChannel thread = event.getGuild().getThreadChannelById(submission.getThreadId());
				String name = thread == null ? String.valueOf(submission.getThreadId()) : thread.getName();
				if (config.getQuestionChannel().getThreadChannels().stream().noneMatch(t -> t.getName().equals(name))) {
					choices.add(new Command.Choice(name, submission.getThreadId()));
				}
			});
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return AutocompleteUtils.filterChoices(event, choices);
	}
}
