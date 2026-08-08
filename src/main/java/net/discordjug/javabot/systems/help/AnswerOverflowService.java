package net.discordjug.javabot.systems.help;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import org.springframework.stereotype.Service;

/**
 * A service class for interacting with the Answer Overflow bot.
 */
@Service
@Slf4j
public class AnswerOverflowService {
	private final String apiKey;
	
	public AnswerOverflowService(BotConfig config) {
		apiKey = config.getSystems().getAnswerOverflowApiKey();
	}
	
	/**
	 * Marks a message as the answer of a forum post with Answer Overflow.
	 * @param postId The Discord ID of the forum post
	 * @param messageId The Discord ID of the message that should be marked as the answer.
	 */
	public void markAnswer(long postId, long messageId) {
		if (apiKey == null || apiKey.isBlank()) {
			return;
		}
		
		try (HttpClient client = HttpClient.newHttpClient()) {
			client.sendAsync(
					HttpRequest.newBuilder(URI.create("https://www.answeroverflow.com/api/v1/messages/" + postId))
						.header("x-api-key", apiKey)
						.header("Content-Type", "application/json")
						.POST(BodyPublishers.ofString("""
								{
									"solutionId": "%d"
								}
								""".formatted(messageId)))
					.build(),
					BodyHandlers.ofString())
				.thenAccept(response -> {
					if (response.statusCode() != 200) {
						log.warn("Answer Overflow responded with unexpected status code {}, post ID: {}, message ID: {}, body: {}", response.statusCode(), postId, messageId, response.body());
					}
				})
				.exceptionally(e -> {
					log.error("An exception occured trying to mark a post as an answer.", e);
					return null;
				});
		}
	}
}
