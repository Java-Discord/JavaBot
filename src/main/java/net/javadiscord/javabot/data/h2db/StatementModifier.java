package net.javadiscord.javabot.data.h2db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementModifier {
	void modify(PreparedStatement s) throws SQLException;
}
