package net.javadiscord.javabot.data.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.jetbrains.annotations.NotNull;

/**
 * Contains configuration settings for various systems which the bot uses, such
 * as databases or dependencies that have runtime properties.
 */
@Data
@Slf4j
public class SystemsConfig {

	/**
	 * The token used to create the JDA Discord bot instance.
	 */
	private String jdaBotToken = "";

	/**
	 * The Key used for the bing-search-api.
	 */
	private String azureSubscriptionKey = "";

	/**
	 * The DSN for the Sentry API.
	 */
	private String sentryDsn = "";

	/**
	 * The number of threads to allocate to the bot's general purpose async
	 * thread pool.
	 */
	private int asyncPoolSize = 4;


	/**
	 * The location of some sort of bash script that re-compiles a new version of the Bot.
	 * Example in <a href="https://github.com/Java-Discord/JavaBot/pull/330">Pull Request #330</a>.
	 */
	private String redeployScriptLocation = "";

	/**
	 * Configuration for the Hikari connection pool that's used for the bot's
	 * SQL data source.
	 */
	private HikariConfig hikariConfig = new HikariConfig();

	/**
	 * Configuration settings for certain commands which need an extra layer of
	 * security.
	 */
	private AdminConfig adminConfig = new AdminConfig();

	/**
	 * Configuration settings for all the different emojis the bot uses.
	 */
	private EmojiConfig emojiConfig = new EmojiConfig();

	/**
	 * Configuration settings for the Hikari connection pool.
	 */
	@Data
	public static class HikariConfig {
		private String jdbcUrl = "jdbc:h2:tcp://localhost:9122/./java_bot";
		private int maximumPoolSize = 5;
		private long leakDetectionThreshold = 10000;
	}

	/**
	 * Configuration settings for certain commands which need an extra layer of
	 * security.
	 */
	@Data
	public static class AdminConfig {
		/**
		 * An array of user-Ids only which can manage some of the bot's systems.
		 */
		private Long[] adminUsers = new Long[]{};
	}

	/**
	 * Configuration settings for all the different emojis the bot uses.
	 */
	@Data
	public static class EmojiConfig {
		private long failureId = 0;
		private long successId = 0;
		private long upvoteId = 0;
		private long downvoteId = 0;
		private String clockUnicode = "\uD83D\uDD57";
		private String jobChannelVoteUnicode = "\uD83D\uDDD1️";

		public Emoji getFailureEmote(JDA jda) {
			return getEmoji(jda, failureId, "\u274C");
		}

		public Emoji getSuccessEmote(JDA jda) {
			return getEmoji(jda, successId, "\u2714\uFE0F");
		}

		public Emoji getUpvoteEmote(JDA jda) {
			return getEmoji(jda, upvoteId, "\uD83D\uDC4D");
		}

		public Emoji getDownvoteEmote(JDA jda) {
			return getEmoji(jda, downvoteId, "\uD83D\uDC4E");
		}

		public Emoji getClockEmoji() {
			return Emoji.fromUnicode(clockUnicode);
		}

		public Emoji getJobChannelVoteEmoji() {
			return Emoji.fromUnicode(jobChannelVoteUnicode);
		}

		private @NotNull Emoji getEmoji(@NotNull JDA jda, long emoteId, String backup) {
			RichCustomEmoji emote = jda.getEmojiById(emoteId);
			if (emote != null) {
				return emote;
			}
			log.error("Could not find emote with id {}: using backup instead: {}", emoteId, backup);
			return Emoji.fromUnicode(backup);
		}
	}
}
