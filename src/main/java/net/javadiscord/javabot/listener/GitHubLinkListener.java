package net.javadiscord.javabot.listener;

import xyz.dynxsty.dih4jda.util.Pair;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.InteractionUtils;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for GitHub Links and sends the code snippet if it found one.
 */
public class GitHubLinkListener extends ListenerAdapter {

	private final Pattern GITHUB_LINK_PATTERN = Pattern.compile("https:?//github\\.com/([A-Za-z0-9\\-_.]+)/([A-Za-z0-9\\-_.]+)/(?:blob|tree)/(\\S+?)/(\\S+?)(\\.\\S+)?#L(\\d+)[-~]?L?(\\d*)");

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
		Matcher matcher = GITHUB_LINK_PATTERN.matcher(event.getMessage().getContentRaw());
		if (matcher.find()) {
			Pair<String, String> content = parseGithubUrl(matcher.group());
			if (!content.getFirst().isBlank() && !content.getSecond().isBlank()) {
				event.getMessage().reply(String.format("```%s\n%s\n```", content.getSecond(), StringUtils.standardSanitizer().compute(content.getFirst())))
						.setAllowedMentions(List.of())
						.setActionRow(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"), Button.link(matcher.group(), "View on GitHub"))
						.queue();
			}
		}
	}

	/**
	 * Gets the contents of a GitHub URL.
	 *
	 * @param link The initial input url.
	 * @return A {@link Pair} containing the files content & extension.
	 */
	@Contract("_ -> new")
	private @NotNull Pair<String, String> parseGithubUrl(@NotNull String link) {
		String[] arr = link.split("/");
		// Removes all unnecessary elements
		String[] segments = Arrays.copyOfRange(arr, 3, arr.length);
		// The file name, split by "."
		String[] file = segments[segments.length - 1].split("\\.");
		Integer[] lines = Arrays.stream(file[1].split("L"))
				.map(line -> line.replace("-", ""))
				.filter(line -> line.matches("-?\\d+")) // check if the given link is a number
				.map(Integer::valueOf).sorted().toArray(Integer[]::new);
		if (lines.length == 0) {
			return new Pair<>("", "");
		}
		int to = lines.length != 2 ? lines[0] : lines[1];
		String reqUrl = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
				segments[0], segments[1],
				String.join("/", Arrays.copyOfRange(segments, 3, segments.length - 1)),
				segments[segments.length - 1]);
		String content;
		try {
			content = this.getContentFromRawGitHubUrl(reqUrl, lines[0], to);
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			content = e.getMessage();
		}
		if (content.equals(reqUrl)) content = "Unable to fetch content.";
		return new Pair<>(content, file[file.length - 1].split("#")[0]);
	}

	private String getContentFromRawGitHubUrl(String reqUrl, int from, int to) throws IOException {
		URLConnection connection = new URL(reqUrl).openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return StringUtils.fromBufferedReader(reader, from, to);
	}
}
