package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.moderation.ModerationService;

/**
 * Utility class that contains several methods for managing utility interactions, such as ban, kick, warn, etc.
 */
public class InteractionUtils {

	/**
	 * Template Interaction ID for deleting the original Message.
	 */
	public static String DELETE_ORIGINAL_TEMPLATE = "utils:delete";

	/**
	 * Template Interaction ID for banning a single member from the current guild.
	 */
	public static String BAN_TEMPLATE = "utils:ban:%s";

	/**
	 * Template Interaction ID for unbanning a single user from the current guild.
	 */
	public static String UNBAN_TEMPLATE = "utils:unban:%s";

	/**
	 * Template Interaction ID for kicking a single member from the current guild.
	 */
	public static String KICK_TEMPLATE = "utils:kick:%s";

	/**
	 * Template Interaction ID for warning a single member in the current guild.
	 */
	public static String WARN_TEMPLATE = "utils:warn:%s";

	private InteractionUtils() {
	}

	/**
	 * Utility methods for interactions.
	 *
	 * @param id    The button's id, split by ":".
	 * @param event The {@link ButtonInteractionEvent} that is fired upon use.
	 */
	public static void handleButton(ButtonInteractionEvent event, String[] id) {
		event.deferEdit().queue();
		if (event.getGuild() == null) {
			Responses.error(event.getHook(), "This button may only be used in context of a server.").queue();
			return;
		}
		switch (id[1]) {
			case "delete" -> event.getHook().deleteOriginal().queue();
			case "kick" -> InteractionUtils.kick(event.getInteraction(), event.getGuild(), id[2]);
			case "ban" -> InteractionUtils.ban(event.getInteraction(), event.getGuild(), id[2]);
			case "unban" -> InteractionUtils.unban(event.getInteraction(), Long.parseLong(id[2]));
		}
	}

	private static void kick(ButtonInteraction interaction, Guild guild, String memberId) {
		ModerationService service = new ModerationService(interaction);
		guild.retrieveMemberById(memberId).queue(
				member -> {
					service.kick(member, "None", interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.editButton(interaction.getButton().withLabel("Kicked by " + interaction.getUser().getAsTag()).asDisabled()).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private static void ban(ButtonInteraction interaction, Guild guild, String memberId) {
		ModerationService service = new ModerationService(interaction);
		guild.retrieveMemberById(memberId).queue(
				member -> {
					service.ban(member, "None", interaction.getMember(), interaction.getMessageChannel(), false);
					interaction.editButton(interaction.getButton().withLabel("Banned by " + interaction.getUser().getAsTag()).asDisabled()).queue();
				}, error -> Responses.error(interaction.getHook(), "Could not find member: " + error.getMessage()).queue()
		);
	}

	private static void unban(ButtonInteraction interaction, long memberId) {
		ModerationService service = new ModerationService(interaction);
		service.unban(memberId, interaction.getMember(), interaction.getMessageChannel(), false);
		interaction.editButton(interaction.getButton().withLabel("Unbanned by " + interaction.getUser().getAsTag()).asDisabled()).queue();
	}
}
