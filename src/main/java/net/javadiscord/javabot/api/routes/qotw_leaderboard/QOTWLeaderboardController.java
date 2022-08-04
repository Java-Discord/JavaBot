package net.javadiscord.javabot.api.routes.qotw_leaderboard;

import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.api.response.ApiResponseBuilder;
import net.javadiscord.javabot.api.response.ApiResponses;
import net.javadiscord.javabot.api.routes.CaffeineCache;
import net.javadiscord.javabot.api.routes.qotw_leaderboard.model.QOTWMemberData;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.Checks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles all GET-Requests on the {guild_id}/qotw/leaderboard route.
 */
@RestController
public class QOTWLeaderboardController extends CaffeineCache<Long, List<QOTWMemberData>> {
	private final JDA jda;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link JDA} instance to use.
	 */
	@Autowired
	public QOTWLeaderboardController(final JDA jda) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
	}

	/**
	 * Serves the specified amount of users. Sorted by the
	 * amount of qotw-points.
	 *
	 * @param guildId     The guilds' id.
	 * @param amountParam The amount of users to return. Defaults to 3.
	 * @return The {@link ResponseEntity}.
	 */
	@GetMapping(
			value = "{guild_id}/qotw/leaderboard",
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<String> getQOTWLeaderboard(
			@PathVariable(value = "guild_id") String guildId,
			@RequestParam(value = "amount", defaultValue = "3") String amountParam
	) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return new ResponseEntity<>(ApiResponses.INVALID_GUILD_IN_REQUEST, HttpStatus.BAD_REQUEST);
		}
		if (!Checks.checkInteger(amountParam)) {
			return new ResponseEntity<>(ApiResponses.INVALID_NUMBER_IN_REQUEST, HttpStatus.BAD_REQUEST);
		}
		int amount = Integer.parseInt(amountParam);
		QOTWPointsService service = new QOTWPointsService(Bot.getDataSource());
		List<QOTWMemberData> members = getCache().getIfPresent(guild.getIdLong());
		if (members == null || members.isEmpty()) {
			members = service.getTopMembers(amount, guild).stream()
					.map(p -> {
						QOTWMemberData data = new QOTWMemberData();
						data.setUserId(p.second().getIdLong());
						data.setUserName(p.second().getUser().getName());
						data.setDiscriminator(p.second().getUser().getDiscriminator());
						data.setEffectiveAvatarUrl(p.second().getEffectiveAvatarUrl());
						data.setAccount(p.first());
						return data;
					})
					.toList();
			getCache().put(guild.getIdLong(), members);
		}
		return new ResponseEntity<>(new ApiResponseBuilder().add("leaderboard", members.stream().limit(amount).toList()).build(), HttpStatus.OK);
	}
}
