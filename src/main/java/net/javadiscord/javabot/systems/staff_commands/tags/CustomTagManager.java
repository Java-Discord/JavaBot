package net.javadiscord.javabot.systems.staff_commands.tags;

import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.javadiscord.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to manage the so-called "Custom Commands", which are
 * basically customizable Slash Commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomTagManager {
	private static final Map<Long, Set<CustomTag>> LOADED_TAGS;

	static {
		LOADED_TAGS = new HashMap<>();
	}

	private final CustomTagRepository customTagRepository;

	/**
	 * Cleans the given String by removing all whitespaces and slashes, so it can be used for custom tags.
	 *
	 * @param s The string that should be cleaned.
	 * @return The cleaned string.
	 */
	public static @NotNull String cleanString(@NotNull String s) {
		return s.trim()
				.toLowerCase()
				.replaceAll("\\s+", "-")
				.replace("/", "");
	}

	/**
	 * Replies with all available custom tags.
	 *
	 * @param guild The current {@link Guild}.
	 * @param text The text choices need to match
	 * @return A {@link List} with all Option Choices.
	 */
	public static @NotNull List<Command.Choice> replyTags(@NotNull Guild guild, String text) {
		List<Command.Choice> choices = new ArrayList<>(25);
		for (CustomTag command : LOADED_TAGS.get(guild.getIdLong())) {
			if (choices.size() < 26 && command.getName().toLowerCase().contains(text)) {
				choices.add(new Command.Choice(command.getName(), command.getName()));
			}
		}
		return choices;
	}

	public static @NotNull AutoCompleteCallbackAction handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event) {
		return event.replyChoices(AutoCompleteUtils.filterChoices(event, replyTags(event.getGuild(), event.getFocusedOption().getValue().toLowerCase())));
	}

	/**
	 * Handles a single {@link CustomTag}.
	 *
	 * @param event The {@link SlashCommandInteractionEvent} which was fired.
	 * @param tag   The corresponding {@link CustomTag}.
	 * @return A {@link RestAction}.
	 */
	public static @NotNull RestAction<?> handleCustomTag(@NotNull SlashCommandInteractionEvent event, @NotNull CustomTag tag) {
		Set<RestAction<?>> actions = new HashSet<>();
		if (tag.isEmbed()) {
			if (tag.isReply()) {
				actions.add(event.getHook().sendMessageEmbeds(tag.toEmbed()));
			} else {
				actions.add(event.getChannel().sendMessageEmbeds(tag.toEmbed()));
			}
		} else {
			if (tag.isReply()) {
				actions.add(event.getHook().sendMessage(tag.getResponse()).setAllowedMentions(List.of()));
			} else {
				actions.add(event.getChannel().sendMessage(tag.getResponse()).setAllowedMentions(List.of()));
			}
		}
		if (!tag.isReply()) {
			actions.add(event.reply("Done!").setEphemeral(true));
		}
		return RestAction.allOf(actions);
	}

	/**
	 * Iterates through all guilds and loads their corresponding {@link CustomTag}s.
	 * @param jda The main {@link JDA} instance
	 * @throws SQLException If an error occurs.
	 */
	public void init(JDA jda) throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			Set<CustomTag> tags = LOADED_TAGS.put(guild.getIdLong(), getCustomTags(guild.getIdLong()));
			if (tags != null && !tags.isEmpty()) {
				log.info("Loaded {} Custom Tags for Guild \"{}\": {}", tags.size(), guild.getName(),
						tags.stream().map(CustomTag::getName).collect(Collectors.joining(", ")));
			}
		}
	}

	/**
	 * Gets all {@link CustomTag}s (from the database) for the specified {@link Guild}.
	 *
	 * @param guildId The guilds' id.
	 * @return A {@link Set} of all {@link CustomTag}s for the current guild.
	 * @throws SQLException If an error occurs.
	 */
	@Contract("_ -> new")
	private @NotNull Set<CustomTag> getCustomTags(long guildId) throws DataAccessException {
		return new HashSet<>(customTagRepository.getCustomTagsByGuildId(guildId));
	}

	/**
	 * Gets all loaded {@link CustomTag}s for the specified {@link Guild}.
	 *
	 * @param guildId The {@link Guild}s id.
	 * @return An unmodifiable {@link Set} of all {@link CustomTag}s for the current guild.
	 */
	public Set<CustomTag> getLoadedCommands(long guildId) {
		return LOADED_TAGS.get(guildId);
	}

	/**
	 * Attempts to get a {@link CustomTag}, based on the specified name.
	 *
	 * @param guildId The guilds' id.
	 * @param name    The tag's name.
	 * @return An {@link Optional} which may contains the desired {@link CustomTag}.
	 */
	public Optional<CustomTag> getByName(long guildId, String name) {
		return getLoadedCommands(guildId).stream()
				.filter(c -> c.getName().equalsIgnoreCase(name))
				.findFirst();
	}

	/**
	 * Creates a new {@link CustomTag} for the specified guild.
	 *
	 * @param guild The {@link Guild}.
	 * @param tag   The {@link CustomTag} to create.
	 * @return Whether the command was successfully created/added.
	 * @throws SQLException If an error occurs.
	 */
	public boolean addCommand(@NotNull Guild guild, @NotNull CustomTag tag) throws DataAccessException {
		if (doesTagExist(guild.getIdLong(), tag.getName())) {
			return false;
		}
		Set<CustomTag> tags = new HashSet<>(LOADED_TAGS.get(guild.getIdLong()));
		tags.add(tag);
		LOADED_TAGS.put(guild.getIdLong(), tags);
		log.info("Created Custom Tag in guild \"{}\": {}", guild.getName(), tag.getName());
		return customTagRepository.insert(tag) != null;
	}

	/**
	 * Removes a single {@link CustomTag} from the specified guild.
	 *
	 * @param guildId The guilds' id.
	 * @param tag     The {@link CustomTag} to delete.
	 * @return Whether the command was successfully deleted.
	 * @throws SQLException If an error occurs.
	 */
	public boolean removeCommand(long guildId, @NotNull CustomTag tag) throws DataAccessException {
		if (!doesTagExist(guildId, tag.getName())) {
			return false;
		}
		customTagRepository.delete(tag);
		LOADED_TAGS.put(guildId, new HashSet<>(getCustomTags(guildId)));
		log.info("Deleted Custom Tag in guild \"{}\": {}", guildId, tag);
		return true;
	}

	/**
	 * Edits and updates a single {@link CustomTag}.
	 *
	 * @param guildId The guilds' id.
	 * @param old     The "old" {@link CustomTag}.
	 * @param update  The "new" and updated {@link CustomTag} object.
	 * @return Whether the command was successfully edited.
	 * @throws SQLException If an error occurs.
	 */
	public boolean editCommand(long guildId, @NotNull CustomTag old, @NotNull CustomTag update) throws DataAccessException {
		if (!doesTagExist(guildId, old.getName())) {
			return false;
		}
		customTagRepository.edit(old, update);
		LOADED_TAGS.put(guildId, new HashSet<>(getCustomTags(guildId)));
		log.info("Edited Custom Tag in guild \"{}\": {} -> {}", guildId, old, update);
		return true;
	}

	/**
	 * Checks whether a single {@link CustomTag} already exists in the specified guild.
	 *
	 * @param guildId The guild's id.
	 * @param tagName The tag's name.
	 * @return Whether a {@link CustomTag} already exists with that name.
	 */
	private boolean doesTagExist(long guildId, String tagName) {
		Set<CustomTag> tags = LOADED_TAGS.get(guildId);
		return tags != null && tags.stream().anyMatch(c -> c.getName().equalsIgnoreCase(tagName));
	}
}
