package net.javadiscord.javabot.util;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.notification.NotificationService;

import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class that contains several methods for managing utility interactions, such as ban, kick, warn, etc.
 */
@AutoDetectableComponentHandler("utils")
@RequiredArgsConstructor
public class InteractionUtils implements ButtonHandler {
	/**
	 * Template Interaction ID for deleting the original Message.
	 */
	public static final String DELETE_ORIGINAL_TEMPLATE = "utils:delete";
	/**
	 * Template Interaction ID for banning a single member from the current guild.
	 */
	public static final String BAN_TEMPLATE = "utils:ban:%s";
	/**
	 * Template Interaction ID for unbanning a single user from the current guild.
	 */
	public static final String UNBAN_TEMPLATE = "utils:unban:%s";
	/**
	 * Template Interaction ID for kicking a single member from the current guild.
	 */
	public static final String KICK_TEMPLATE = "utils:kick:%s";
	/**
	 * Template Interaction ID for warning a single member in the current guild.
	 */
	public static final String WARN_TEMPLATE = "utils:warn:%s";

	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * Deletes a message, only if the person deleting the message is the author
	 * of the message, a staff member, or the owner.
	 *
	 * @param interaction The button interaction.
	 */
	private void delete(@NotNull ButtonInteraction interaction) {
		Member member = interaction.getMember();
		if (member == null) {
			Responses.warning(interaction.getHook(), "Could not get member.").queue();
			return;
		}
		GuildConfig config = botConfig.get(interaction.getGuild());
		Message msg = interaction.getMessage();
		if (
				member.getUser().getIdLong() == msg.getAuthor().getIdLong() ||
						member.getRoles().contains(config.getModerationConfig().getStaffRole()) ||
						member.isOwner()
		) {
			msg.delete().queue();
		} else {
			Responses.warning(interaction.getHook(), "You don't have permission to delete this message.").queue();
		}
	}

	private void kick(ButtonInteraction interaction, @NotNull Guild guild, String memberId) {
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		guild.retrieveMemberById(memberId).queue(
				member -> {
					service.kick(member.getUser(), "None", interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.editButton(interaction.getButton().withLabel("Kicked by " + interaction.getUser().getAsTag()).asDisabled()).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private void ban(ButtonInteraction interaction, @NotNull Guild guild, String memberId) {
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		guild.getJDA().retrieveUserById(memberId).queue(
				user -> {
					service.ban(user, "None", interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.editButton(interaction.getButton().withLabel("Banned by " + interaction.getUser().getAsTag()).asDisabled()).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private void unban(ButtonInteraction interaction, long memberId) {
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		service.unban(memberId, "None", interaction.getMember(), interaction.getMessageChannel(), false);
		interaction.editButton(interaction.getButton().withLabel("Unbanned by " + interaction.getUser().getAsTag()).asDisabled()).queue();
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		event.deferEdit().queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if (event.getGuild() == null) {
			Responses.error(event.getHook(), "This button may only be used in context of a server.").queue();
			return;
		}
		switch (id[1]) {
			case "delete" -> delete(event.getInteraction());
			case "kick" -> kick(event.getInteraction(), event.getGuild(), id[2]);
			case "ban" -> ban(event.getInteraction(), event.getGuild(), id[2]);
			case "unban" -> unban(event.getInteraction(), Long.parseLong(id[2]));
		}
	}
}
