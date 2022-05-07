package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks User's names & nicknames and makes sure they're pingable.
 */
@Slf4j
public class PingableNameListener extends ListenerAdapter {

	private static final String ADJECTIVES_URL = "https://gist.githubusercontent.com/karlbright/f91229b8c5ac6f4291dc/raw/4a69c2c50b88ee4559b021c443fee899535adc60/adjectives.txt";
	private static final String NOUNS_URL = "https://raw.githubusercontent.com/hugsy/stuff/main/random-word/english-nouns.txt";
	private static final Random random = new Random();
	private final List<String> nouns;
	private final List<String> adjectives;

	/**
	 * Constructs a new PingableNameListener and loads nouns & adjectives.
	 */
	public PingableNameListener() {
		nouns = readStrings(NOUNS_URL);
		adjectives = readStrings(ADJECTIVES_URL);
		log.info("Loaded {} Nouns!", nouns.size());
		log.info("Loaded {} Adjectives!", adjectives.size());
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (!isPingable(event.getUser().getName()) && !canBypassCheck(event.getMember())) {
			String newName = generateRandomName();
			event.getMember().modifyNickname(newName).queue();
			event.getUser().openPrivateChannel()
					.flatMap(channel -> channel.sendMessageFormat("Your nickname has been set to %s since your username's first three characters were deemed as not-pingable.", newName))
					.queue();
		}
	}

	@Override
	public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
		if (!isPingable(event.getNewNickname()) && !isPingable(event.getUser().getName()) && !canBypassCheck(event.getMember())) {
			String newName = generateRandomName();
			event.getMember().modifyNickname(newName).queue();
			event.getUser().openPrivateChannel()
					.flatMap(channel -> channel.sendMessageFormat("Your nickname has been set to `%s` since both your user- and nickname's first three characters were deemed as not-pingable.", newName))
					.queue();
		}
	}

	/**
	 * Checks if the given name's first three characters contain ASCII characters not between 32 and 126.
	 * @param name The name to check.
	 * @return True if first three characters contain invalid characters, False if not.
	 */
	private boolean isPingable(String name) {
		if (name == null) return true;
		char[] nameChars = name.toCharArray();
		for (int i = 0; i < 2; i++) {
			char c = nameChars[i];
			if (c < 32 || c > 126) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Generates a random name consisting of a noun, an adjective and a number from 1 to 9999.
	 * @return The generated name.
	 */
	private String generateRandomName() {
		String noun = this.nouns.get(random.nextInt(nouns.size()));
		String adjective = this.adjectives.get(random.nextInt(adjectives.size()));
		int number = random.nextInt(10000);
		return StringUtils.capitalize(adjective) + StringUtils.capitalize(noun) + number;
	}

	/**
	 * Reads strings from the given URL to a {@link List}, removes words with a dash.
	 * @param url The URL to read.
	 * @return A {@link List} of Strings.
	 */
	private static List<String> readStrings(String url) {
		List<String> list;
		try(Scanner scan = new Scanner(new URL(url).openStream()).useDelimiter("\\A")) {
			String response = scan.next();
			list = Arrays.stream(response.split("\n")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
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
		return member.getUser().isBot() || member.getUser().isSystem() || member.hasPermission(Permission.NICKNAME_MANAGE, Permission.MESSAGE_MANAGE);
	}
}
