package net.javadiscord.javabot.api.routes.leaderboard.qotw;

import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.api.exception.InvalidEntityIdException;
import net.javadiscord.javabot.api.routes.CaffeineCache;
import net.javadiscord.javabot.api.routes.leaderboard.qotw.model.QOTWUserData;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles all GET-Requests on the guilds/{guild_id}/leaderboard/qotw/ route.
 */
@RestController
public class QOTWLeaderboardController extends CaffeineCache<Pair<Long, Integer>, List<QOTWUserData>> {
	private static final int PAGE_AMOUNT = 8;
	private final JDA jda;
	private final QOTWPointsService pointsService;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link JDA} instance to use.
	 * @param pointsService The {@link QOTWPointsService}
	 */
	@Autowired
	public QOTWLeaderboardController(final JDA jda, QOTWPointsService pointsService) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
		this.pointsService = pointsService;
	}

	/**
	 * Serves the specified amount of users. Sorted by the
	 * amount of qotw-points.
	 *
	 * @param guildId     The guilds' id.
	 * @param page The page to get. Defaults to 1.
	 * @return The {@link ResponseEntity}.
	 */
	@GetMapping("guilds/{guild_id}/leaderboard/qotw")
	public ResponseEntity<List<QOTWUserData>> getQOTWLeaderboard(
			@PathVariable("guild_id") long guildId,
			@RequestParam(value = "page", defaultValue = "1") int page
	) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			throw new InvalidEntityIdException(Guild.class, "You've provided an invalid guild id!");
		}
		List<QOTWUserData> members = getCache().getIfPresent(new Pair<>(guild.getIdLong(), page));
		if (members == null || members.isEmpty()) {
			members = pointsService.getTopAccounts(PAGE_AMOUNT, page).stream()
					.map(p -> QOTWUserData.of(p, jda.retrieveUserById(p.getUserId()).complete()))
					.toList();
			getCache().put(new Pair<>(guild.getIdLong(), page), members);
		}
		return new ResponseEntity<>(members, HttpStatus.OK);
	}
}
