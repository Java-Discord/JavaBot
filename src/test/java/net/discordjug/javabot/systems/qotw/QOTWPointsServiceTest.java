package net.discordjug.javabot.systems.qotw;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.discordjug.javabot.systems.qotw.model.QOTWAccount;

class QOTWPointsServiceTest {
	
	private QOTWPointsService pointsService = new QOTWPointsService(null);

	@Test
	void testGetQOTWRankNotPresent() {
		assertEquals(-1, pointsService.getQOTWRank(0, List.of()));
		assertEquals(-1, pointsService.getQOTWRank(0, List.of(createAccount(1, 1))));
	}
	
	@Test
	void testNormalQOTWRank() {
		assertEquals(1, pointsService.getQOTWRank(1, List.of(createAccount(1, 1))));
		assertEquals(2, pointsService.getQOTWRank(2, List.of(
				createAccount(1, 2),
				createAccount(2, 1)
				)));
	}
	
	@Test
	void testQOTWRankWithTiesBefore() {
		assertEquals(3, pointsService.getQOTWRank(1, List.of(
				createAccount(2, 2),
				createAccount(3, 2),
				createAccount(1, 1)
				)));
	}
	
	@Test
	void testQOTWRankWithTiesAtSamePosition() {
		assertEquals(2, pointsService.getQOTWRank(1, List.of(
				createAccount(2, 2),
				createAccount(3, 1),
				createAccount(1, 1)
				)));
		assertEquals(2, pointsService.getQOTWRank(1, List.of(
				createAccount(2, 2),
				createAccount(1, 1),
				createAccount(3, 1)
				)));
	}

	private QOTWAccount createAccount(long userId, int score) {
		QOTWAccount account = new QOTWAccount();
		account.setUserId(userId);
		account.setPoints(score);
		return account;
	}

}
