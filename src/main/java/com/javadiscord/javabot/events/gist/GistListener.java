package com.javadiscord.javabot.events.gist;

import com.javadiscord.javabot.external_apis.github.File;
import com.javadiscord.javabot.external_apis.github.GistResponse;
import com.javadiscord.javabot.external_apis.github.GithubService;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GistListener extends ListenerAdapter {

	private static final String BASIC_GIST_PATTERN_STRING = ".*https://gist\\.github\\.com/.+?(?=/)/(.+?(?=#|\\s))";
	private static final Pattern BASIC_GIST_PATTERN = Pattern.compile(BASIC_GIST_PATTERN_STRING);
	private static final int GIST_DELAY = 30;
	private final GithubService service;
	private static final int SIZE_LIMIT = 2000;
	private static final Logger log = LoggerFactory.getLogger(GistListener.class);
	private final Map<String, GistState> messageMap = new ConcurrentHashMap<>();
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	public GistListener(GithubService service) {
		this.service = service;
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		executorService.scheduleAtFixedRate(() -> messageMap.entrySet().stream()
				.filter(this::isOverMaxTime)
				.forEach(entry -> handleExpiredGist(event, entry)), GIST_DELAY, GIST_DELAY, TimeUnit.MINUTES);
	}

	private void handleExpiredGist(ReadyEvent event, Map.Entry<String, GistState> entry) {
		clearReactionsFromExpiredGist(event, entry);
		messageMap.entrySet().remove(entry);
	}

	private void clearReactionsFromExpiredGist(ReadyEvent event, Map.Entry<String, GistState> entry) {
		TextChannel channel = event.getJDA().getTextChannelById(entry.getValue().getChannelId());
		if (channel != null) {
			channel.retrieveMessageById(entry.getKey()).queue(msg -> msg.clearReactions().queue());
		}
	}

	private boolean isOverMaxTime(Map.Entry<String, GistState> gist) {
		return Duration.between(gist.getValue().getCreated(), Instant.now()).toHours() >= GIST_DELAY;
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
					if (result == null || result.getFiles().size() <= 0) return;
					List<File> fileList = new ArrayList<>(result.getFiles().values());
					File file = fileList.get(0);
					event.getChannel().sendMessage(constructMessage(file)).queue(msg -> {
						messageMap.put(msg.getId(), new GistState(fileList, 0, Instant.now(), msg.getChannel().getIdLong()));
						msg.addReaction("U+23EA").submit().thenRun(() -> msg.addReaction("U+23E9").queue());

					});
				}

				@Override
				public void onFailure(@NotNull Call<GistResponse> call, @NotNull Throwable t) {
					log.error("Unable to complete/parse Gist api call", t);
				}
			});
		}
	}

	public Message constructMessage(File file) {
		MessageBuilder builder = new MessageBuilder();
		builder.appendCodeBlock(file.getContent(), file.getLanguage());
		builder.append("Current file: ").append(file.getFilename());
		return builder.build();
	}

	@Override
	public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
		if (event.getUser().isBot() || !messageMap.containsKey(event.getMessageId())) return;
		GistState state = messageMap.get(event.getMessageId());
		if (event.getReaction().getReactionEmote().getName().equals("⏪")) {
			event.getChannel().retrieveMessageById(event.getMessageId()).queue(msg -> {
				if (state.getPosition() > 0) {
					state.setPosition(state.getPosition() - 1);
				}
				File file = state.getFiles().get(state.getPosition());
				msg.editMessage(constructMessage(file)).queue();

			});
		} else if (event.getReaction().getReactionEmote().getName().equals("⏩")) {
			event.getChannel().retrieveMessageById(event.getMessageId()).queue(msg -> {
				if (state.getPosition() < state.getFiles().size()) {
					state.setPosition(state.getPosition() + 1);
				}
				File file = state.getFiles().get(state.getPosition());
				msg.editMessage(constructMessage(file)).queue();

			});
		}
		event.getReaction().removeReaction(event.getUser()).queue();
	}
}
