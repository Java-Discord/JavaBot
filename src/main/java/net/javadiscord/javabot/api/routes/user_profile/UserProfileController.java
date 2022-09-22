package net.javadiscord.javabot.api.routes.user_profile;

import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.api.exception.InternalServerException;
import net.javadiscord.javabot.api.exception.InvalidEntityIdException;
import net.javadiscord.javabot.api.routes.CaffeineCache;
import net.javadiscord.javabot.api.routes.user_profile.model.HelpAccountData;
import net.javadiscord.javabot.api.routes.user_profile.model.UserProfileData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;
import net.javadiscord.javabot.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Handles all GET-Requests on the guilds/{guild_id}/users/{user_id}/ route.
 */
@RestController
public class UserProfileController extends CaffeineCache<Pair<Long, Long>, UserProfileData> {
	private final JDA jda;
	private final QOTWPointsService qotwPointsService;
	private final UserPreferenceService preferenceService;
	private final DataSource dataSource;
	private final BotConfig botConfig;
	private final HelpExperienceService helpExperienceService;
	private final WarnRepository warnRepository;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link Autowired} {@link JDA} instance to use.
	 * @param qotwPointsService The {@link QOTWPointsService}
	 * @param preferenceService The {@link UserPreferenceService}
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 * @param warnRepository DAO for interacting with the set of {@link Warn} objects.
	 */
	@Autowired
	public UserProfileController(final JDA jda, QOTWPointsService qotwPointsService, UserPreferenceService preferenceService, BotConfig botConfig, DataSource dataSource, HelpExperienceService helpExperienceService, WarnRepository warnRepository) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
		this.qotwPointsService = qotwPointsService;
		this.preferenceService = preferenceService;
		this.dataSource = dataSource;
		this.botConfig = botConfig;
		this.helpExperienceService = helpExperienceService;
		this.warnRepository = warnRepository;
	}

	/**
	 * Serves a single users' profile in a specified guild.
	 *
	 * @param guildId The guilds' id.
	 * @param userId  The users' id.
	 * @return The {@link ResponseEntity} containing the {@link UserProfileData}.
	 */
	@GetMapping("guilds/{guild_id}/users/{user_id}")
	public ResponseEntity<UserProfileData> getUserProfile(
			@PathVariable("guild_id") long guildId,
			@PathVariable("user_id") long userId
	) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			throw new InvalidEntityIdException(Guild.class, "You've provided an invalid guild id!");
		}
		User user = jda.retrieveUserById(userId).complete();
		if (user == null) {
			throw new InvalidEntityIdException(User.class, "You've provided an invalid user id!");
		}
		try (Connection con = dataSource.getConnection()) {
			// Check Cache
			UserProfileData data = getCache().getIfPresent(new Pair<>(guild.getIdLong(), user.getIdLong()));
			if (data == null) {
				data = new UserProfileData();
				data.setUserId(user.getIdLong());
				data.setUserName(user.getName());
				data.setDiscriminator(user.getDiscriminator());
				data.setEffectiveAvatarUrl(user.getEffectiveAvatarUrl());
				// Question of the Week Account
				QOTWAccount qotwAccount = qotwPointsService.getOrCreateAccount(user.getIdLong());
				data.setQotwAccount(qotwAccount);
				// Help Account
				HelpAccount helpAccount = helpExperienceService.getOrCreateAccount(user.getIdLong());
				data.setHelpAccount(HelpAccountData.of(helpAccount, guild));
				// User Preferences
				List<UserPreference> preferences = Arrays.stream(Preference.values()).map(p -> preferenceService.getOrCreate(user.getIdLong(), p)).toList();
				data.setPreferences(preferences);
				// User Warns
				LocalDateTime cutoff = LocalDateTime.now().minusDays(botConfig.get(guild).getModerationConfig().getWarnTimeoutDays());
				data.setWarns(warnRepository.getWarnsByUserId(user.getIdLong(), cutoff));
				// Insert into cache
				getCache().put(new Pair<>(guild.getIdLong(), user.getIdLong()), data);
			}
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (DataAccessException|SQLException e) {
			throw new InternalServerException("An internal server error occurred.", e);
		}
	}
}
