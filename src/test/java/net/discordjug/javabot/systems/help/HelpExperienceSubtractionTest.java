package net.discordjug.javabot.systems.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import net.discordjug.javabot.data.h2db.DbHelper;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.model.HelpAccount;

/**
 * Tests functionality of automated experience subtraction.
 */
public class HelpExperienceSubtractionTest {
	
	private HikariDataSource dataSource;
	private HelpAccountRepository repo;
	
	@BeforeEach
	void setUp() throws IOException, SQLException {
		dataSource = DataSourceBuilder.create()
				.type(HikariDataSource.class)
				.url("jdbc:h2:mem:test")
				.username("test")
				.password("")
				.build();
		
		DbHelper.initializeSchema(dataSource);
		
		JdbcTemplate template = new JdbcTemplate(dataSource);
		repo = new HelpAccountRepository(template);
	}
	
	@AfterEach
	void cleanUp() {
		dataSource.close();
	}
	
	/**
	 * If a user has less XP than the minimum experience subtraction, the user should lose all XP.
	 */
	@Test
	void testUserHasLessThanMinimum() {
		repo.insert(new HelpAccount(1, 1));
		repo.removeExperienceFromAllAccounts(50, 2, 10);
		assertEquals(0, repo.getByUserId(1).get().getExperience());
	}
	
	/**
	 * If the XP to subtract is less than the minimum, the minimum XP should be subtracted.
	 */
	@Test
	void testSubtractMinimum() {
		repo.insert(new HelpAccount(1, 6));//6XP
		//would remove 50% i.e. 3XP
		//the minimum is 4XP hence it should subtract 4XP
		repo.removeExperienceFromAllAccounts(50, 4, 10);
		assertEquals(2, repo.getByUserId(1).get().getExperience());
	}
	
	@Test
	void testSubtractMaximum() {
		repo.insert(new HelpAccount(1, 100));
		//tries to subtract 50% which is 50XP
		//but maximum is 10XP hence it should subtract 10XP
		repo.removeExperienceFromAllAccounts(50, 1, 10);
		assertEquals(90, repo.getByUserId(1).get().getExperience());
	}
	
	@Test
	void testFraction() {
		repo.insert(new HelpAccount(1, 100));
		//subtract 10% i.e. 10XP
		//should be inside [1,50] hence neither minimum nor maximum is active
		repo.removeExperienceFromAllAccounts(10, 1, 50);
		assertEquals(90, repo.getByUserId(1).get().getExperience());
		//subtract 10% i.e. 9XP
		//should be inside [1,50] hence neither minimum nor maximum is active
		repo.removeExperienceFromAllAccounts(10, 1, 50);
		assertEquals(81, repo.getByUserId(1).get().getExperience());
	}
	
	@Test
	void testMultipleUsers() {
		repo.insert(new HelpAccount(79, 79));//below min
		repo.insert(new HelpAccount(80, 80));//exactly min
		repo.insert(new HelpAccount(100, 100));//within bounds
		repo.insert(new HelpAccount(2_000, 2_000));//within bounds
		repo.insert(new HelpAccount(3_999, 3_999));//close to upper bound
		repo.insert(new HelpAccount(4_000, 4_000));//exactly upper bound
		repo.insert(new HelpAccount(4_001, 4_001));//exceeds upper bound
		repo.insert(new HelpAccount(10_000, 10_000));//significantly exceeds upper bound
		
		repo.removeExperienceFromAllAccounts(1.25, 1, 50);
		
		double delta = 0.0001;//required precision
		assertEquals(78, repo.getByUserId(79).get().getExperience(), delta);
		assertEquals(79, repo.getByUserId(80).get().getExperience(), delta);
		assertEquals(98.75, repo.getByUserId(100).get().getExperience(), delta);
		assertEquals(1975, repo.getByUserId(2_000).get().getExperience(), delta);
		assertEquals(3949.0125, repo.getByUserId(3_999).get().getExperience(), delta);
		assertEquals(3950, repo.getByUserId(4_000).get().getExperience(), delta);
		assertEquals(3951, repo.getByUserId(4_001).get().getExperience(), delta);
		assertEquals(9_950, repo.getByUserId(10_000).get().getExperience(), delta);
	}
}
