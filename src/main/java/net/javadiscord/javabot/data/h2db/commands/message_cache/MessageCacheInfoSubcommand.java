package net.javadiscord.javabot.data.h2db.commands.message_cache;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbActions;

/**
 * Allows staff members to get more detailed information about the message cache.
 */
public class MessageCacheInfoSubcommand implements SlashCommand {
	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		return event.replyEmbeds(this.buildInfoEmbed(Bot.config.get(event.getGuild()), event.getUser()));
	}

	private MessageEmbed buildInfoEmbed(GuildConfig config, User author) {
		long messages = DbActions.count("SELECT count(*) FROM message_cache");
		int maxDatabaseMessages = config.getMessageCache().getMaxCachedMessages();
		int maxMemoryMessages = config.getMessageCache().getMessageSynchronizationInterval();
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Cache Info")
				.setColor(config.getSlashCommand().getDefaultColor())
				.addField("Table Size", DbActions.getLogicalSize("message_cache") + " bytes", true)
				.addField("Cached (Memory)", String.format("%s/%s (%.2f%%)", Bot.messageCache.cache.size(), maxMemoryMessages, ((float) Bot.messageCache.cache.size() / maxMemoryMessages) * 100), false)
				.addField("Cached (Database)", String.format("%s/%s (%.2f%%)", messages, maxDatabaseMessages, ((float) messages / maxDatabaseMessages) * 100), true)
				.build();
	}
}
