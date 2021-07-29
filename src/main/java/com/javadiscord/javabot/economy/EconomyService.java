package com.javadiscord.javabot.economy;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.economy.dao.AccountRepository;
import com.javadiscord.javabot.economy.dao.TransactionRepository;
import com.javadiscord.javabot.economy.model.Account;
import com.javadiscord.javabot.economy.model.Transaction;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public class EconomyService {

	public Transaction performTransaction(Long fromUserId, Long toUserId, long value) throws SQLException {
		Transaction t = new Transaction();
		t.setFromUserId(fromUserId);
		t.setToUserId(toUserId);
		t.setValue(value);
		if (value == 0) throw new IllegalArgumentException("Cannot create zero-value transaction.");
		Connection con = Bot.dataSource.getConnection();
		con.setAutoCommit(false);
		TransactionRepository transactionRepository = new TransactionRepository(con);
		AccountRepository accountRepository = new AccountRepository(con);
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
