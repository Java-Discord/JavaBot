package net.discordjug.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks User's names & nicknames and makes sure they're pingable.
 */
@Slf4j
public class PingableNameListener extends ListenerAdapter {

	private static final String ADJECTIVES_URL = "https://gist.githubusercontent.com/karlbright/f91229b8c5ac6f4291dc/raw/4a69c2c50b88ee4559b021c443fee899535adc60/adjectives.txt";
	private static final String NOUNS_URL = "https://raw.githubusercontent.com/hugsy/stuff/main/random-word/english-nouns.txt";
	private static final Pattern PINGABLE_NAME_PATTERN = Pattern.compile("(\\W*\\w){3,}\\W*");
	private static final Random random = new Random();
	private final NotificationService notificationService;
	private final List<String> nouns;
	private final List<String> adjectives;

	/**
	 * Constructs a new PingableNameListener and loads nouns & adjectives.
	 * @param notificationService The {@link NotificationService}
	 */
	public PingableNameListener(NotificationService notificationService) {
		nouns = readStrings(NOUNS_URL);
		adjectives = readStrings(ADJECTIVES_URL);
		this.notificationService = notificationService;
		log.info("Loaded {} Nouns!", nouns.size());
		log.info("Loaded {} Adjectives!", adjectives.size());
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		checkNickname(event.getMember(), null);
	}

	@Override
	public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
		checkNickname(event.getMember(), event.getNewNickname());
	}

	/**
	 * Checks whether the given {@link Member}'s nickname should be changed.
	 * @param member The {@link Member} to check.
	 * @param nickname The {@link Member}'s new Nickname, null if that does not exist.
	 */
	private void checkNickname(Member member, String nickname) {
		if (!isPingable(member.getEffectiveName()) && !canBypassCheck(member)) {
			changeName(member);
		}
	}

	/**
	 * Changes the given {@link Member}s name to a randomly generated one.
	 * @param member The Member whose name should be changed.
	 */
	private void changeName(Member member) {
		String oldName = member.getNickname();
		String newName = generateRandomName();
		member.modifyNickname(newName.substring(0, Math.min(31, newName.length()))).queue();
		member.getUser().openPrivateChannel()
				.flatMap(channel -> channel.sendMessageFormat("Your display name in %s has been set to `%s` since your previous name was deemed as non-pingable.", member.getGuild().getName(), newName))
				.queue();
		notificationService.withGuild(member.getGuild()).sendToMessageLog(c -> c.sendMessageFormat("Changed %s's nickname from `%s` to `%s`.", member.getAsMention(), oldName, newName));
	}

	/**
	 * Checks if the given name's first three characters contain ASCII characters not between 32 and 126.
	 * @param name The name to check.
	 * @return True if first three characters contain invalid characters, False if not.
	 */
	private boolean isPingable(String name) {
		return PINGABLE_NAME_PATTERN.matcher(name).matches();
	}

	/**
	 * Generates a random name consisting of a noun, an adjective and a number from 1 to 9999.
	 * @return The generated name.
	 */
	private String generateRandomName() {
		String noun = nouns.get(random.nextInt(nouns.size()));
		String adjective = adjectives.get(random.nextInt(adjectives.size()));
		return StringUtils.capitalize(adjective) + StringUtils.capitalize(noun);
	}

	/**
	 * Reads strings from the given URL to a {@link List}, removes words with a dash.
	 * @param url The URL to read.
	 * @return A {@link List} of Strings.
	 */
	private static List<String> readStrings(String url) {
		List<String> list;
		try (Scanner scan = new Scanner(new URL(url).openStream()).useDelimiter("\\n")) {
			list = scan.tokens().collect(Collectors.toList());
		} catch (IOException e) {
			ExceptionLogger.capture(e, PingableNameListener.class.getSimpleName());
			log.error("Error during retrieval of words.");
			list = new ArrayList<>();
		}

		list.removeIf(word -> word.contains("-"));
		return list;
	}

	/**
	 * Checks if the given {@link Member} can bypass the name-check.
	 * @param member The {@link Member} to check.
	 * @return Whether the Member can bypass name-checks or not.
	 */
	private static boolean canBypassCheck(Member member) {
		return member.getUser().isBot() || member.getUser().isSystem() || !member.getGuild().getSelfMember().canInteract(member);
	}
}
