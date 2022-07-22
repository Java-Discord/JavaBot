package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.util.Responses;

/**
 * Allows staff members to get more detailed information about the message cache.
 */
public class MessageCacheInfoSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public MessageCacheInfoSubcommand() {
		setSubcommandData(new SubcommandData("info", "Displays some info about the Message Cache."));
		requireUsers(Bot.config.getSystems().getAdminConfig().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildInfoEmbed(Bot.config.get(event.getGuild()), event.getUser())).queue();
	}

	private MessageEmbed buildInfoEmbed(GuildConfig config, User author) {
		long messages = DbActions.count("SELECT count(*) FROM message_cache");
		int maxMessages = config.getMessageCache().getMaxCachedMessages();
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Cache Info")
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Table Size", DbActions.getLogicalSize("message_cache") + " bytes", false)
				.addField("Message Count", String.valueOf(Bot.messageCache.messageCount), true)
				.addField("Cached (Memory)", String.format("%s/%s (%.2f%%)", Bot.messageCache.cache.size(), maxMessages, ((float) Bot.messageCache.cache.size() / maxMessages) * 100), true)
				.addField("Cached (Database)", String.format("%s/%s (%.2f%%)", messages, maxMessages, ((float) messages / maxMessages) * 100), true)
				.build();
	}
}
