package net.javadiscord.javabot.systems.economy.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.economy.model.Account;
import net.javadiscord.javabot.systems.economy.model.AccountPreferences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class AccountRepository {
	private final Connection con;

	public void saveNewAccount(Account account) throws SQLException {
		try (PreparedStatement stmt = this.con.prepareStatement("INSERT INTO economy_account (user_id, balance) VALUES (?, ?)")) {
			stmt.setLong(1, account.getUserId());
			stmt.setLong(2, account.getBalance());
			stmt.executeUpdate();
		}

		try (PreparedStatement stmt = this.con.prepareStatement("INSERT INTO economy_account_preferences (user_id) VALUES (?)")) {
			stmt.setLong(1, account.getUserId());
			stmt.executeUpdate();
		}
	}

	public void updateAccount(Account account) throws SQLException {
		try (PreparedStatement stmt = this.con.prepareStatement("UPDATE economy_account SET balance = ? WHERE user_id = ?")) {
			stmt.setLong(1, account.getBalance());
			stmt.setLong(2, account.getUserId());
			stmt.executeUpdate();
		}
	}

	public Account getAccount(long userId) throws SQLException {
		try (var stmt = this.con.prepareStatement("SELECT * FROM economy_account WHERE user_id = ?")) {
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return this.readAccount(rs);
			}
			return null;
		}
	}

	private Account readAccount(ResultSet rs) throws SQLException {
		Account account = new Account();
		account.setUserId(rs.getLong("user_id"));
		account.setBalance(rs.getLong("balance"));
		return account;
	}

	public AccountPreferences getPreferences(long userId) throws SQLException {
		try (var stmt = this.con.prepareStatement("SELECT * FROM economy_account_preferences WHERE user_id = ?")) {
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				AccountPreferences prefs = new AccountPreferences();
				prefs.setUserId(userId);
				prefs.setReceiveTransactionDms(rs.getBoolean("receive_transaction_dms"));
				return prefs;
			}
			return null;
		}
	}

	public void savePreferences(AccountPreferences prefs) throws SQLException {
		try (var stmt = this.con.prepareStatement("UPDATE economy_account_preferences SET receive_transaction_dms = ? WHERE user_id = ?")) {
			stmt.setBoolean(1, prefs.isReceiveTransactionDms());
			stmt.setLong(2, prefs.getUserId());
			stmt.executeUpdate();
		}
	}
}
