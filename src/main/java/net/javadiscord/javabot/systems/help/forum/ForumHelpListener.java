package net.javadiscord.javabot.systems.help.forum;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.*;

import javax.sql.DataSource;

/**
 * Listens for all events releated to the forum help channel system.
 */
@RequiredArgsConstructor
public class ForumHelpListener extends ListenerAdapter implements ButtonHandler {

	/**
	 * A static Map that holds all messages that was sent in a specific reserved forum channel.
	 */
	public static final Map<Long, List<Message>> HELP_POST_MESSAGES = new HashMap<>();

	private final BotConfig botConfig;
	private final DataSource dataSource;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;
	private final DbActions dbActions;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getMessage().getAuthor().isSystem() || event.getMessage().getAuthor().isBot()) {
			return;
		}
		// check for guild & channel type
		if (!event.isFromGuild() || event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
			return;
		}
		// get post & check parent channel
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (post.getParentChannel().getType() != ChannelType.FORUM) {
			return;
		}
		ForumChannel forum = post.getParentChannel().asForumChannel();
		GuildConfig config = botConfig.get(event.getGuild());
		// check for channel id
		if (forum.getIdLong() != config.getHelpForumConfig().getHelpForumChannelId()) {
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
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD
				|| event.getChannel().asThreadChannel().getParentChannel().getType() != ChannelType.FORUM) {
			Responses.error(event, "This button may only be used inside help forum threads.").queue();
			return;
		}
		ForumHelpManager manager = new ForumHelpManager(event.getChannel().asThreadChannel(), dbActions, botConfig, dataSource, helpAccountRepository, helpTransactionRepository);
		switch (id[0]) {
			case ForumHelpManager.HELP_THANKS_IDENTIFIER -> handleHelpThanksInteraction(event, manager, id);
		}
	}

	private void handleHelpThanksInteraction(@NotNull ButtonInteractionEvent event, @NotNull ForumHelpManager manager, String @NotNull [] id) {
		ThreadChannel post = manager.getPostThread();
		HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
		switch (id[2]) {
			case "done" -> {
				List<Button> buttons = event.getMessage().getButtons();
				// immediately delete the message
				event.getMessage().delete().queue(s -> {
					// close post
					manager.close(event, false, null);
					// add experience
					try {
						HelpExperienceService service = new HelpExperienceService(dataSource, botConfig, helpAccountRepository, helpTransactionRepository);
						Map<Long, Double> experience = HelpChannelManager.calculateExperience(HELP_POST_MESSAGES.get(post.getIdLong()), post.getOwnerIdLong(), config);
						for (Map.Entry<Long, Double> entry : experience.entrySet()) {
							service.performTransaction(entry.getKey(), entry.getValue(), HelpTransactionMessage.HELPED, config.getGuild());
						}
					} catch (DataAccessException e) {
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
}