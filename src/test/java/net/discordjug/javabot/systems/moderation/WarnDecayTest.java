package net.discordjug.javabot.systems.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.systems.moderation.ModerationService.SeverityInformation;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;
import net.discordjug.javabot.systems.moderation.warn.model.WarnSeverity;

/**
 * Tests for warn decay.
 */
public class WarnDecayTest {
	@Test
	void testWithNoWarns() {
		ModerationConfig config = new ModerationConfig();
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, Collections.emptyList());
		assertEquals(new SeverityInformation(0, 0, Collections.emptyList()), result);
	}
	
	@Test
	void testWithNewWarn() {
		ModerationConfig config = new ModerationConfig();
		List<Warn> warns = List.of(createWarn(LocalDateTime.now()));
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(20, 0, warns), result);
	}
	
	@Test
	void testWithOnceDiscountedWarn() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(createWarn(LocalDateTime.now().minusDays(1)));
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(15, 5, warns), result);
	}
	
	@Test
	void testWithMultipleTimesDiscountedWarn() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(createWarn(LocalDateTime.now().minusDays(2)));
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(10, 10, warns), result);
	}
	
	@Test
	void testWithMultipleDiscountedWarns() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(
				createWarn(LocalDateTime.now().minusDays(2)),
				createWarn(LocalDateTime.now().minusDays(5), WarnSeverity.HIGH)
			);
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(75, 25, warns), result);
	}
	
	@Test
	void testWithTooOldWarn() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(createWarn(LocalDateTime.now().minusDays(10)));
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(0, 0, Collections.emptyList()), result);
	}
	
	@Test
	void testWithTooOldAndRecentWarns() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		Warn recentWarn = createWarn(LocalDateTime.now().minusDays(2));
		List<Warn> warns = List.of(
				recentWarn,
				createWarn(LocalDateTime.now().minusDays(8))
			);
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(10, 10, List.of(recentWarn)), result);
	}
	
	@Test
	void testWarnReenablingFutureWarns() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(
				createWarn(LocalDateTime.now().minusDays(2)),
				createWarn(LocalDateTime.now().minusDays(8)),
				createWarn(LocalDateTime.now().minusDays(10), WarnSeverity.HIGH)
			);
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(70, 50, warns), result);
	}
	
	@Test
	void testWithMultipleRecentAndOldWarns() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> expected = List.of(
				createWarn(LocalDateTime.now().minusDays(2)),//20
				createWarn(LocalDateTime.now().minusDays(5), WarnSeverity.HIGH)//80, -25
			);
		List<Warn> warns = new ArrayList<>(expected);
		warns.add(createWarn(LocalDateTime.now().minusDays(10)));//ignored
		
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(75, 25, expected), result);
	}
	
	@Test
	void testMultipleOldWarns() {
		ModerationConfig config = new ModerationConfig();
		config.setWarnDecayAmount(5);
		config.setWarnDecayDays(1);
		
		List<Warn> warns = List.of(
				//would all be ignored on their own but outweigh decay together
				createWarn(LocalDateTime.now().minusDays(5)),
				createWarn(LocalDateTime.now().minusDays(5)),
				createWarn(LocalDateTime.now().minusDays(5)),
				createWarn(LocalDateTime.now().minusDays(5)),
				createWarn(LocalDateTime.now().minusDays(5))
			);
		SeverityInformation result = ModerationService.calculateSeverityWeight(config, warns);
		assertEquals(new SeverityInformation(75, 25, warns), result);
	}
	
	private Warn createWarn(LocalDateTime creationTimestamp) {
		return createWarn(creationTimestamp, WarnSeverity.LOW);
	}
	
	private Warn createWarn(LocalDateTime creationTimestamp, WarnSeverity severity) {
		Warn warn = new Warn(0, 0, severity, "a");
		warn.setCreatedAt(creationTimestamp);
		return warn;
	}
}
