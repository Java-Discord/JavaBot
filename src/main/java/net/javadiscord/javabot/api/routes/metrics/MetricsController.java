package net.javadiscord.javabot.api.routes.metrics;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.api.response.ApiResponseBuilder;
import net.javadiscord.javabot.api.response.ApiResponses;
import net.javadiscord.javabot.api.routes.JDAEntity;
import net.javadiscord.javabot.api.routes.metrics.model.MetricsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles all GET-Requests on the {guild_id}/metrics route.
 */
@Slf4j
@RestController
public class MetricsController implements JDAEntity {

	/**
	 * Serves metrics for the specified guild.
	 *
	 * @param guildId The guilds' id.
	 * @return The {@link ResponseEntity}.
	 */
	@GetMapping(
			value = "{guild_id}/metrics",
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<String> getMetrics(@PathVariable(value = "guild_id") String guildId) {
		Guild guild = getJDA().getGuildById(guildId);
		if (guild == null) {
			return new ResponseEntity<>(ApiResponses.INVALID_GUILD_IN_REQUEST, HttpStatus.BAD_REQUEST);
		}
		MetricsData data = new MetricsData();
		data.setMemberCount(guild.getMemberCount());
		data.setOnlineCount(guild.retrieveMetaData().complete().getApproximatePresences());
		data.setWeeklyMessages(Bot.getConfig().get(guild).getMetricsConfig().getWeeklyMessages());
		return new ResponseEntity<>(new ApiResponseBuilder().add("metrics", data).build(), HttpStatus.OK);
	}
}
