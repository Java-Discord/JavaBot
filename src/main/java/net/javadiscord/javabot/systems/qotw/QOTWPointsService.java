package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.ExceptionLogger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service class which is used to get and manipulate other {@link QOTWAccount}s.
 */
@RequiredArgsConstructor
public class QOTWPointsService {
	private final DataSource dataSource;

	/**
	 * Creates a new QOTW Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link QOTWAccount} object.
	 * @throws SQLException If an error occurs.
	 */
	public QOTWAccount getOrCreateAccount(long userId) throws SQLException {
		QOTWAccount account;
		try (Connection con = this.dataSource.getConnection()) {
			con.setAutoCommit(false);
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			Optional<QOTWAccount> optional = repo.getByUserId(userId);
			if (optional.isPresent()) {
				account = optional.get();
			} else {
				account = new QOTWAccount();
				account.setUserId(userId);
				account.setPoints(0);
				repo.insert(account);
			}
			con.commit();
			return account;
		}
	}

	/**
	 * Gets the given user's QOTW-Rank.
	 *
	 * @param userId The user whose rank should be returned.
	 * @return The QOTW-Rank as an integer.
	 */
	public int getQOTWRank(long userId) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			List<QOTWAccount> accounts = repo.sortByPoints();
			return accounts.stream()
					.map(QOTWAccount::getUserId)
					.toList()
					.indexOf(userId) + 1;
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return -1;
		}
	}

	/**
	 * Gets the given user's QOTW-Points.
	 *
	 * @param userId The id of the user.
	 * @return The user's total QOTW-Points
	 */
	public long getPoints(long userId) {
		try {
			return getOrCreateAccount(userId).getPoints();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return -1;
		}
	}

	/**
	 * Gets the top N members from a guild based on their QOTW-Points.
	 *
	 * @param n     The amount of members to get.
	 * @param guild The current guild.
	 * @return A {@link List} with the top member ids.
	 */
	public List<Member> getTopMembers(int n, Guild guild) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			List<QOTWAccount> accounts = repo.sortByPoints();
			return accounts.stream()
					.map(QOTWAccount::getUserId)
					.map(guild::getMemberById)
					.filter(Objects::nonNull)
					.limit(n)
					.toList();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}

	/**
	 * Increments a single user's QOTW-Points.
	 *
	 * @param userId The discord Id of the user.
	 * @return The total points after the update.
	 */
	public long increment(long userId) {
		try (Connection con = dataSource.getConnection()) {
			QuestionPointsRepository repo = new QuestionPointsRepository(con);
			QOTWAccount account = getOrCreateAccount(userId);
			account.setPoints(account.getPoints() + 1);
			if (repo.update(account)) {
				return account.getPoints();
			} else {
				return 0;
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return 0;
		}
	}
}
