package com.javadiscord.javabot.economy;

import com.javadiscord.javabot.data.H2DataSource;
import com.javadiscord.javabot.economy.dao.AccountRepository;
import com.javadiscord.javabot.economy.dao.TransactionRepository;
import com.javadiscord.javabot.economy.model.Account;
import com.javadiscord.javabot.economy.model.Transaction;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class EconomyService {
	private final H2DataSource dataSource;

	public Account getOrCreateAccount(long userId) throws SQLException {
		Connection con = this.dataSource.getConnection();
		con.setAutoCommit(false);
		AccountRepository accountRepository = new AccountRepository(con);
		Account account = accountRepository.getAccount(userId);
		if (account == null) {
			account = new Account();
			account.setUserId(userId);
			account.setBalance(0);
			accountRepository.saveNewAccount(account);
		}
		con.commit();
		con.close();
		return account;
	}

	public List<Transaction> getRecentTransactions(long userId, int count) throws SQLException {
		Connection con = this.dataSource.getConnection();
		con.setReadOnly(true);
		var transactions = new TransactionRepository(con).getLatestTransactions(userId, count);
		con.close();
		return transactions;
	}

	public Transaction performTransaction(Long fromUserId, Long toUserId, long value, String message) throws SQLException {
		if (value == 0) throw new IllegalArgumentException("Cannot create zero-value transaction.");
		if (Objects.equals(fromUserId, toUserId)) throw new IllegalArgumentException("Sender and recipient cannot be the same.");

		Transaction t = new Transaction();
		t.setFromUserId(fromUserId);
		t.setToUserId(toUserId);
		t.setValue(value);
		t.setMessage(message);

		Connection con = this.dataSource.getConnection();
		con.setAutoCommit(false);
		TransactionRepository transactionRepository = new TransactionRepository(con);
		AccountRepository accountRepository = new AccountRepository(con);
		// Deduct the amount from the sender's account balance.
		if (fromUserId != null) {
			Account account = accountRepository.getAccount(fromUserId);
			if (account == null) {
				account = new Account();
				account.setUserId(fromUserId);
				account.setBalance(0);
				accountRepository.saveNewAccount(account);
			}
			if (account.getBalance() < value) throw new IllegalStateException("Sender account does not have the required funds.");
			account.updateBalance(-value);
			accountRepository.updateAccount(account);
		}
		// Add the amount to the receiver's account balance.
		if (toUserId != null) {
			Account account = accountRepository.getAccount(toUserId);
			if (account == null) {
				account = new Account();
				account.setUserId(toUserId);
				account.setBalance(0);
				accountRepository.saveNewAccount(account);
			}
			account.updateBalance(value);
			accountRepository.updateAccount(account);
		}
		transactionRepository.saveNewTransaction(t);
		con.commit();
		Transaction transaction = transactionRepository.getTransaction(t.getId());
		con.close();
		return transaction;
	}
}
