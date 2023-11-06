package net.discordjug.javabot.api.routes.user_profile;

import com.github.benmanes.caffeine.cache.Caffeine;

import net.discordjug.javabot.api.exception.InternalServerException;
import net.discordjug.javabot.api.exception.InvalidEntityIdException;
import net.discordjug.javabot.api.routes.CaffeineCache;
import net.discordjug.javabot.api.routes.user_profile.model.HelpAccountData;
import net.discordjug.javabot.api.routes.user_profile.model.UserProfileData;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.help.HelpExperienceService;
import net.discordjug.javabot.systems.help.model.HelpAccount;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.discordjug.javabot.util.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

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
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Handles all GET-Requests on the guilds/{guild_id}/users/{user_id}/ route.
 */
@RestController
public class UserProfileController extends CaffeineCache<Pair<Long, Long>, UserProfileData> {
	private final JDA jda;
	private final QOTWPointsService qotwPointsService;
	private final DataSource dataSource;
	private final BotConfig botConfig;
	private final HelpExperienceService helpExperienceService;
	private final WarnRepository warnRepository;

	/**
	 * The constructor of this class which initializes the {@link Caffeine} cache.
	 *
	 * @param jda The {@link Autowired} {@link JDA} instance to use.
	 * @param qotwPointsService The {@link QOTWPointsService}
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 * @param warnRepository DAO for interacting with the set of {@link Warn} objects.
	 */
	@Autowired
	public UserProfileController(final JDA jda, QOTWPointsService qotwPointsService, BotConfig botConfig, DataSource dataSource, HelpExperienceService helpExperienceService, WarnRepository warnRepository) {
		super(Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build()
		);
		this.jda = jda;
		this.qotwPointsService = qotwPointsService;
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
		User user;
		try{
			user = jda.retrieveUserById(userId).complete();
		}catch (ErrorResponseException e) {
			throw new InvalidEntityIdException(User.class, "Cannot fetch user: " + e.getMeaning());
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
				data.setHelpAccount(HelpAccountData.of(botConfig, helpAccount, guild));
				// User Warns
				LocalDateTime cutoff = LocalDateTime.now().minusDays(botConfig.get(guild).getModerationConfig().getWarnTimeoutDays());
				data.setWarns(warnRepository.getActiveWarnsByUserId(user.getIdLong(), cutoff));
				// Insert into cache
				getCache().put(new Pair<>(guild.getIdLong(), user.getIdLong()), data);
			}
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (DataAccessException|SQLException e) {
			throw new InternalServerException("An internal server error occurred.", e);
		}
	}
}
