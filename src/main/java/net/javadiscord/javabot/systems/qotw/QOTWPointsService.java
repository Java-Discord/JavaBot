package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service class which is used to get and manipulate other {@link QOTWAccount}s.
 */
@RequiredArgsConstructor
@Service
public class QOTWPointsService {
	private final QuestionPointsRepository pointsRepository;

	/**
	 * Creates a new QOTW Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link QOTWAccount} object.
	 * @throws DataAccessException If an error occurs.
	 */
	@Transactional
	public QOTWAccount getOrCreateAccount(long userId) throws DataAccessException {
		QOTWAccount account;
		Optional<QOTWAccount> optional = pointsRepository.getByUserId(userId, getCurrentMonth());
		if (optional.isPresent()) {
			account = optional.get();
		} else {
			account = new QOTWAccount();
			account.setUserId(userId);
			account.setPoints(0);
		}
		return account;
	}

	/**
	 * Gets the given user's QOTW-Rank.
	 *
	 * @param userId The user whose rank should be returned.
	 * @return The QOTW-Rank as an integer.
	 */
	public int getQOTWRank(long userId) {
		try{
			List<QOTWAccount> accounts = pointsRepository.sortByPoints(getCurrentMonth());
			return accounts.stream()
					.map(QOTWAccount::getUserId)
					.toList()
					.indexOf(userId) + 1;
		} catch (DataAccessException e) {
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
		} catch (DataAccessException e) {
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
	public List<Pair<QOTWAccount, Member>> getTopMembers(int n, Guild guild) {
		try {
			List<QOTWAccount> accounts = pointsRepository.sortByPoints(getCurrentMonth());
			return accounts.stream()
					.map(s -> new Pair<>(s, guild.getMemberById(s.getUserId())))
					.filter(p->p.first().getPoints() > 0)
					.filter(p -> p.second() != null)
					.limit(n)
					.toList();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}

	/**
	 * Gets the specified amount of {@link QOTWAccount}s, sorted by their points.
	 *
	 * @param amount The amount to retrieve.
	 * @param page The page to get.
	 * @return An unmodifiable {@link List} of {@link QOTWAccount}s.
	 */
	public List<QOTWAccount> getTopAccounts(int amount, int page) {
		try {
			return pointsRepository.getTopAccounts(getCurrentMonth(), page, amount);
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}

	/**
	 * Increments a single user's QOTW-Points.
	 *
	 * @param userId The ID of the user whose points shall be incremented
	 * @return The total points after the update.
	 */
	public long increment(long userId) {
		return increment(userId, 1);
	}

	/**
	 * Increments a single user's QOTW-Points.
	 *
	 * @param userId The ID of the user whose points shall be incremented
	 * @param incrementCount the number of points to increment
	 * @return The total points after the update.
	 */
	public long increment(long userId, int incrementCount) {
		try {
			LocalDate date=LocalDate.now();
			int points = pointsRepository.getPointsAtDate(userId, date) + incrementCount;
			pointsRepository.setPointsAtDate(userId, date, points);
			LocalDate month = getCurrentMonth();
			long newScore = pointsRepository.getByUserId(userId, month).map(QOTWAccount::getPoints).orElse(0L);
			return newScore;
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return 0;
		}
	}

	public static LocalDate getCurrentMonth() {
		return YearMonth.from(LocalDate.now()).atDay(1);
	}
}
