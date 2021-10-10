package com.javadiscord.javabot.events;

import com.javadiscord.javabot.external_apis.github.GistResponse;
import com.javadiscord.javabot.external_apis.github.GithubService;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GistListener extends ListenerAdapter {

    private static String BASIC_GIST_PATTERN_STRING = ".*https://gist\\.github\\.com/.+?(?=/)/(.+?(?=#|\\s))";
    private static Pattern BASIC_GIST_PATTERN = Pattern.compile(BASIC_GIST_PATTERN_STRING);
    private GithubService service;
    private static int SIZE_LIMIT = 2000;
    private static final Logger log = LoggerFactory.getLogger(GistListener.class);


    public GistListener() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        service = retrofit.create(GithubService.class);
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        Matcher matcher = BASIC_GIST_PATTERN.matcher(event.getMessage().getContentRaw());

        if (matcher.find()) {
            service.getGistInformation(matcher.group(1)).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NotNull Call<GistResponse> call, @NotNull Response<GistResponse> response) {
                    GistResponse result = response.body();
                    if (result == null || result.getFiles().size() != 1) return;
                    result.getFiles().values().forEach(file -> {
                        if (file.getSize() <= SIZE_LIMIT) {
                            String message = constructMessage(file.getContent(), file.getLanguage());
                            event.getChannel().sendMessage(message).queue();
                        }
                    });
                }


                @Override
                public void onFailure(@NotNull Call<GistResponse> call, @NotNull Throwable t) {
                    log.error("Unable to complete/parse Gist api call", t);
                }
            });
        }


    }

    public String constructMessage(String content, String language) {
        return "```" + language + "\n" + content + "```";
    }
}
