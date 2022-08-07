package net.javadiscord.javabot.api.routes.metrics;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.api.exception.InvalidEntityIdException;
import net.javadiscord.javabot.api.routes.CaffeineCache;
import net.javadiscord.javabot.api.routes.metrics.model.MetricsData;
import net.javadiscord.javabot.data.config.guild.MetricsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Handles all GET-Requests on the guilds/{guild_id}/metrics/ route.
 */
@Slf4j
@RestController
public class MetricsController extends CaffeineCache<Long, MetricsData> {
	private final JDA jda;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link Autowired} {@link JDA} instance to use.
	 */
	@Autowired
	public MetricsController(final JDA jda) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(15, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
	}

	/**
	 * Serves metrics for the specified guild.
	 *
	 * @param guildId The guilds' id.
	 * @return The {@link ResponseEntity}.
	 */
	@GetMapping("guilds/{guild_id}/metrics")
	public ResponseEntity<MetricsData> getMetrics(@PathVariable("guild_id") long guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			throw new InvalidEntityIdException(Guild.class, "You've provided an invalid guild id!");
		}
		MetricsData data = getCache().getIfPresent(guild.getIdLong());
		if (data == null) {
			data = new MetricsData();
			data.setMemberCount(guild.getMemberCount());
			data.setOnlineCount(guild.retrieveMetaData().complete().getApproximatePresences());
			MetricsConfig config = Bot.getConfig().get(guild).getMetricsConfig();
			data.setWeeklyMessages(config.getWeeklyMessages());
			data.setActiveMembers(config.getActiveMembers());
			getCache().put(guild.getIdLong(), data);
		}
		return new ResponseEntity<>(data, HttpStatus.OK);
	}
}
