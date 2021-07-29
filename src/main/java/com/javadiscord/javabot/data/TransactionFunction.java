package com.javadiscord.javabot.data;

import java.sql.Connection;

public interface TransactionFunction {
	void execute(Connection c) throws Exception;
}
