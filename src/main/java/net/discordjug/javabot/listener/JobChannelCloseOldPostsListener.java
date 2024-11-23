package net.discordjug.javabot.listener;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.util.InteractionUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Automatically closes older job posts when the same user creates a new one.
 */
@RequiredArgsConstructor
public class JobChannelCloseOldPostsListener extends ListenerAdapter {

	private final BotConfig botConfig;

	@Override
	public void onChannelCreate(ChannelCreateEvent event) {
		if (event.getChannel().getType() != ChannelType.GUILD_PUBLIC_THREAD) {
			return;
		}
		
		ThreadChannel post = event.getChannel().asThreadChannel();
		ModerationConfig moderationConfig = botConfig.get(event.getGuild()).getModerationConfig();
		long jobChannelId = moderationConfig.getJobChannelId();
		long projectChannelId = moderationConfig.getProjectChannelId();
		long parentChannelId = post.getParentChannel().getIdLong();
		
		if (parentChannelId != jobChannelId && parentChannelId != projectChannelId) {
			return;
		}

		List<ThreadChannel> threadChannels = post.getParentChannel()
				.getThreadChannels()
				.stream()
				.filter(c -> c.getOwnerIdLong() == post.getOwnerIdLong())
				.filter(otherPost -> otherPost.getIdLong() != post.getIdLong())
				.filter(c -> !c.isPinned())
				.collect(Collectors.toList());
		
		for (ThreadChannel otherPost : threadChannels) {
			if(otherPost.getTimeCreated().plusDays(7).isAfter(post.getTimeCreated())) {
				post.sendMessageEmbeds(
						new EmbedBuilder()
						.setTitle("Post closed")
						.setDescription("This post has been blocked because you have created other recent posts.\nPlease do not spam posts.")
						.build())
				.setContent(UserSnowflake.fromId(post.getOwnerIdLong()).getAsMention())
				.flatMap(msg -> post.getManager().setArchived(true).setLocked(true))
				.queue();
				return;
			}
		}
		
		if(parentChannelId == jobChannelId && !threadChannels.isEmpty()) {
			for (ThreadChannel otherPost : threadChannels) {
				otherPost.sendMessageEmbeds(
						new EmbedBuilder()
							.setTitle("Post closed")
							.setDescription("This post has been closed because the post owner created [another post](%s)."
									.formatted(post.getJumpUrl()))
							.build())
					.flatMap(msg -> otherPost.getManager().setArchived(true).setLocked(true))
					.queue();
			}
			post.sendMessageEmbeds(
					new EmbedBuilder()
					.setTitle("Posts closed")
					.setDescription("Since only one open post is allowed per user, older posts have been closed")
					.setColor(Color.YELLOW)
					.build())
				.addActionRow(InteractionUtils.createDeleteButton(post.getOwnerIdLong()))
				.queue();
		}
	}
}
