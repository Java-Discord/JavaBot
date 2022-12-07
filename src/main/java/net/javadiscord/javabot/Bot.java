package net.javadiscord.javabot;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

import xyz.dynxsty.dih4jda.DIH4JDA;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.interactions.components.IdMapping;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.interactions.components.StringSelectMenuHandler;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.annotations.PreRegisteredListener;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.tasks.PresenceUpdater;

/**
 * The main class where the bot is initialized.
 */
@SpringBootApplication
@ComponentScan(
	includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SlashCommand.class, ContextCommand.class, ListenerAdapter.class, Subcommand.class }),
	excludeFilters = @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, classes = PresenceUpdater.class)
)
@EnableScheduling
@RequiredArgsConstructor
public class Bot {

	private final DIH4JDA dih4jda;
	private final BotConfig config;
	private final List<SlashCommand> commands;
	private final List<ContextCommand<?>> contextCommands;
	private final List<ListenerAdapter> listeners;
	private final ApplicationContext ctx;


	private void addEventListeners(final List<ListenerAdapter> listeners) {
		for (ListenerAdapter listener : listeners) {
			if(!(listener.getClass().isAnnotationPresent(PreRegisteredListener.class))) {
				dih4jda.getJDA().addEventListener(listener);
			}
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
		dih4jda.registerInteractions();
		addEventListeners(listeners);
		registerComponentHandlers(ctx);
	}

	/**
	 * The main method that starts the bot. This involves a few steps:
	 * <ol>
	 *     <li>Setting the time zone to UTC, to keep our sanity when working with times.</li>
	 *     <li>Loading the configuration JSON file.</li>
	 *     <li>Creating and configuring the {@link net.dv8tion.jda.api.JDA} instance that enables the bots' Discord connectivity.</li>
	 *     <li>Initializing the {@link DIH4JDA} instance.</li>
	 *     <li>Adding event listeners to the bot.</li>
	 * </ol>
	 *
	 * @param args Command-line arguments.
	 * @throws Exception If any exception occurs during bot creation.
	 */
	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
		MessageRequest.setDefaultMentions(EnumSet.of(Message.MentionType.ROLE, Message.MentionType.CHANNEL, Message.MentionType.USER, Message.MentionType.EMOJI));
		SpringApplication.run(Bot.class, args);
	}

	private void registerComponentHandlers(@NotNull ApplicationContext ctx) {
		Map<String, Object> interactionHandlers = ctx.getBeansWithAnnotation(AutoDetectableComponentHandler.class);
		List<IdMapping<ButtonHandler>> buttonMappings = new ArrayList<>();
		List<IdMapping<ModalHandler>> modalMappings = new ArrayList<>();
		List<IdMapping<StringSelectMenuHandler>> stringSelectMappings = new ArrayList<>();
		for (Object handler : interactionHandlers.values()) {
			AutoDetectableComponentHandler annotation = handler.getClass().getAnnotation(AutoDetectableComponentHandler.class);
			String[] keys = annotation.value();
			addComponentHandler(buttonMappings, keys, handler, ButtonHandler.class);
			addComponentHandler(modalMappings, keys, handler, ModalHandler.class);
			addComponentHandler(stringSelectMappings, keys, handler, StringSelectMenuHandler.class);
		}
		dih4jda.addButtonMappings(buttonMappings.toArray(IdMapping[]::new));
		dih4jda.addModalMappings(modalMappings.toArray(IdMapping[]::new));
		dih4jda.addStringSelectMenuMappings(stringSelectMappings.toArray(IdMapping[]::new));
	}

	private <T> void addComponentHandler(List<IdMapping<T>> handlers, String[] keys, Object handler, @NotNull Class<T> type) {
		if (type.isInstance(handler)) {
			handlers.add(IdMapping.of(type.cast(handler), keys));
		}
	}
}

