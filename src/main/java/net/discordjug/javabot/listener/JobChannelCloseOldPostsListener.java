package net.discordjug.javabot.listener;

import java.awt.Color;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.InteractionUtils;
import net.dv8tion.jda.api.EmbedBuilder;
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
		if (post.getParentChannel().getIdLong() !=
				botConfig.get(event.getGuild()).getModerationConfig().getJobChannelId()) {
			return;
		}


		boolean postClosed = false;

		for (ThreadChannel otherPost : post.getParentChannel().getThreadChannels()) {
			if (otherPost.getOwnerIdLong() == post.getOwnerIdLong() &&
					otherPost.getIdLong() != post.getIdLong() &&
					!otherPost.isPinned()) {
				otherPost.sendMessageEmbeds(
						new EmbedBuilder()
							.setTitle("Post closed")
							.setDescription("This post has been closed because the post owner created [another post](%s)."
									.formatted(post.getJumpUrl()))
							.build())
					.flatMap(msg -> otherPost.getManager().setArchived(true).setLocked(true))
					.queue();
				postClosed = true;
			}
		}
		if (postClosed) {
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
