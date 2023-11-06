package net.discordjug.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.HelpConfig;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.help.model.HelpAccount;
import net.discordjug.javabot.systems.help.model.HelpTransaction;
import net.discordjug.javabot.systems.user_commands.leaderboard.ExperienceLeaderboardSubcommand;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.ImageCache;
import net.discordjug.javabot.util.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class that handles Help Experience Transactions.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class HelpExperienceService {
	private final BotConfig botConfig;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * Creates a new Help Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object.
	 * @throws DataAccessException If an error occurs.
	 */
	@Transactional
	public HelpAccount getOrCreateAccount(long userId) throws DataAccessException {
		HelpAccount account;
		Optional<HelpAccount> optional = helpAccountRepository.getByUserId(userId);
		if (optional.isPresent()) {
			account = optional.get();
		} else {
			account = new HelpAccount();
			account.setUserId(userId);
			account.setExperience(0);
			helpAccountRepository.insert(account);
		}
		return account;
	}

	/**
	 * Returns the specified amount of {@link HelpAccount}s, sorted by their experience.
	 *
	 * @param amount The amount to retrieve.
	 * @param page   The page to get.
	 * @return A {@link List} of {@link HelpAccount}s.
	 */
	public List<HelpAccount> getTopAccounts(int amount, int page) {
		try {
			return helpAccountRepository.getAccounts(page, amount);
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return Collections.emptyList();
		}
	}

	/**
	 * Performs a single transaction.
	 *
	 * @param recipient The recipient's user id.
	 * @param value     The transaction's value.
	 * @param guild     The current guild.
	 * @param channelId The ID of the channel the transaction should be performed in
	 * @throws DataAccessException If an error occurs.
	 */
	@Transactional
	public void performTransaction(long recipient, double value, Guild guild, long channelId) throws DataAccessException {
		if (value == 0) {
			log.error("Cannot make zero-value transactions");
			return;
		}
		HelpTransaction transaction = new HelpTransaction();
		transaction.setRecipient(recipient);
		transaction.setWeight(value);
		transaction.setChannelId(channelId);
		HelpAccount account = getOrCreateAccount(recipient);
		account.updateExperience(value);
		helpAccountRepository.update(account);
		helpTransactionRepository.save(transaction);
		checkExperienceRoles(guild, account);
		log.info("Added {} help experience to {}'s help account", value, recipient);
		ImageCache.removeCachedImagesByKeyword(ExperienceLeaderboardSubcommand.CACHE_PREFIX);
	}

	private void checkExperienceRoles(@NotNull Guild guild, @NotNull HelpAccount account) {
		guild.retrieveMemberById(account.getUserId()).queue(member ->
				botConfig.get(guild).getHelpConfig().getExperienceRoles().forEach((key, value) -> {
					Pair<Role, Double> role = account.getCurrentExperienceGoal(botConfig, guild);
					if (role.first() == null) return;
					if (key.equals(role.first().getIdLong())) {
						guild.addRoleToMember(member, role.first()).queue();
					} else {
						Role remove = guild.getRoleById(key);
						if (remove != null) {
							guild.removeRoleFromMember(member, remove).queue();
						}
					}
				}), e -> {});
	}

	/**
	 * add XP to all helpers depending on the messages they sent.
	 *
	 * @param post The {@link ThreadChannel} post
	 * @param allowIfXPAlreadyGiven {@code true} if XP should be awarded if XP have already been awarded
	 */
	public void addMessageBasedHelpXP(ThreadChannel post, boolean allowIfXPAlreadyGiven) {
		HelpConfig config = botConfig.get(post.getGuild()).getHelpConfig();
		try {
			Map<Long, Double> experience = HelpManager.calculateExperience(HelpListener.HELP_POST_MESSAGES.get(post.getIdLong()), post.getOwnerIdLong(), config);
			for (Map.Entry<Long, Double> entry : experience.entrySet()) {
				if(entry.getValue()>0 && (allowIfXPAlreadyGiven||!helpTransactionRepository.existsTransactionWithRecipientInChannel(entry.getKey(), post.getIdLong()))) {
					performTransaction(entry.getKey(), entry.getValue(), config.getGuild(), post.getIdLong());
				}
			}
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getName());
		}
	}
}
