package net.discordjug.javabot.data.h2db;

import java.sql.Connection;

/**
 * Interface that defines a function that executes using a transaction.
 * It is possible for an exception to be thrown, in which case the transaction
 * will attempt to roll back.
 */
public interface TransactionFunction {
	void execute(Connection c) throws Exception;
}
