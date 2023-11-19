package net.discordjug.javabot.data.h2db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Interface to modify statements.
 */
@FunctionalInterface
public interface StatementModifier {
	void modify(PreparedStatement s) throws SQLException;
}
