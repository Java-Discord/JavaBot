package net.discordjug.javabot.systems.help;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnswerOverflowService {
	private final String apiKey;
	
	public AnswerOverflowService(BotConfig config) {
		apiKey = config.getSystems().getAnswerOverflowApiKey();
	}
	
	public void markAnswer(long postId, long messageId) {
		if (apiKey == null || apiKey.isBlank()) {
			return;
		}
		
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpResponse<String> response = client.send(
					HttpRequest.newBuilder(URI.create("https://www.answeroverflow.com/api/v1/messages/" + postId))
						.header("x-api-key", apiKey)
						.header("Content-Type", "application/json")
						.POST(BodyPublishers.ofString("""
								{
								  "solutionId": "%d"
								}
								""".formatted(messageId)))
					.build(),
					BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.warn("Answer Overflow responded with unexpected status code {}, post ID: {}, message ID: {}, body: {}", response.statusCode(), postId, messageId, response.body());
			}
		} catch (IOException e) {
			log.error("An exception occured trying to mark a post as an answer.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		};
	}
}
