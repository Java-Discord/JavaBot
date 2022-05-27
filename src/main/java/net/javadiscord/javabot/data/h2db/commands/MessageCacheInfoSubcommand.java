package net.javadiscord.javabot.data.h2db.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
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
		int maxMessages = config.getMessageCache().getMaxCachedMessages();
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Cache Info")
				.setColor(config.getSlashCommand().getDefaultColor())
				.addField("Table Size", DbActions.getLogicalSize("message_cache") + " bytes", false)
				.addField("Message Count", String.valueOf(Bot.messageCache.messageCount), true)
				.addField("Cached (Memory)", String.format("%s/%s (%.2f%%)", Bot.messageCache.cache.size(), maxMessages, ((float) Bot.messageCache.cache.size() / maxMessages) * 100), true)
				.addField("Cached (Database)", String.format("%s/%s (%.2f%%)", messages, maxMessages, ((float) messages / maxMessages) * 100), true)
				.build();
	}
}
