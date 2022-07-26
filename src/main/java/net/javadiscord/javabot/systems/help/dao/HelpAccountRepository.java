package net.javadiscord.javabot.systems.help.dao;

import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.h2db.DatabaseRepository;
import net.javadiscord.javabot.data.h2db.TableProperty;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import org.h2.api.H2Type;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the HELP_ACCOUNT SQL Table.
 */
@Slf4j
public class HelpAccountRepository extends DatabaseRepository<HelpAccount> {

	/**
	 * The constructor of this {@link DatabaseRepository} class which defines all important information
	 * about the HELP_ACCOUNT database table.
	 *
	 * @param con The {@link Connection} to use.
	 */
	public HelpAccountRepository(Connection con) {
		super(con, HelpAccount.class, "HELP_ACCOUNT", List.of(
				TableProperty.of("user_id", H2Type.BIGINT, (x, y) -> x.setUserId((Long) y), HelpAccount::getUserId),
				TableProperty.of("experience", H2Type.DOUBLE_PRECISION, (x, y) -> x.setExperience((Double) y), HelpAccount::getExperience)
		));
	}

	/**
	 * Updates a single {@link HelpAccount}.
	 *
	 * @param account The account that should be updated.
	 * @return Whether the update was successful.
	 * @throws SQLException If an error occurs.
	 */
	public boolean update(@NotNull HelpAccount account) throws SQLException {
		return update("UPDATE help_account SET experience = ? WHERE user_id = ?", account.getExperience(), account.getUserId()) > 0;
	}

	/**
	 * Tries to retrieve a {@link HelpAccount}, based on the given id.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object, as an {@link Optional}.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<HelpAccount> getByUserId(long userId) throws SQLException {
		return querySingle("WHERE user_id = ?", userId);
	}

	/**
	 * Gets a specified amount of {@link HelpAccount}s.
	 *
	 * @param page The page.
	 * @param size The amount of {@link HelpAccount}s to return.
	 * @return A {@link List} containing the specified amount of {@link HelpAccount}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<HelpAccount> getAccounts(long page, long size) throws SQLException {
		return queryMultiple(String.format("SELECT * FROM help_account WHERE experience > 0 ORDER BY experience DESC LIMIT %d OFFSET %d",
				size, Math.max(0, (page * size) - size))
		);
	}

	/**
	 * Gets the total amount of {@link HelpAccount}s stored in the database, that have more than 0 experience.
	 *
	 * @return The amount, as a {@link Long}.
	 */
	public long countAccounts() {
		return count("SELECT COUNT(*) FROM help_account WHERE experience > 0");
	}

	/**
	 * Removes the specified amount of experience from all {@link HelpAccount}s.
	 *
	 * @param change The amount to subtract.
	 * @param min    The minimum amount to subtract.
	 * @param max    The maximum amount to subtract.
	 * @return The amount of affected rows.
	 * @throws SQLException If an error occurs.
	 */
	public int removeExperienceFromAllAccounts(double change, int min, int max) throws SQLException {
		return update("UPDATE help_account SET experience = GREATEST(experience - LEAST(GREATEST(experience * (1 - ? / 100), ?), ?), 0)", change, min, max);
	}
}

