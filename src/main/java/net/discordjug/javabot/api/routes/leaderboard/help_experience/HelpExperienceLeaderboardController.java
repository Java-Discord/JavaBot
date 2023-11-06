package net.discordjug.javabot.api.routes.leaderboard.help_experience;

import com.github.benmanes.caffeine.cache.Caffeine;

import net.discordjug.javabot.api.exception.InvalidEntityIdException;
import net.discordjug.javabot.api.routes.CaffeineCache;
import net.discordjug.javabot.api.routes.leaderboard.help_experience.model.ExperienceUserData;
import net.discordjug.javabot.systems.help.HelpExperienceService;
import net.discordjug.javabot.util.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

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
 * Handles all GET-Requests on the guilds/{guild_id}/leaderboard/experience/ route.
 */
@RestController
public class HelpExperienceLeaderboardController extends CaffeineCache<Pair<Long, Integer>, List<ExperienceUserData>> {
	private static final int PAGE_AMOUNT = 8;
	private final JDA jda;
	private final HelpExperienceService helpExperienceService;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link JDA} instance to use.
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 */
	@Autowired
	public HelpExperienceLeaderboardController(final JDA jda, HelpExperienceService helpExperienceService) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
		this.helpExperienceService = helpExperienceService;
	}

	/**
	 * Serves the specified amount of users. Sorted by the
	 * amount of help experience.
	 *
	 * @param guildId     The guilds' id.
	 * @param page The page to get. Defaults to 1.
	 * @return The {@link ResponseEntity}.
	 */
	@GetMapping("guilds/{guild_id}/leaderboard/experience")
	public ResponseEntity<List<ExperienceUserData>> getHelpExperienceLeaderboard(
			@PathVariable("guild_id") long guildId,
			@RequestParam(value = "page", defaultValue = "1") int page
	) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			throw new InvalidEntityIdException(Guild.class, "You've provided an invalid guild id!");
		}
		List<ExperienceUserData> members = getCache().getIfPresent(new Pair<>(guild.getIdLong(), page));
		if (members == null || members.isEmpty()) {
			members = helpExperienceService.getTopAccounts(PAGE_AMOUNT, page).stream()
					.map(p -> ExperienceUserData.of(p, jda.retrieveUserById(p.getUserId()).complete()))
					.toList();
			getCache().put(new Pair<>(guild.getIdLong(), page), members);
		}
		return new ResponseEntity<>(members, HttpStatus.OK);
	}
}
