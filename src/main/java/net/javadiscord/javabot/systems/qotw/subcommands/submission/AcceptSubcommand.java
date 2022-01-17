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
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubCommand;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class AcceptSubcommand implements SlashCommandHandler {
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
		var member = manager.getSubmissionThreadOwner(thread);
		if (member == null) {
			return Responses.error(event, "Cannot accept a submission of a user who is not a member of this server");
		}
		new IncrementSubCommand().correct(member, true);
		var embed = buildAcceptSubmissionEmbed(member, event.getMember(), thread);
		thread.sendMessageEmbeds(embed).queue();
		config.getModeration().getLogChannel()
				.sendMessageEmbeds(embed)
				.addFile(new ByteArrayInputStream(manager.archiveThreadContents(thread).getBytes(StandardCharsets.UTF_8)), channel.getId() + ".txt")
				.queue();
		log.info("Submission by User {} was approved by {}", member.getUser().getAsTag(), event.getUser().getAsTag());
		if (!thread.isArchived()) thread.getManager().setArchived(true).queue();
		return Responses.success(event, "Submission accepted", String.format("Successfully accepted %s's submission.", member.getAsMention()));
	}

	private MessageEmbed buildAcceptSubmissionEmbed(Member member, Member acceptedBy, ThreadChannel channel) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setColor(Bot.config.get(member.getGuild()).getSlashCommand().getSuccessColor())
				.setTitle("Submission accepted")
				.addField("Accepted by", acceptedBy.getAsMention(), true)
				.addField("Thread", channel.getAsMention(), false)
				.build();
	}
}
