package net.javadiscord.javabot.systems.starboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.data.config.guild.StarboardConfig;
import net.javadiscord.javabot.systems.starboard.dao.StarboardRepository;
import net.javadiscord.javabot.systems.starboard.model.StarboardEntry;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Handles & manages all starboard interactions.
 */
@Slf4j
@RequiredArgsConstructor
public class StarboardManager extends ListenerAdapter {
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final StarboardRepository starboardRepository;

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		if (isInvalidUser(event.getUser())) return;
		if (isInvalidChannel(event.getChannel())) return;
		handleReactionEvent(event.getGuild(), event.getEmoji(), event.getChannel(), event.getMessageIdLong());
	}

	@Override
	public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
		if (isInvalidUser(event.getUser())) return;
		if (isInvalidChannel(event.getGuildChannel())) return;
		handleReactionEvent(event.getGuild(), event.getEmoji(), event.getChannel(), event.getMessageIdLong());
	}

	private void handleReactionEvent(Guild guild, Emoji emoji, MessageChannel channel, long messageId) {
		asyncPool.submit(() -> {
			StarboardConfig config = botConfig.get(guild).getStarboardConfig();
			if (config.getStarboardChannel().equals(channel)) return;
			Emoji starEmote = config.getEmojis().get(0);
			if (!emoji.equals(starEmote)) return;
			channel.retrieveMessageById(messageId).queue(
					message -> {
						int stars = getReactionCountForEmote(starEmote, message);
						asyncPool.execute(()->{
							try {
								starboardRepository
									.getEntryByMessageId(message.getIdLong())
									.ifPresentOrElse(entry->{
										updateStarboardMessage(message, stars, config);
									}, ()->{
										if (stars >= config.getReactionThreshold()) {
											addMessageToStarboard(message, stars, config);
										} else if (stars < 1 && !removeMessageFromStarboard(message.getIdLong(), channel, config)) {
											log.error("Could not remove Message from Starboard");
										}
									});
							} catch (DataAccessException e) {
								ExceptionLogger.capture(e, StarboardManager.class.getSimpleName());
							}
						});
					}, e -> log.error("Could not add Message to Starboard", e)
			);
		});
	}

	private boolean isInvalidChannel(@NotNull MessageChannel channel) {
		ChannelType type = channel.getType();
		return type != ChannelType.TEXT && type != ChannelType.GUILD_PUBLIC_THREAD;
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		if (isInvalidChannel(event.getChannel())) return;
		try {
			StarboardConfig config = botConfig.get(event.getGuild()).getStarboardConfig();
			Optional<StarboardEntry> entry;
			if (event.getChannel().getIdLong() == config.getStarboardChannelId()) {
				entry = starboardRepository.getEntryByStarboardMessageId(event.getMessageIdLong());
			} else {
				entry = starboardRepository.getEntryByMessageId(event.getMessageIdLong());
			}
			entry.ifPresent(e->{
				if (!removeMessageFromStarboard(e.getOriginalMessageId(), event.getChannel(), config)) {
					log.error("Could not remove Message from Starboard");
				}
			});

		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
	}

	private boolean isInvalidUser(User user) {
		return user == null || user.isBot() || user.isSystem();
	}

	/**
	 * Attemps to get the amount of reactions for the given emote.
	 *
	 * @param emoji   The emote.
	 * @param message The message.
	 * @return The amount of reactions.
	 */
	private int getReactionCountForEmote(Emoji emoji, @NotNull Message message) {
		return message.getReactions().stream()
				.filter(r -> r.getEmoji().equals(emoji))
				.findFirst()
				.map(MessageReaction::getCount)
				.orElse(0);
	}

	private void addMessageToStarboard(Message message, int stars, @NotNull StarboardConfig config) throws DataAccessException {
		if (stars < config.getReactionThreshold()) return;
		MessageEmbed embed = buildStarboardEmbed(message);
		MessageCreateAction action = config.getStarboardChannel()
				.sendMessage(String.format("%s %s", config.getEmojis().get(0), stars))
				.setActionRow(Button.link(message.getJumpUrl(), "Jump to Message"))
				.setEmbeds(embed);
		for (Message.Attachment a : message.getAttachments()) {
			try {
				action.addFiles(FileUpload.fromData(a.getProxy().download().get(), a.getFileName()));
			} catch (InterruptedException | ExecutionException e) {
				action.addContent("Could not add Attachment: " + a.getFileName());
			}
		}
		action.queue(starboardMessage -> {
				StarboardEntry entry = new StarboardEntry();
				entry.setOriginalMessageId(message.getIdLong());
				entry.setGuildId(message.getGuild().getIdLong());
				entry.setChannelId(message.getChannel().getIdLong());
				entry.setAuthorId(message.getAuthor().getIdLong());
				entry.setStarboardMessageId(starboardMessage.getIdLong());
				asyncPool.execute(()->{
					try {
						starboardRepository.insert(entry);
					} catch (DataAccessException e) {
						ExceptionLogger.capture(e, StarboardManager.class.getSimpleName());
					}
				});
			}, e -> log.error("Could not send Message to Starboard", e)
		);
	}

	private void updateStarboardMessage(@NotNull Message message, int stars, @NotNull StarboardConfig config) throws DataAccessException {
		Optional<StarboardEntry> starboardEntry = starboardRepository.getEntryByMessageId(message.getIdLong());
		starboardEntry.ifPresentOrElse(entry->{
			long starboardId = entry.getStarboardMessageId();
			config.getStarboardChannel().retrieveMessageById(starboardId).queue(
					starboardMessage -> {
						if (starboardMessage.getAuthor().getIdLong() != message.getJDA().getSelfUser().getIdLong()) {
							log.error("Could not update/edit Starboard entry ({}), as it was created by another account.", starboardEntry);
							return;
						}
						if (stars < 1) {
							try {
								if (!removeMessageFromStarboard(message.getIdLong(), message.getChannel(), config)) {
									log.error("Could not remove Message from Starboard");
								}
							} catch (DataAccessException e) {
								ExceptionLogger.capture(e, getClass().getSimpleName());
							}
						} else {
							UnicodeEmoji starEmote = config.getEmojis().get(0);
							if (stars > 10) starEmote = config.getEmojis().get(1);
							if (stars > 25) starEmote = config.getEmojis().get(2);
							starboardMessage.editMessage(
											String.format("%s %s | %s", starEmote, stars, message.getChannel().getAsMention()))
									.queue();
						}
					}, e -> {
						log.error("Could not retrieve original Message. Deleting corresponding Starboard Entry...");
						try {
							removeMessageFromStarboard(message.getIdLong(), message.getChannel(), config);
						} catch (DataAccessException ex) {
							ex.printStackTrace();
						}
					}
			);
		}, ()->log.error("updateStarboardMessage called but StarboardEntry was not found"));

	}

	private boolean removeMessageFromStarboard(long messageId, MessageChannel channel, StarboardConfig config) throws DataAccessException {
		Optional<StarboardEntry> entry = starboardRepository.getEntryByMessageId(messageId);
		if (entry.isEmpty()) return false;
		if (!channel.equals(config.getStarboardChannel())) {
			config.getStarboardChannel().retrieveMessageById(entry.get().getStarboardMessageId()).queue(
					starboardMessage -> starboardMessage.delete().queue(), ExceptionLogger::capture
			);
		}
		starboardRepository.delete(messageId);
		log.info("Removed Starboard Entry with message Id {}", messageId);
		return true;
	}

	private @NotNull MessageEmbed buildStarboardEmbed(@NotNull Message message) {
		User author = message.getAuthor();
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), message.getJumpUrl(), author.getEffectiveAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(message.getContentRaw())
				.setFooter("#" + message.getChannel().getName())
				.build();
	}
}
