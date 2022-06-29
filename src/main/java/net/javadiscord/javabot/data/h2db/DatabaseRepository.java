package net.javadiscord.javabot.data.h2db;

import com.dynxsty.dih4jda.util.Checks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.api.H2Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a simple "Repository" for all database tables.
 * It provides some basic functions in order to properly interact with the corresponding table.
 *
 * @param <T> The model class for this table.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class DatabaseRepository<T> {
	private final Connection con;
	private final Class<T> modelClass;
	private final String tableName;
	private final List<TableProperty<T>> properties;

	/**
	 * Inserts a single instance of the specified model class into the database.
	 *
	 * @param instance The instance to insert.
	 * @throws SQLException If an error occurs.
	 */
	public final void insert(T instance) throws SQLException {
		List<TableProperty<T>> filteredProperties = this.properties.stream().filter(p -> !p.isExcludeFromInsertion()).toList();
		try (PreparedStatement stmt = con.prepareStatement(String.format("INSERT INTO %s (%s) VALUES (%s)",
				tableName, filteredProperties.stream().map(TableProperty::getPropertyName).collect(Collectors.joining(",")),
				",?".repeat(filteredProperties.size()).substring(1)
		))) {
			int index = 1;
			for (TableProperty<T> property : filteredProperties) {
				stmt.setObject(index, property.getFunction().apply(instance), property.getH2Type());
				index++;
			}
			if (stmt.executeUpdate() > 0) {
				log.info("Inserted {}: {}", modelClass.getSimpleName(), instance);
			} else {
				log.error("Could not insert {}: {}", modelClass.getSimpleName(), instance);
			}
		}
	}

	public final int update(String query, Object... args) throws SQLException {
		return DbActions.update(query, args);
	}

	/**
	 * Queries a single (the first) row which matches the given filter and converts it to the specified
	 * model class. Additionally, this value is then wrapped in an {@link Optional} as it is possible for the value
	 * to be empty.
	 *
	 * @param filter The filter for this query. Might look something like "WHEN age > 18".
	 * @param args Additional arguments used for formatting. "?" symbols are used as placeholders.
	 * @return An {@link Optional} which eventually holds the desired value.
	 * @throws SQLException If an error occurs.
	 */
	public final Optional<T> querySingle(String filter, Object @NotNull ... args) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(String.format("SELECT * FROM %s%s", tableName, filter.isEmpty() ? "" : " " + filter))) {
			int i = 1;
			for (Object arg : args) {
				stmt.setObject(i++, arg);
			}
			ResultSet rs = stmt.executeQuery();
			T t = null;
			if (rs.next()) {
				t = read(rs);
			}
			return Optional.ofNullable(t);
		}
	}

	/**
	 * Queries a multiple rows which match the given filter and converts them to the specified
	 * model class.
	 *
	 * @param filter The filter for this query. Might look something like "WHEN age > 18".
	 * @param args Additional arguments used for formatting. "?" symbols are used as placeholders.
	 * @return An unmodifiable {@link List} which holds the desired value(s).
	 * @throws SQLException If an error occurs.
	 */
	public final @NotNull List<T> queryMultiple(String filter, Object @NotNull ... args) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(String.format("SELECT * FROM %s%s", tableName, filter.isEmpty() ? "" : " " + filter))) {
			int i = 1;
			for (Object arg : args) {
				stmt.setObject(i++, arg);
			}
			ResultSet rs = stmt.executeQuery();
			List<T> list = new ArrayList<>();
			while (rs.next()) {
				System.out.println(list);
				list.add(read(rs));
			}
			return list;
		}
	}

	public final long count() {
		return DbActions.count("COUNT * FROM " + tableName);
	}

	public final long count(String query, Object... args) {
		return DbActions.count(String.format(query, args));
	}

	public final int getLogicalSize() {
		return DbActions.getLogicalSize(tableName);
	}

	/**
	 * Reads the given {@link ResultSet} and tries to convert it to the specified model class {@link T}.
	 *
	 * @param rs The {@link ResultSet} to read.
	 * @return The specified model class {@link T}.
	 * @throws SQLException If an error occurs.
	 */
	public final T read(ResultSet rs) throws SQLException {
		T instance = instantiate();
		for (TableProperty<T> property : properties) {
			property.getConsumer().accept(instance, readResultSetValue(property, rs));
		}
		return instance;
	}

	public final Connection getConnection() {
		return con;
	}

	private Object readResultSetValue(@NotNull TableProperty<T> property, @NotNull ResultSet rs) throws SQLException {
		Object object = rs.getObject(property.getPropertyName());
		// some exceptions for convenience
		if (property.getH2Type() == H2Type.TIMESTAMP) {
			object = ((Timestamp) object).toLocalDateTime();
		}
		return object;
	}

	private @Nullable T instantiate() {
		try {
			if (Checks.checkEmptyConstructor(modelClass)) {
				return modelClass.getConstructor().newInstance();
			} else {
				log.error("Expected an empty constructor for class {}", modelClass.getSimpleName());
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			log.error("Could not instantiate Model Class " + modelClass.getSimpleName() + ": ", e);
		}
		return null;
	}
}
