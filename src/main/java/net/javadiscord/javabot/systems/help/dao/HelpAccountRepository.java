package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the HELP_ACCOUNT SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class HelpAccountRepository {
	private final JdbcTemplate jdbcTemplate;
	private final BotConfig botConfig;

	/**
	 * Inserts a new {@link HelpAccount}.
	 *
	 * @param account The account that should be inserted.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(HelpAccount account) throws DataAccessException {

		jdbcTemplate.update("INSERT INTO help_account (user_id, experience) VALUES ( ?, ? )",
				account.getUserId(),
				account.getExperience());
		log.info("Inserted new Help Account: {}", account);
	}

	/**
	 * Updates a single {@link HelpAccount}.
	 *
	 * @param account The account that should be updated.
	 * @throws SQLException If an error occurs.
	 */
	public void update(HelpAccount account) throws DataAccessException {
		jdbcTemplate.update("UPDATE help_account SET experience = ? WHERE user_id = ?",
				account.getExperience(),
				account.getUserId());
	}

	/**
	 * Tries to retrieve a {@link HelpAccount}, based on the given id.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object, as an {@link Optional}.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<HelpAccount> getByUserId(long userId) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM help_account WHERE user_id = ?", (rs, row)->this.read(rs), userId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets a specified amount of {@link HelpAccount}s.
	 *
	 * @param page    The page.
	 * @param size    The amount of {@link HelpAccount}s to return.
	 * @return A {@link List} containing the specified amount of {@link HelpAccount}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<HelpAccount> getAccounts(int page, int size) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM help_account WHERE experience > 0 ORDER BY experience DESC LIMIT ? OFFSET ?", (rs, row)->this.read(rs),
				size, Math.max(0, (page * size) - size));
	}

	/**
	 * Gets the total amount of {@link HelpAccount}s stored in the database, that have more than 0 experience.
	 *
	 * @return The amount, as an {@link Integer}.
	 * @throws SQLException If an error occurs.
	 */
	public int getTotalAccounts() throws DataAccessException {
		try {
			return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM help_account WHERE experience > 0", (rs, row)->rs.getInt(1));
		}catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	/**
	 * Removes the specified amount of experience from all {@link HelpAccount}s.
	 *
	 * @param change The amount to subtract.
	 * @param min The minimum amount to subtract.
	 * @param max The maximum amount to subtract.
	 * @throws DataAccessException If an error occurs.
	 */
	public void removeExperienceFromAllAccounts(double change, int min, int max) throws DataAccessException {
		long rows = jdbcTemplate.execute("UPDATE help_account SET experience = GREATEST(experience - LEAST(GREATEST(experience * (? / 100), ?), ?), 0)",new CallableStatementCallback<Long>() {

			@Override
			public Long doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
				cs.setDouble(1, change);
				cs.setInt(2, min);
				cs.setInt(3, max);
				return cs.executeLargeUpdate();
			}
		});
		log.info("Removed {} experience from all Help Accounts. {} rows affected.", change, rows);
	}

	private @NotNull HelpAccount read(@NotNull ResultSet rs) throws SQLException {
		HelpAccount account = new HelpAccount();
		account.setUserId(rs.getLong("user_id"));
		account.setExperience(rs.getDouble("experience"));
		return account;
	}
}

