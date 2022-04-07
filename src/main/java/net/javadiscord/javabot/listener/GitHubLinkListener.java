package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
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
			Pair<String, String> content = this.parseGithubUrl(matcher.group());
			if (!content.first().isBlank() && !content.first().isBlank()) {
				event.getMessage().replyEmbeds(this.buildGitHubEmbed(content, event.getMessage()))
						.setActionRow(Button.link(matcher.group(), "View on GitHub"))
						.queue();
			}
		}
	}

	private MessageEmbed buildGitHubEmbed(Pair<String, String> content, Message message) {
		return new EmbedBuilder()
				.setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
				.setColor(Bot.config.get(message.getGuild()).getSlashCommand().getDefaultColor())
				.setDescription(String.format("```%s\n%s\n```", content.second(), content.first()))
				.build();
	}

	/**
	 * Gets the contents of a GitHub URL.
	 *
	 * @param link The initial input url.
	 * @return A {@link Pair} containing the files content & extension.
	 */
	private Pair<String, String> parseGithubUrl(String link) {
		String[] arr = link.split("/");
		// Removes all unnecessary elements
		String[] segments = Arrays.copyOfRange(arr, 3, arr.length);
		// The file name, split by "."
		String[] file = segments[segments.length - 1].split("\\.");
		Integer[] lines = Arrays.stream(file[1].split("L"))
				.map(line -> line.replace("-", ""))
				.filter(line -> line.matches("-?\\d+")) // check if the given link is a number
				.map(Integer::valueOf).sorted().toArray(Integer[]::new);
		int to = lines.length != 2 ? lines[0] : lines[1];
		String reqUrl = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
				segments[0], segments[1],
				String.join("/", Arrays.copyOfRange(segments, 3, segments.length - 1)),
				segments[segments.length - 1]);
		String content;
		try {
			content = this.getContentFromRawGitHubUrl(reqUrl, lines[0], to);
		} catch (IOException e) {
			content = e.getMessage();
		}
		if (content.equals(reqUrl)) content = "Unable to fetch content.";
		return new Pair<>(content, file[1].split("#")[0]);
	}

	private String getContentFromRawGitHubUrl(String reqUrl, int from, int to) throws IOException {
		URLConnection connection = new URL(reqUrl).openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return StringUtils.fromBufferedReader(reader, from, to);
	}
}
