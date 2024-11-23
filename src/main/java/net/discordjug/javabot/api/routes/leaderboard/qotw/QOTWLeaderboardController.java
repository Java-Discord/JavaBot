package net.discordjug.javabot.api.routes.leaderboard.qotw;

import com.github.benmanes.caffeine.cache.Caffeine;

import net.discordjug.javabot.api.exception.InvalidEntityIdException;
import net.discordjug.javabot.api.routes.CaffeineCache;
import net.discordjug.javabot.api.routes.leaderboard.qotw.model.QOTWUserData;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.discordjug.javabot.util.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

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
	private static final int PAGE_AMOUNT = 10;
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
			List<QOTWAccount> topAccounts = pointsService.getTopAccounts(PAGE_AMOUNT, page);
			members = topAccounts.stream()
					.map(account -> new Pair<>(account, guild.retrieveMemberById(account.getUserId()).complete()))
					.filter(pair -> pair.second() != null)
					.map(pair -> createAPIAccount(pair.first(), pair.second().getUser(), topAccounts, page))
					.toList();
			getCache().put(new Pair<>(guild.getIdLong(), page), members);
		}
		return new ResponseEntity<>(members, HttpStatus.OK);
	}

	private QOTWUserData createAPIAccount(QOTWAccount account, User user, List<QOTWAccount> topAccounts, int page) {
		return QOTWUserData.of(
				account,
				user,
				//this can be inaccurate for later pages with multiple users having the same score on the previous page
				//specifically, it counts all users on previous pages as strictly higher in the leaderboard
				pointsService.getQOTWRank(account.getUserId(), topAccounts)+(page-1)*PAGE_AMOUNT);
	}
}
