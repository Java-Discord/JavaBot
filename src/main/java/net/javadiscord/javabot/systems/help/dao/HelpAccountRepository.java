package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the HELP_ACCOUNT SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
public class HelpAccountRepository {
	private final Connection con;
	private final BotConfig botConfig;

	/**
	 * Inserts a new {@link HelpAccount}.
	 *
	 * @param account The account that should be inserted.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(HelpAccount account) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("INSERT INTO help_account (user_id, experience) VALUES ( ?, ? )")) {
			s.setLong(1, account.getUserId());
			s.setDouble(2, account.getExperience());
			s.executeUpdate();
			log.info("Inserted new Help Account: {}", account);
		}
	}

	/**
	 * Updates a single {@link HelpAccount}.
	 *
	 * @param account The account that should be updated.
	 * @throws SQLException If an error occurs.
	 */
	public void update(HelpAccount account) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("UPDATE help_account SET experience = ? WHERE user_id = ?")) {
			s.setDouble(1, account.getExperience());
			s.setLong(2, account.getUserId());
			s.executeUpdate();
		}
	}

	/**
	 * Tries to retrieve a {@link HelpAccount}, based on the given id.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object, as an {@link Optional}.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<HelpAccount> getByUserId(long userId) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM help_account WHERE user_id = ?")) {
			s.setLong(1, userId);
			ResultSet rs = s.executeQuery();
			HelpAccount account = null;
			if (rs.next()) {
				account = this.read(rs);
			}
			return Optional.ofNullable(account);
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
	public List<HelpAccount> getAccounts(int page, int size) throws SQLException {
		String sql = "SELECT * FROM help_account WHERE experience > 0 ORDER BY experience DESC LIMIT %d OFFSET %d";
		try (PreparedStatement stmt = con.prepareStatement(String.format(sql, size, Math.max(0, (page * size) - size)))) {
			ResultSet rs = stmt.executeQuery();
			List<HelpAccount> accounts = new ArrayList<>(size);
			while (rs.next()) {
				accounts.add(this.read(rs));
			}
			return accounts;
		}
	}

	/**
	 * Gets the total amount of {@link HelpAccount}s stored in the database, that have more than 0 experience.
	 *
	 * @return The amount, as an {@link Integer}.
	 * @throws SQLException If an error occurs.
	 */
	public int getTotalAccounts() throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT COUNT(*) FROM help_account WHERE experience > 0")) {
			ResultSet rs = s.executeQuery();
			if (rs.next()) return rs.getInt(1);
			return 0;
		}
	}

	/**
	 * Removes the specified amount of experience from all {@link HelpAccount}s.
	 *
	 * @param change The amount to subtract.
	 * @param min The minimum amount to subtract.
	 * @param max The maximum amount to subtract.
	 * @throws SQLException If an error occurs.
	 */
	public void removeExperienceFromAllAccounts(double change, int min, int max) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("UPDATE help_account SET experience = GREATEST(experience - LEAST(GREATEST(experience * (1 - ? / 100), ?), ?), 0)")) {
			s.setDouble(1, change);
			s.setInt(2, min);
			s.setInt(3, max);
			long rows = s.executeLargeUpdate();
			log.info("Removed {} experience from all Help Accounts. {} rows affected.", change, rows);
		}
	}

	private @NotNull HelpAccount read(@NotNull ResultSet rs) throws SQLException {
		HelpAccount account = new HelpAccount(botConfig);
		account.setUserId(rs.getLong("user_id"));
		account.setExperience(rs.getDouble("experience"));
		return account;
	}
}

