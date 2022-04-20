package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.systems.help.model.HelpAccount;

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
	 * Gets as many Accounts as specified.
	 *
	 * @param page    The page.
	 * @param size    The amount of account to return.
	 * @return A {@link List} containing the specified amount of {@link HelpAccount}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<HelpAccount> getAccountsWithRank(int page, int size) throws SQLException {
		String sql = "SELECT * FROM help_account WHERE experience > 0 ORDER BY experience DESC LIMIT %d OFFSET %d";
		PreparedStatement stmt = con.prepareStatement(String.format(sql, size, (page * size) - size));
		ResultSet rs = stmt.executeQuery();
		List<HelpAccount> accounts = new ArrayList<>(size);
		while (rs.next()) {
			accounts.add(this.read(rs));
		}
		stmt.close();
		return accounts;
	}

	/**
	 * Gets the total amount of {@link HelpAccount}s stored in the database, that have more than 0 experience.
	 *
	 * @return The amount, as an {@link Integer}.
	 * @throws SQLException If an error occurs.
	 */
	public int getTotalRankedAccounts() throws SQLException {
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
	 * @throws SQLException If an error occurs.
	 */
	public void removeExperienceFromAllAccounts(double change) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("UPDATE help_account SET experience = GREATEST(experience - ?, 0)")) {
			s.setDouble(1, change);
			long rows = s.executeLargeUpdate();
			log.info("Removed {} experience from all Help Accounts. {} rows affected.", change, rows);
		}
	}

	private HelpAccount read(ResultSet rs) throws SQLException {
		HelpAccount account = new HelpAccount();
		account.setUserId(rs.getLong("user_id"));
		account.setExperience(rs.getDouble("experience"));
		return account;
	}
}

