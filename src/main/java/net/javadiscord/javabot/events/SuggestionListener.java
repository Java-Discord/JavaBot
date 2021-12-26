package net.javadiscord.javabot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class SuggestionListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.getMessage().getType() == MessageType.THREAD_CREATED) return;
		if (!event.getChannel().equals(Bot.config.get(event.getGuild()).getModeration().getSuggestionChannel())) return;

		var config = Bot.config.get(event.getGuild());
		var eb = new EmbedBuilder()
				.setColor(config.getSlashCommand().getDefaultColor())
				.setImage(null)
				.setAuthor(event.getAuthor().getAsTag() + " · Suggestion", null, event.getAuthor().getEffectiveAvatarUrl())
				.setTimestamp(Instant.now())
				.setDescription(event.getMessage().getContentRaw())
				.build();

		if (!event.getMessage().getAttachments().isEmpty()) {
			Message.Attachment attachment = event.getMessage().getAttachments().get(0);
			try {
				event.getChannel().sendFile(attachment.retrieveInputStream().get(), "attachment." + attachment.getFileExtension()).setEmbeds(eb).queue(message -> {
					message.addReaction(config.getEmote().getUpvoteEmote()).queue();
					message.addReaction(config.getEmote().getDownvoteEmote()).queue();
				});
			} catch (Exception e) { event.getChannel().sendMessage(e.getMessage()).queue(); }
		} else {
			event.getChannel().sendMessageEmbeds(eb).queue(message -> {
				message.addReaction(config.getEmote().getUpvoteEmote()).queue();
				message.addReaction(config.getEmote().getDownvoteEmote()).queue();
			});
		}
		event.getMessage().delete().queue();
	}
}
