package net.javadiscord.javabot.systems.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Command that allows members to search the internet using the bing api.
 */
public class SearchCommand implements SlashCommand {

	private static final String HOST = "https://api.bing.microsoft.com";
	private static final String PATH = "/v7.0/search";

	private SearchResults searchWeb(String searchQuery) throws IOException {
		// Construct the URL.
		URL url = new URL(HOST + PATH + "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString()) + "&mkt=" + "en-US" + "&safeSearch=Strict");

		// Open the connection.
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Ocp-Apim-Subscription-Key", Bot.config.getSystems().azureSubscriptionKey);

		// Receive the JSON response body.
		String response;
		try(Scanner scan = new Scanner(connection.getInputStream()).useDelimiter("\\A")){
			response = scan.next();
		}

		// Construct the result object.
		SearchResults results = new SearchResults(new HashMap<>(), response);

		// Extract Bing-related HTTP headers.
		Map<String, List<String>> headers = connection.getHeaderFields();
		for (String header : headers.keySet()) {
			if (header == null) continue;      // may have null key
			if (header.startsWith("BingAPIs-") || header.startsWith("X-MSEdge-")) {
				results.relevantHeaders.put(header, headers.get(header).get(0));
			}
		}
		return results;
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var query = event.getOption("query");
		if (query == null) {
			return Responses.warning(event, "Missing Required Query");
		}
		String searchTerm = query.getAsString();
		String name;
		String url;
		String snippet;
		var embed = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setTitle("Search Results");

		try {
			SearchResults result = searchWeb(searchTerm);
			JsonObject json = JsonParser.parseString(result.jsonResponse).getAsJsonObject();
			JsonArray urls = json.get("webPages").getAsJsonObject().get("value").getAsJsonArray();
			StringBuilder resultString = new StringBuilder();
			for (int i = 0; i < Math.min(3, urls.size()); i++) {
				JsonObject object = urls.get(i).getAsJsonObject();
				name = object.get("name").getAsString();
				url = object.get("url").getAsString();
				snippet = object.get("snippet").getAsString();
				if (object.get("snippet").getAsString().length() > 320) {
					snippet = snippet.substring(0, 320);
					int snippetLastPeriod = snippet.lastIndexOf('.');
					if (snippetLastPeriod != -1) {
						snippet = snippet.substring(0, snippetLastPeriod + 1);
					} else {
						snippet = snippet.concat("...");
					}
				}
				resultString.append("**").append(i + 1).append(". [").append(name).append("](")
						.append(url).append(")** \n").append(snippet).append("\n\n");
			}

			embed.setDescription(resultString);
		} catch (IOException e) {
			return Responses.info(event, "Not Found", "There were no results for your search. This might be due to safe-search or because your search was too complex. Please try again.");
		}
		return event.replyEmbeds(embed.build());
	}

	/**
	 * Simple record class, that represents the search results.
	 *
	 * @param relevantHeaders The most relevant headers.
	 * @param jsonResponse    The HTTP Response, formatted as a JSON.
	 */
	public record SearchResults(Map<String, String> relevantHeaders, String jsonResponse) {
	}
}
