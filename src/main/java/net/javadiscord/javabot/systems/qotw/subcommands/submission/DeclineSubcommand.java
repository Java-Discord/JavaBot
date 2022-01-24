package net.javadiscord.javabot.systems.qotw.subcommands.submission;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.qotw.SubmissionManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Subcommand that allows staff-members to decline QOTW-Submissions.
 */
@Slf4j
public class DeclineSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) throws ResponseException {
		var threadOption = event.getOption("thread");
		if (threadOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var channel = threadOption.getAsGuildChannel();
		if (channel.getType() != ChannelType.GUILD_PRIVATE_THREAD) {
			return Responses.error(event, "Channel must be a private Thread.");
		}
		var thread = (ThreadChannel) channel;
		var config = Bot.config.get(event.getGuild());
		var submissionChannel = config.getQotw().getSubmissionChannel();
		if (thread.getParentChannel() != submissionChannel) {
			return Responses.error(event, "Thread must be part of " + submissionChannel.getAsMention());
		}
		var manager = new SubmissionManager(config.getQotw());
		var memberOptional = manager.getSubmissionThreadOwner(thread);
		if (memberOptional.isEmpty()) {
			return Responses.error(event, "Cannot decline a submission of a user who is not a member of this server");
		}
		var member = memberOptional.get().getMember();
		var embed = buildDeclineSubmissionEmbed(member, event.getMember(), thread);
		thread.sendMessageEmbeds(embed).queue();
		config.getModeration().getLogChannel()
				.sendMessageEmbeds(embed)
				.addFile(new ByteArrayInputStream(manager.archiveThreadContents(thread).getBytes(StandardCharsets.UTF_8)), channel.getId() + ".txt")
				.queue();
		log.info("Submission by User {} was declined by {}", member.getUser().getAsTag(), event.getUser().getAsTag());
		if (!thread.isArchived()) thread.getManager().setArchived(true).queue();
		return Responses.success(event, "Submission declined", String.format("Successfully declined %s's submission.", member.getAsMention()));
	}

	private MessageEmbed buildDeclineSubmissionEmbed(Member member, Member declinedBy, ThreadChannel channel) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getErrorColor())
				.setTitle("Submission declined")
				.addField("Declined by", declinedBy.getAsMention(), true)
				.addField("Thread", channel.getAsMention(), false)
				.build();
	}
}
