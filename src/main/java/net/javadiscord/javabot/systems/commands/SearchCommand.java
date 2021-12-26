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
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.BotConfig;

public class SearchCommand implements SlashCommandHandler {
    static BotConfig config = new BotConfig(Path.of("config"));
    static String subscriptionKey = config.getSystems().azureSubscriptionKey;

    static String host = "https://api.bing.microsoft.com";
    static String path = "/v7.0/search";

    public SearchResults SearchWeb(String searchQuery) throws Exception {
        // Construct the URL.
        URL url = new URL(host + path + "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString()));

        // Open the connection.
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);

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
        String searchTerm = String.valueOf(event.getOption("query"));
        String name;
        String url;
        String snippet;
        var embed = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setTitle("Search Urls");

        try {
            SearchResults result = SearchWeb(searchTerm);
            JsonObject json = JsonParser.parseString(result.jsonResponse).getAsJsonObject();
            JsonArray urls = json.get("webPages").getAsJsonObject().get("value").getAsJsonArray();
            for (int i = 0; i < urls.size(); i++) {
                JsonObject object = urls.get(i).getAsJsonObject();
                name = object.get("name").getAsString();
                url = object.get("name").getAsString();
                snippet = object.get("snippet").getAsString();
                if (object.get("snippet").getAsString().length() > 45) {
                    snippet = object.get("snippet").getAsString().substring(0, 44).concat("...");
                }
                embed
                        .addField("Name", name, true)
                        .addField("Url", url, true)
                        .addField("Embed", snippet, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event.replyEmbeds(embed.build());
    }

    static class SearchResults {
        HashMap<String, String> relevantHeaders;
        String jsonResponse;

        SearchResults(HashMap<String, String> headers, String json) {
            relevantHeaders = headers;
            jsonResponse = json;
        }
    }
}
