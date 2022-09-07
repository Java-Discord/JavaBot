package net.javadiscord.javabot;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.dynxsty.dih4jda.DIH4JDA;
import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import com.dynxsty.dih4jda.interactions.components.SelectMenuHandler;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.tasks.PresenceUpdater;
import net.javadiscord.javabot.util.ExceptionLogger;

/**
 * The main class where the bot is initialized.
 */
@SpringBootApplication
@ComponentScan(
	includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SlashCommand.class, ContextCommand.class, ListenerAdapter.class }),
	excludeFilters = @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, classes = PresenceUpdater.class)
)
@EnableScheduling
@RequiredArgsConstructor
public class Bot {

	private final DIH4JDA dih4jda;
	private final BotConfig config;
	private final List<SlashCommand> commands;
	private final List<ContextCommand> contextCommands;
	private final List<ListenerAdapter> listeners;
	private final ApplicationContext ctx;


	private void addEventListeners(final List<ListenerAdapter> listeners) {
		for (ListenerAdapter listener : listeners) {
			dih4jda.getJDA().addEventListener(listener);
		}
		dih4jda.getJDA().addEventListener(dih4jda);
	}

	/**
	 * Initializes Sentry, interactions and listeners.
	 */
	@PostConstruct
	public void init() {
		Sentry.init(options -> {
			options.setDsn(config.getSystems().getSentryDsn());
			options.setTracesSampleRate(1.0);
			options.setDebug(false);
		});
		if (!commands.isEmpty()) {
			dih4jda.addSlashCommands(commands.toArray(SlashCommand[]::new));
		}
		if (!contextCommands.isEmpty()) {
			dih4jda.addContextCommands(contextCommands.toArray(ContextCommand[]::new));
		}
		try {
			dih4jda.registerInteractions();
		} catch (ReflectiveOperationException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
		addEventListeners(listeners);
		registerComponentHandlers(ctx);
	}

	/**
	 * The main method that starts the bot. This involves a few steps:
	 * <ol>
	 *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
	 *     <li>Loading the configuration JSON file.</li>
	 *     <li>Creating and configuring the {@link JDA} instance that enables the bots' Discord connectivity.</li>
	 *     <li>Initializing the {@link DIH4JDA} instance.</li>
	 *     <li>Adding event listeners to the bot.</li>
	 * </ol>
	 *
	 * @param args Command-line arguments.
	 * @throws Exception If any exception occurs during bot creation.
	 */
	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
		AllowedMentions.setDefaultMentions(EnumSet.of(Message.MentionType.ROLE, Message.MentionType.CHANNEL, Message.MentionType.USER, Message.MentionType.EMOJI));
		SpringApplication.run(Bot.class, args);
	}

	private void registerComponentHandlers(ApplicationContext ctx) {
		Map<String, Object> interactionHandlers = ctx.getBeansWithAnnotation(AutoDetectableComponentHandler.class);
		Map<List<String>, ButtonHandler> buttonHandlers = new HashMap<>();
		Map<List<String>, ModalHandler> modalHandlers = new HashMap<>();
		Map<List<String>, SelectMenuHandler> selectMenuHandlers = new HashMap<>();
		for (Object handler : interactionHandlers.values()) {
			AutoDetectableComponentHandler annotation = handler.getClass().getAnnotation(AutoDetectableComponentHandler.class);
			List<String> keys = Arrays.asList(annotation.value());
			addComponentHandler(buttonHandlers, keys, handler, ButtonHandler.class);
			addComponentHandler(modalHandlers, keys, handler, ModalHandler.class);
			addComponentHandler(selectMenuHandlers, keys, handler, SelectMenuHandler.class);
		}
		dih4jda.addButtonHandlers(buttonHandlers);
		dih4jda.addModalHandlers(modalHandlers);
		dih4jda.addSelectMenuHandlers(selectMenuHandlers);
	}

	private <T> void addComponentHandler(Map<List<String>, T> handlers, List<String> keys, Object handler, Class<T> type) {
		if (type.isInstance(handler)) {
			T old = handlers.putIfAbsent(keys, type.cast(handler));
			if(old!=null) {
				throw new IllegalStateException("The same interaction name was registered multiple times");
			}
		}
	}
}

