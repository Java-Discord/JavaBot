package net.javadiscord.javabot.systems.commands;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.BotConfig;

public class SearchCommand implements SlashCommandHandler {
    private final BotConfig config = new BotConfig(Path.of("config"));

    public SearchResults SearchWeb(String searchQuery) throws Exception {
        // Construct the URL.
        String HOST = "https://api.bing.microsoft.com";
        String PATH = "/v7.0/search";
        URL url = new URL(HOST + PATH + "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString()) + "&mkt=" + "en-US" + "&safeSearch=Strict");

        // Open the connection.
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", config.getSystems().azureSubscriptionKey);

        // Receive the JSON response body.
        InputStream stream = connection.getInputStream();
        String response = new Scanner(stream).useDelimiter("\\A").next();

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
        stream.close();
        return results;
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
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
            SearchResults result = SearchWeb(searchTerm);
            JsonObject json = JsonParser.parseString(result.jsonResponse).getAsJsonObject();
            JsonArray urls = json.get("webPages").getAsJsonObject().get("value").getAsJsonArray();
            StringBuilder resultString = new StringBuilder();
            for (int i = 0; i < Math.min(3, urls.size()); i++) {
                JsonObject object = urls.get(i).getAsJsonObject();
                name = object.get("name").getAsString();
                url = object.get("url").getAsString();
                snippet = object.get("snippet").getAsString();
                if (object.get("snippet").getAsString().length() > 260) {
                    snippet = object.get("snippet").getAsString().substring(0, 260).concat("...");
                }
                resultString.append("**").append(i + 1).append(". [").append(name).append("](").append(url).append(")** \n").append(snippet).append("\n\n");
            }

            embed.setDescription(resultString);
        } catch (Exception e) {
            return Responses.info(event, "Not Found", "There were no results for your search. This might be due to safe-search or because your search was too complex.");
        }
        return event.replyEmbeds(embed.build());
    }

    public record SearchResults(HashMap<String, String> relevantHeaders, String jsonResponse) {
    }
}
