package net.javadiscord.javabot.systems.help.forum;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.config.guild.HelpForumConfig;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

/**
 * Listens for all events releated to the forum help channel system.
 */
public class ForumHelpListener extends ListenerAdapter implements ButtonHandler {

	/**
	 * A static Map that holds all messages that was sent in a specific reserved forum channel.
	 */
	public static final Map<Long, List<Message>> HELP_POST_MESSAGES = new HashMap<>();

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getMessage().getAuthor().isSystem() || event.getMessage().getAuthor().isBot()) {
			return;
		}
		// check for forum post
		if (isInvalidForumPost(event.getChannel())) {
			return;
		}
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (isInvalidHelpForumChannel(post.getParentChannel().asForumChannel())) {
			return;
		}
		// cache messages
		List<Message> messages = new ArrayList<>();
		messages.add(event.getMessage());
		if (HELP_POST_MESSAGES.containsKey(post.getIdLong())) {
			messages.addAll(HELP_POST_MESSAGES.get(post.getIdLong()));
		}
		HELP_POST_MESSAGES.put(post.getIdLong(), messages);
	}

	@Override
	public void onChannelCreate(@NotNull ChannelCreateEvent event) {
		if (event.getGuild() == null || isInvalidForumPost(event.getChannel())) {
			return;
		}
		HelpForumConfig config = Bot.getConfig().get(event.getGuild()).getHelpForumConfig();
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (isInvalidHelpForumChannel(post.getParentChannel().asForumChannel())) {
			return;
		}
		// send post buttons
		post.sendMessageComponents(ActionRow.of(
				Button.primary(ComponentIdBuilder.build(ForumHelpManager.HELP_CLOSE_IDENTIFIER, post.getIdLong()), "Close Post"),
				Button.secondary(ComponentIdBuilder.build(ForumHelpManager.HELP_GUIDELINES_IDENTIFIER), "Help Guidelines").withEmoji(Emoji.fromUnicode("📚"))
		)).queue(success -> {
			// send /close reminder (if enabled)
			UserPreferenceService service = new UserPreferenceService(Bot.getDataSource());
			UserPreference preference = service.getOrCreate(post.getOwnerIdLong(), Preference.FORUM_CLOSE_REMINDER);
			if (Boolean.parseBoolean(preference.getState())) {
				post.sendMessageFormat(config.getCloseReminderText(), UserSnowflake.fromId(post.getOwnerIdLong()).getAsMention()).queue();
			}
		});
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if (isInvalidForumPost(event.getChannel()) ||
				isInvalidHelpForumChannel(event.getChannel().asThreadChannel().getParentChannel().asForumChannel())
		) {
			Responses.error(event, "This button may only be used inside help forum threads.").queue();
			return;
		}
		ThreadChannel post = event.getChannel().asThreadChannel();
		ForumHelpManager manager = new ForumHelpManager(post);
		switch (id[0]) {
			case ForumHelpManager.HELP_THANKS_IDENTIFIER -> handleHelpThanksInteraction(event, manager, id);
			case ForumHelpManager.HELP_GUIDELINES_IDENTIFIER -> handleReplyGuidelines(event, post.getParentChannel().asForumChannel());
			case ForumHelpManager.HELP_CLOSE_IDENTIFIER -> handlePostClose(event, manager);
		}
	}

	private boolean isInvalidForumPost(@NotNull MessageChannelUnion union) {
		return union.getType() != ChannelType.GUILD_PUBLIC_THREAD ||
				union.asThreadChannel().getParentChannel().getType() != ChannelType.FORUM;
	}

	private boolean isInvalidForumPost(@NotNull ChannelUnion union) {
		return union.getType() != ChannelType.GUILD_PUBLIC_THREAD ||
				union.asThreadChannel().getParentChannel().getType() != ChannelType.FORUM;
	}

	private boolean isInvalidHelpForumChannel(@NotNull ForumChannel forum) {
		HelpForumConfig config = Bot.getConfig().get(forum.getGuild()).getHelpForumConfig();
		return config.getHelpForumChannelId() != forum.getIdLong();
	}

	private void handleHelpThanksInteraction(@NotNull ButtonInteractionEvent event, @NotNull ForumHelpManager manager, String @NotNull [] id) {
		ThreadChannel post = manager.postThread();
		HelpConfig config = Bot.getConfig().get(event.getGuild()).getHelpConfig();
		switch (id[2]) {
			case "done" -> {
				List<Button> buttons = event.getMessage().getButtons();
				// immediately delete the message
				event.getMessage().delete().queue(s -> {
					// close post
					manager.close(event, false, null);
					// add experience
					try {
						HelpExperienceService service = new HelpExperienceService(Bot.getDataSource());
						Map<Long, Double> experience = HelpChannelManager.calculateExperience(HELP_POST_MESSAGES.get(post.getIdLong()), post.getOwnerIdLong(), config);
						for (Map.Entry<Long, Double> entry : experience.entrySet()) {
							service.performTransaction(entry.getKey(), entry.getValue(), HelpTransactionMessage.HELPED, config.getGuild());
						}
					} catch (SQLException e) {
						ExceptionLogger.capture(e, getClass().getName());
					}
					// thank all helpers
					buttons.stream().filter(ActionComponent::isDisabled)
							.filter(b -> b.getId() != null)
							.forEach(b -> manager.thankHelper(event, post, Long.parseLong(ComponentIdBuilder.split(b.getId())[2])));
				});

			}
			case "cancel" -> event.getMessage().delete().queue();
			default -> event.editButton(event.getButton().asDisabled()).queue();
		}
	}

	private void handleReplyGuidelines(@NotNull IReplyCallback callback, @NotNull ForumChannel channel) {
		callback.replyEmbeds(new EmbedBuilder()
						.setTitle("Help Guidelines")
						.setDescription(channel.getTopic())
						.build()
				).setEphemeral(true)
				.queue();
	}

	private void handlePostClose(ButtonInteractionEvent event, @NotNull ForumHelpManager manager) {
		if (manager.isForumEligibleToBeUnreserved(event)) {
			manager.close(event, event.getUser().getIdLong() == manager.postThread().getOwnerIdLong(), null);
		} else {
			Responses.warning(event, "Could not close this post", "You're not allowed to close this post.").queue();
		}
	}
}