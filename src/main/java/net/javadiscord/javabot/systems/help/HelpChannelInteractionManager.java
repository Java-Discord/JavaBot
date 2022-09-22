package net.javadiscord.javabot.systems.help;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Handles various interactions regarding the help channel system.
 */
@Slf4j
@RequiredArgsConstructor
@AutoDetectableComponentHandler({"help-channel", "help-thank"})
public class HelpChannelInteractionManager implements ButtonHandler {

	private final BotConfig botConfig;
	private final ScheduledExecutorService asyncPool;
	private final DbActions dbActions;
	private final HelpExperienceService helpExperienceService;

	/**
	 * Handles button interactions pertaining to the interaction provided to
	 * users when they choose to unreserve their channel, giving them options to
	 * thank helpers or cancel the unreserving.
	 *
	 * @param event         The button event.
	 * @param reservationId The help channel's reservation id.
	 * @param action        The data extracted from the button's id.
	 */
	private void handleHelpThankButton(@NotNull ButtonInteractionEvent event, String reservationId, String action) {
		event.deferEdit().queue();
		HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
		HelpChannelManager channelManager = new HelpChannelManager(botConfig, event.getGuild(), dbActions, asyncPool, helpExperienceService);
		Optional<ChannelReservation> optionalReservation = channelManager.getReservation(Long.parseLong(reservationId));
		if (optionalReservation.isEmpty()) {
			event.getInteraction().getHook().sendMessage("Could not find reservation data for this channel. Perhaps it's no longer reserved?")
					.setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}
		ChannelReservation reservation = optionalReservation.get();
		TextChannel channel = event.getChannel().asTextChannel();
		if (!event.isAcknowledged()) {
			event.deferReply(true).queue();
		}
		User owner = channelManager.getReservedChannelOwner(channel);
		if (owner == null) {
			event.getInteraction().getHook().sendMessage("Sorry, but this channel is currently unreserved.").setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}

		if (owner.getIdLong() != reservation.getUserId() || channel.getIdLong() != reservation.getChannelId()) {
			event.getInteraction().getHook().sendMessage("The reservation data for this channel doesn't match up with Discord's information.")
					.setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}

		if (event.getUser().equals(owner)) {
			if (action.equals("done")) {
				event.getMessage().delete().queue();
				channelManager.unreserveChannel(channel).queue();
			} else if (action.equals("cancel")) {
				event.getInteraction().getHook().sendMessage("Unreserving of this channel has been cancelled.").setEphemeral(true).queue();
				event.getMessage().delete().queue();
				try {
					channelManager.setTimeout(channel, config.getInactivityTimeouts().get(0));
				} catch (SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
				}
			} else {
				long helperId = Long.parseLong(action);
				thankHelper(event, channel, owner, helperId, reservation, channelManager);
			}
		} else {
			event.getInteraction().getHook().sendMessage("Sorry, only the person who reserved this channel can thank users.").setEphemeral(true).queue();
		}
	}

	private void thankHelper(@NotNull ButtonInteractionEvent event, TextChannel channel, User owner, long helperId, ChannelReservation reservation, HelpChannelManager channelManager) {
		Button btn = event.getButton();
		long thankCount = dbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE reservation_id = ? AND helper_id = ?",
				s -> {
					s.setLong(1, reservation.getId());
					s.setLong(2, helperId);
				}
		);
		if (thankCount > 0) {
			event.getInteraction().getHook().sendMessage("You can't thank someone twice!").setEphemeral(true).queue();
			if (btn != null) {
				event.editButton(btn.asDisabled()).queue();
			}
		} else {
			event.getJDA().retrieveUserById(helperId).queue(helper -> {
				// First insert the new thanks data.
				try {
					dbActions.update(
							"INSERT INTO help_channel_thanks (reservation_id, user_id, channel_id, helper_id) VALUES (?, ?, ?, ?)",
							reservation.getId(),
							owner.getIdLong(),
							channel.getIdLong(),
							helper.getIdLong()
					);
					event.getInteraction().getHook().sendMessageFormat("You thanked %s", helper.getAsTag()).setEphemeral(true).queue();
					HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
					// Perform experience transactions
					helpExperienceService.performTransaction(helper.getIdLong(), config.getThankedExperience(), HelpTransactionMessage.GOT_THANKED, event.getGuild());
					helpExperienceService.performTransaction(owner.getIdLong(), config.getThankExperience(), HelpTransactionMessage.THANKED_USER, event.getGuild());
				} catch (DataAccessException|SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					botConfig.get(event.getGuild()).getModerationConfig().getLogChannel().sendMessageFormat(
							"Could not record user %s thanking %s for help in channel %s: %s",
							owner.getAsTag(),
							helper.getAsTag(),
							channel.getAsMention(),
							e.getMessage()
					).queue();
				}
				// Then disable the button, or unreserve the channel if there's nobody else to thank.
				if (btn != null) {
					List<Button> activeButtons = event.getMessage().getButtons().stream()
							.filter(b -> !b.isDisabled() && !b.getLabel().equals("Unreserve") && !b.getLabel().equals("Cancel") && !b.equals(btn))
							.toList();
					if (activeButtons.isEmpty()) {// If there are no more people to thank, automatically unreserve the channel.
						event.getMessage().delete().queue();
						channelManager.unreserveChannel(channel).queue();
					} else {// Otherwise, disable the button.
						event.editButton(btn.asDisabled()).queue();
					}
				}
			});
		}
	}

	private void handleHelpChannelButton(@NotNull ButtonInteractionEvent event, String reservationId, String action) {
		event.deferEdit().queue();
		HelpChannelManager channelManager = new HelpChannelManager(botConfig, event.getGuild(),dbActions, asyncPool, helpExperienceService);
		Optional<ChannelReservation> optionalReservation = channelManager.getReservation(Long.parseLong(reservationId));
		if (optionalReservation.isEmpty()) {
			event.reply("Could not find reservation data for this channel. Perhaps it's no longer reserved?")
					.setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}
		ChannelReservation reservation = optionalReservation.get();
		TextChannel channel = event.getChannel().asTextChannel();
		if (!event.isAcknowledged()) {
			event.deferReply(true).queue();
		}
		User owner = channelManager.getReservedChannelOwner(channel);
		// If a reserved channel doesn't have an owner, it's in an invalid state, but the system will handle it later automatically.
		if (owner == null) {
			// Remove the original message, just to make sure no more interactions are sent.
			event.getInteraction().getHook().sendMessage("Uh oh! It looks like this channel is no longer reserved, so these buttons can't be used.")
					.setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}

		if (owner.getIdLong() != reservation.getUserId() || channel.getIdLong() != reservation.getChannelId()) {
			event.getInteraction().getHook().sendMessage("The reservation data for this channel doesn't match up with Discord's information.")
					.setEphemeral(true).queue();
			event.getMessage().delete().queue();
			return;
		}

		// Check that the user is allowed to do the interaction.
		if (
				event.getUser().equals(owner) ||
						event.getMember() != null && event.getMember().getRoles().contains(botConfig.get(event.getGuild()).getModerationConfig().getStaffRole())
		) {
			if (action.equals("done")) {
				event.getMessage().delete().queue();
				if (event.getUser().equals(owner)) { // If the owner is unreserving their own channel, handle it separately.
					channelManager.unreserveChannelByOwner(channel, owner, null, event);
				} else {
					channelManager.unreserveChannel(channel).queue();
				}
			} else if (action.equals("not-done")) {
				log.info("Removing timeout check message in {} because it was marked as not-done.", channel.getAsMention());
				event.getMessage().delete().queue();
				try {
					int nextTimeout = channelManager.getNextTimeout(channel);
					channelManager.setTimeout(channel, nextTimeout);
					channel.sendMessage(String.format(
							"Okay, we'll keep this channel reserved for you, and check again in **%d** minutes.",
							nextTimeout
					)).queue();
				} catch (SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					Responses.error(event.getHook(), "An error occurred while managing this help channel.").queue();
				}
			}
		} else {
			event.getInteraction().getHook().sendMessage("Sorry, only the person who reserved this channel or moderators are allowed to use these buttons.")
					.setEphemeral(true).queue();
		}
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		if (event.getUser().isBot()) return;
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		switch (id[0]) {
			case "help-channel" -> handleHelpChannelButton(event, id[1], id[2]);
			case "help-thank" -> handleHelpThankButton(event, id[1], id[2]);
		}
	}
}
