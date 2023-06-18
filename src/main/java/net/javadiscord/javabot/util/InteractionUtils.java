package net.javadiscord.javabot.util;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.interactions.components.StringSelectMenuHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.systems.notification.NotificationService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class that contains several methods for managing utility interactions, such as ban, kick, warn, etc.
 */
@AutoDetectableComponentHandler("utils")
@RequiredArgsConstructor
public class InteractionUtils implements ButtonHandler, ModalHandler, StringSelectMenuHandler {
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
	 * @return the {@link ReplyCallbackAction} for responding to the request
	 */
	@CheckReturnValue
	private InteractionCallbackAction<?> delete(@NotNull ButtonInteraction interaction) {
		Member member = interaction.getMember();
		if (member == null) {
			return Responses.warning(interaction, "Could not get member.");
		}
		GuildConfig config = botConfig.get(interaction.getGuild());
		Message msg = interaction.getMessage();
		if (
				member.getUser().getIdLong() == msg.getAuthor().getIdLong() ||
						member.getRoles().contains(config.getModerationConfig().getStaffRole()) ||
						member.isOwner()
		) {
			msg.delete().queue();
			return interaction.deferEdit();
		} else {
			return Responses.warning(interaction, "You don't have permission to delete this message.");
		}
	}

	private void kick(ModalInteraction interaction, @NotNull Guild guild, String memberId, String reason) {
		if(!interaction.getMember().hasPermission(Permission.KICK_MEMBERS)) {
			Responses.error(interaction.getHook(), "Missing permissions").queue();
			return;
		}
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		guild.retrieveMemberById(memberId).queue(
				member -> {
					service.kick(member.getUser(), reason, interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.getMessage().editMessageComponents(ActionRow.of(Button.danger(interaction.getModalId(), "Kicked by "+interaction.getUser().getAsTag()).asDisabled())).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}
	
	private void warn(ModalInteraction interaction, @NotNull Guild guild, String memberId, WarnSeverity severity, String reason) {
		if(!interaction.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
			Responses.error(interaction.getHook(), "Missing permissions").queue();
			return;
		}
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		guild.retrieveMemberById(memberId).queue(
				member -> {
					service.warn(member.getUser(), severity, reason, interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.getHook().editOriginalComponents(ActionRow.of(Button.primary(interaction.getModalId(), "Warned by "+interaction.getUser().getAsTag()).asDisabled())).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private void ban(ModalInteraction interaction, @NotNull Guild guild, String memberId, String reason) {
		if(!interaction.getMember().hasPermission(Permission.BAN_MEMBERS)) {
			Responses.error(interaction.getHook(), "Missing permissions").queue();
			return;
		}
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		guild.getJDA().retrieveUserById(memberId).queue(
				user -> {
					service.ban(user, reason, interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.getMessage().editMessageComponents(ActionRow.of(Button.danger(interaction.getModalId(), "Banned by "+interaction.getUser().getAsTag()).asDisabled())).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private void unban(ModalInteraction interaction, long memberId, String reason) {
		if(!interaction.getMember().hasPermission(Permission.BAN_MEMBERS)) {
			Responses.error(interaction.getHook(), "Missing permissions").queue();
			return;
		}
		ModerationService service = new ModerationService(notificationService, botConfig, interaction, warnRepository, asyncPool);
		service.unban(memberId, reason, interaction.getMember(), interaction.getMessageChannel(), false);
		interaction.getMessage().editMessageComponents(ActionRow.of(Button.secondary(interaction.getModalId(), "Unbanned by "+interaction.getUser().getAsTag()).asDisabled())).queue();
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if (event.getGuild() == null) {
			Responses.error(event.getHook(), "This button may only be used in context of a server.").queue();
			return;
		}
		
		(switch (id[1]) {
		case "delete" -> delete(event.getInteraction());
		case "kick" -> generateModal(event, "Kick user");
		case "ban" -> generateModal(event, "Ban user");
		case "unban" -> generateModal(event, "Unban user");
		case "warn" -> event.replyComponents(ActionRow.of(StringSelectMenu.create(event.getComponentId())
				.addOptions(Arrays.stream(WarnSeverity.values())
						.map(severity -> SelectOption.of(severity.toString(), severity.name()))
						.toArray(SelectOption[]::new))
				.build())).setEphemeral(true);
		default -> event.reply("Invalid action");
		}).queue();
	}

	private ModalCallbackAction generateModal(ButtonInteractionEvent event, String title) {
		return event.replyModal(Modal.create(event.getComponentId(), title)
				.addActionRow(TextInput.create("reason", "Reason", TextInputStyle.SHORT).setRequired(true).build())
				.build());
	}

	@Override
	public void handleModal(@Nonnull ModalInteractionEvent event, @Nonnull List<ModalMapping> mappings) {
		event.deferEdit().queue();
		String[] id = ComponentIdBuilder.split(event.getModalId());
		if (event.getGuild() == null) {
			Responses.error(event.getHook(), "This button may only be used in context of a server.").queue();
			return;
		}
		String reason = "None";
		WarnSeverity severity = id.length > 3 ? WarnSeverity.valueOf(id[3]) : null;
		
		for (ModalMapping mapping : mappings) {
			if ("reason".equals(mapping.getId())) {
				reason = mapping.getAsString();
			}
		}
		
		switch (id[1]) {
			case "kick" -> kick(event.getInteraction(), event.getGuild(), id[2], reason);
			case "ban" -> ban(event.getInteraction(), event.getGuild(), id[2], reason);
			case "warn" -> warn(event.getInteraction(), event.getGuild(), id[2], severity, reason);
			case "unban" -> unban(event.getInteraction(), Long.parseLong(id[2]), reason);
		}
	}

	@Override
	public void handleStringSelectMenu(@Nonnull StringSelectInteractionEvent event, @Nonnull List<String> options) {
		event.replyModal(Modal.create(event.getComponentId()+":"+options.get(0), "Warn user")
				.addActionRow(TextInput.create("reason", "Reason", TextInputStyle.SHORT).setRequired(true).build())
				.build())
			.queue();
	}
}
