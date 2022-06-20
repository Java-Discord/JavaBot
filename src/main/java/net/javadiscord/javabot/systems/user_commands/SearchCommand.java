package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;

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
public class SearchCommand extends SlashCommand {
	private static final String HOST = "https://api.bing.microsoft.com";
	private static final String PATH = "/v7.0/search";
	public SearchCommand() {
		setSlashCommandData(Commands.slash("search", "Searches the web using the provided query")
				.addOption(OptionType.STRING, "query", "Text that will be converted into a search query", true)
				.setGuildOnly(true)
		);
	}

	private SearchResults searchWeb(String searchQuery) throws IOException {
		// Construct the URL.
		URL url = new URL(HOST + PATH + "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString()) + "&mkt=" + "en-US" + "&safeSearch=Strict");

		// Open the connection.
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Ocp-Apim-Subscription-Key", Bot.config.getSystems().getAzureSubscriptionKey());
		// Receive the JSON response body.
		String response;
		try (Scanner scan = new Scanner(connection.getInputStream()).useDelimiter("\\A")) {
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
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping query = event.getOption("query");
		if (query == null) {
			Responses.warning(event, "Missing required query option.");
			return;
		}
		String searchTerm = query.getAsString();
		String name;
		String url;
		String snippet;
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
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
			Sentry.captureException(e);
			Responses.info(event, "Not Found", "There were no results for your search. This might be due to safe-search or because your search was too complex. Please try again.")
					.queue();
			return;
		}
		event.replyEmbeds(embed.build()).queue();
	}

	/**
	 * Simple record class, that represents the search results.
	 *
	 * @param relevantHeaders The most relevant headers.
	 * @param jsonResponse    The HTTP Response, formatted as a JSON.
	 */
	public record SearchResults(Map<String, String> relevantHeaders, String jsonResponse) {}
}
