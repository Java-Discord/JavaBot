package net.javadiscord.javabot.events;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.data.config.guild.StarBoardConfig;
import net.javadiscord.javabot.data.mongodb.Database;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class StarboardListener extends ListenerAdapter {

	void addToSB(Guild guild, MessageChannel channel, Message message, int starCount) {
		String guildId = guild.getId();
		String channelId = channel.getId();
		String messageId = message.getId();

		Database db = new Database();
		var config = Bot.config.get(guild).getStarBoard();
		TextChannel sc = config.getChannel();

		EmbedBuilder eb = new EmbedBuilder()
				.setAuthor("Jump to message", message.getJumpUrl())
				.setFooter(message.getAuthor().getAsTag(), message.getAuthor().getEffectiveAvatarUrl())
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setDescription(message.getContentRaw());

		MessageAction msgAction = sc
				.sendMessage(Bot.config.get(guild).getStarBoard().getEmotes().get(0) + " " +
						starCount + " | " + message.getTextChannel().getAsMention())
				.setEmbeds(eb.build());

		if (!message.getAttachments().isEmpty()) {
			try {
				Message.Attachment attachment = message.getAttachments().get(0);
				msgAction.addFile(attachment.retrieveInputStream().get(),
						messageId + "." + attachment.getFileExtension());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		msgAction.queue(sbMsg -> {
			db.createStarboardDoc(guildId, channelId, messageId, sbMsg.getId());
			sbMsg.addReaction(config.getEmotes().get(0)).queue();
		});
	}

	void updateSB(Guild guild, String channelId, String messageId, int reactionCount, Message message) {
		Database db = new Database();
		String guildId = guild.getId();
		String sbcEmbedId = db.getStarboardChannelString(guildId, channelId, messageId, "starboard_embed");
		var config = Bot.config.get(guild).getStarBoard();
		if (sbcEmbedId == null) return;

		Bot.config.get(guild).getStarBoard().getChannel()
				.retrieveMessageById(sbcEmbedId).queue((sbMsg) -> {
					// Adding -1 to the UniqueStarCount since it should not count for the ⭐ of JavaBot.
						this.handleMessage(guild, channelId, messageId, this.getUniqueStarCounts(sbMsg, message, config)-1, sbMsg);
				}, failure -> this.removeMessageFromStarboard(guild, messageId));
	}

	private void handleMessage(Guild guild, String channelId, String messageId, int reactionCount, Message sbMsg) {
		var config = Bot.config.get(guild).getStarBoard();
		if (reactionCount > 0) {
			updateMessage(guild, channelId, reactionCount, sbMsg, config);
		} else {
			removeMessageFromStarboard(guild, messageId);
		}
	}

	private void updateMessage(Guild guild, String channelId, int reactionCount, Message sbMsg, StarBoardConfig config) {
		String starEmote = config.getEmotes().get(0);
		if (reactionCount > 10)
			starEmote = config.getEmotes().get(1);
		if (reactionCount > 25)
			starEmote = config.getEmotes().get(2);

		TextChannel tc = guild.getTextChannelById(channelId);
		sbMsg.editMessage(starEmote + " "
				+ reactionCount + " | " + tc.getAsMention()).queue();
	}

	void removeMessageFromStarboard(Guild guild, String messageId) {

		MongoCollection<Document> collection = StartupListener.mongoClient
				.getDatabase("other")
				.getCollection("starboard_messages");

		Document doc = collection.find(eq("message_id", messageId)).first();
		if (doc == null) return;
		String embedId = doc.getString("starboard_embed");
		Bot.config.get(guild).getStarBoard().getChannel()
				.deleteMessageById(embedId)
				.queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

		collection.deleteOne(doc);
	}

	public void updateAllSBM(Guild guild) {
		log.info("{}[{}]{} Updating Starboard Messages",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

		String guildId = guild.getId();
		MongoCollection<Document> collection = StartupListener.mongoClient
				.getDatabase("other")
				.getCollection("starboard_messages");
		Database db = new Database();
		try (MongoCursor<Document> cursor = collection.find(eq("guild_id", guildId)).iterator()) {
			while (cursor.hasNext()) {
				processStarboardMessage(guild, guildId, collection, db, cursor.next());
			}
		}
	}

	private void processStarboardMessage(Guild guild, String guildId, MongoCollection<Document> collection, Database db, Document doc) {
		String channelId = doc.getString("channel_id");
		String messageId = doc.getString("message_id");
		TextChannel channel = guild.getTextChannelById(channelId);
		if (channel == null) return;
		var config = Bot.config.get(guild).getStarBoard();

		channel.retrieveMessageById(messageId).queue(msg -> {
			updateIfEmpty(guild, channelId, messageId, msg);
			int reactionCount = getReactionCountForEmote(config.getEmotes().get(0), msg);
			if (!db.isMessageOnStarboard(guildId, channelId, messageId) && reactionCount >= config.getReactionThreshold()) {
				addToSB(guild, channel, msg, reactionCount);
			} else if (db.getStarboardChannelString(guildId, channelId, messageId, "starboard_embed") != null) {
				updateSB(guild, channelId, messageId, reactionCount, msg);
			}
		}, failure -> collection.deleteOne(doc));
	}

	private int getReactionCountForEmote(String emote, Message msg) {
		return msg.getReactions().stream()
				.filter(r -> r.getReactionEmote().getName().equals(emote))
				.findFirst()
				.map(MessageReaction::getCount)
				.orElse(0);
	}

	private void updateIfEmpty(Guild guild, String channelId, String messageId, Message msg) {
		if (msg.getReactions().isEmpty()) updateSB(guild, channelId, messageId, 0, msg);
	}

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		if (event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem()) return;

		Database db = new Database();
		var config = Bot.config.get(event.getGuild()).getStarBoard();
		if (config.getEmotes().isEmpty()) {
			log.warn("No emotes have been configured for the starboard.");
			return;
		}
		if (!event.getReactionEmote().getName().equals(config.getEmotes().get(0))) return;

		String guildId = event.getGuild().getId();
		String channelId = event.getChannel().getId();
		String messageId = event.getMessageId();

		if (channelId.equals(config.getChannel().getId())) {
			this.updateSbReactionCounts(event, config, messageId, channelId);
		}

		if (!channelId.equals(config.getChannel().getId())) {
			event.getChannel().retrieveMessageById(messageId).queue(message -> {
				int reactionCount = getReactionCountForEmote(config.getEmotes().get(0), message);
				if (db.isMessageOnStarboard(guildId, channelId, messageId)) {
					updateSB(event.getGuild(), channelId, messageId, reactionCount, message);
				} else if (reactionCount >= config.getReactionThreshold()) {
					addToSB(event.getGuild(), event.getChannel(), message, reactionCount);
				}
			});
		}
	}

	@Override
	public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
		if (event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem()) return;

		Database db = new Database();
		var config = Bot.config.get(event.getGuild()).getStarBoard();

		if (!event.getReactionEmote().getName().equals(config.getEmotes().get(0))) return;

		String guildId = event.getGuild().getId();
		String channelId = event.getChannel().getId();
		String messageId = event.getMessageId();

		if (channelId.equals(config.getChannel().getId())) {
			this.updateSbReactionCounts(event, config, messageId, channelId);
		}

		if (!channelId.equals(config.getChannel().getId())) {

			event.getChannel().retrieveMessageById(messageId).queue(message -> {
				int reactionCount = getReactionCountForEmote(config.getEmotes().get(0), message);
				if (db.isMessageOnStarboard(guildId, channelId, messageId)) {
					updateSB(event.getGuild(), channelId, messageId, reactionCount, message);
				} else if (reactionCount >= config.getReactionThreshold()) {
					addToSB(event.getGuild(), event.getChannel(), message, reactionCount);
				} else {
					removeMessageFromStarboard(event.getGuild(), messageId);
				}
			});
		}
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		removeMessageFromStarboard(event.getGuild(), event.getMessageId());
	}

	private void updateSbReactionCounts(GenericMessageReactionEvent event, StarBoardConfig config, String messageId, String remoteTextChannelId) {
		Objects.requireNonNull(event.getGuild().getTextChannelById(remoteTextChannelId)).retrieveMessageById(messageId).queue(message -> {
			MessageEmbed me = message.getEmbeds().get(0);
			String prevChannelId = Objects.requireNonNull(me.getAuthor().getUrl()).split("/")[5];
			String originalMessageId = Objects.requireNonNull(me.getAuthor().getUrl()).split("/")[6];

			Objects.requireNonNull(event.getGuild().getTextChannelById(prevChannelId)).retrieveMessageById(originalMessageId).queue(msg -> {
				// Adding -1 to the UniqueStarCount since it should not count for the ⭐ of JavaBot.
					this.updateMessage(
							event.getGuild(),
							prevChannelId,
							this.getUniqueStarCounts(message, msg, config)-1,
							message, config);
			});
		});
	}

	private int getUniqueStarCounts(Message starboardMessage, Message originalMessage, StarBoardConfig config){
		try {
			List<User> originalMessageUsers = originalMessage.retrieveReactionUsers(config.getEmotes().get(0)).takeAsync(this.getReactionCountForEmote(config.getEmotes().get(0), originalMessage)).get();
			List<User> starboardMessageUsers = starboardMessage.retrieveReactionUsers(config.getEmotes().get(0)).takeAsync(this.getReactionCountForEmote(config.getEmotes().get(0), starboardMessage)).get();

			Stream<User> stream = Stream.concat(originalMessageUsers.stream(), starboardMessageUsers.stream()).distinct();

			return (int)stream.count();
		}catch(InterruptedException | ExecutionException e){ return 0; }
	}
}
