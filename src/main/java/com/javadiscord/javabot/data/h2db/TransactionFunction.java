package com.javadiscord.javabot.data.h2db;

import java.sql.Connection;

public interface TransactionFunction {
	void execute(Connection c) throws Exception;
}
