package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.help.model.HelpAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
		try (PreparedStatement s = con.prepareStatement("INSERT INTO help_account (user_id, experience, help_contributions) VALUES ( ?, ?, ? )")) {
			s.setLong(1, account.getUserId());
			s.setDouble(2, account.getExperience());
			s.setInt(3, account.getHelpContributions());
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
		try (PreparedStatement s = con.prepareStatement("UPDATE help_account SET experience = ?, help_contributions = ? WHERE user_id = ?")) {
			s.setDouble(1, account.getExperience());
			s.setInt(2, account.getHelpContributions());
			s.setLong(3, account.getUserId());
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
	 * Removes the specified amount of experience from all {@link HelpAccount}s.
	 *
	 * @param change The amount to subtract.
	 * @throws SQLException If an error occurs.
	 */
	public void removeExperienceFromAllAccounts(long change) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("UPDATE help_account SET experience = experience - ? WHERE experience > ?")) {
			s.setLong(1, change);
			s.setLong(2, change);
			long rows = s.executeLargeUpdate();
			log.info("Removed {} experience from all Help Accounts. {} rows affected.", change, rows);
		}
	}

	private HelpAccount read(ResultSet rs) throws SQLException {
		HelpAccount account = new HelpAccount();
		account.setUserId(rs.getLong("user_id"));
		account.setExperience(rs.getDouble("experience"));
		account.setHelpContributions(rs.getInt("help_contributions"));
		return account;
	}
}

