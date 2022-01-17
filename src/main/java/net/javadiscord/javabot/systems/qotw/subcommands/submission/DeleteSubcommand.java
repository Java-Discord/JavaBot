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

@Slf4j
public class DeleteSubcommand implements SlashCommandHandler {
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
			return Responses.error(event, "Cannot delete a submission of a user who is not a member of this server");
		}
		var embed = buildDeleteSubmissionEmbed(member, event.getMember(), thread);
		thread.delete().queue();
		config.getModeration().getLogChannel().sendMessageEmbeds(embed).queue();
		log.info("Submission by User {} was deleted by {}", member.getUser().getAsTag(), event.getUser().getAsTag());
		return Responses.success(event, "Submission deleted", String.format("Successfully deleted %s's submission.", member.getAsMention()));
	}

	private MessageEmbed buildDeleteSubmissionEmbed(Member member, Member deletedBy, ThreadChannel channel) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setTitle("Submission deleted")
				.addField("Deleted by", deletedBy.getAsMention(), true)
				.addField("Thread", channel.getAsMention(), false)
				.build();
	}
}
