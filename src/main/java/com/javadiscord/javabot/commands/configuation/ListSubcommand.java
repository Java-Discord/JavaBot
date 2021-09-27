package com.javadiscord.javabot.commands.configuation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.properties.config.GuildConfig;
import com.javadiscord.javabot.properties.config.ReflectionUtils;
import com.javadiscord.javabot.properties.config.UnknownPropertyException;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ListSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		try {
			var results = ReflectionUtils.getFields(null, GuildConfig.class);
			String msg = results.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> {
						Object propertyValue = null;
						try {
							propertyValue = Bot.config.get(event.getGuild()).resolve(entry.getKey());
						} catch (UnknownPropertyException e) {
							e.printStackTrace();
						}
						return String.format(
								"**%s** `%s` = `%s`",
								entry.getValue().getSimpleName(),
								entry.getKey(),
								propertyValue
						);
					})
					.collect(Collectors.joining("\n"));
			return Responses.info(event, "Configuration Properties", msg);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return Responses.error(event, e.getMessage());
		}
	}
}
