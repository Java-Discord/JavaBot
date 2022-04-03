package net.javadiscord.javabot.systems.staff.self_roles.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.GuildUtils;
import net.javadiscord.javabot.util.MessageActionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that creates a new Reaction Role/Button.
 */
@Slf4j
public class CreateSelfRoleSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var typeOption = event.getOption("type");
		var roleOption = event.getOption("role");
		var descriptionOption = event.getOption("description");
		var permanentOption = event.getOption("permanent");
		var messageIdOption = event.getOption("message-id");
		if (typeOption == null || roleOption == null || descriptionOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		String type = typeOption.getAsString();
		Role role = roleOption.getAsRole();
		String buttonLabel = switch (type) {
			case "STAFF", "EXPERT" -> String.format("Apply for \"%s\"", role.getName());
			default -> String.format("Get \"%s\"", role.getName());
		};
		String description = descriptionOption.getAsString();
		boolean permanent = permanentOption != null && permanentOption.getAsBoolean();
		var config = Bot.config.get(event.getGuild());
		if (messageIdOption == null || type.equals("NONE")) {
			event.getChannel().sendMessageEmbeds(this.buildSelfRoleEmbed(role, description, config.getSlashCommand())).queue(
					message -> this.addSelfRoleButton(event, message, type, role, permanent, buttonLabel, config),
					e -> Responses.error(event.getHook(), e.getMessage()));
		} else {
			event.getChannel().retrieveMessageById(messageIdOption.getAsString()).queue(
					message -> this.addSelfRoleButton(event, message, type, role, permanent, buttonLabel, config),
					e -> Responses.error(event.getHook(), e.getMessage()));
		}
		return event.deferReply(true);
	}

	/**
	 * Adds the Self-Role Button to the given Message.
	 *
	 * @param event     The {@link SlashCommandInteractionEvent} that is fired upon using a Slash Command.
	 * @param message   The message the button should be added to.
	 * @param type      The self-role's type.
	 * @param role      The role.
	 * @param permanent Whether the user should be able to remove the role again.
	 * @param label     The button's label.
	 * @param config    The {@link GuildConfig} of the current Guild.
	 */
	private void addSelfRoleButton(SlashCommandInteractionEvent event, Message message, String type, Role role, boolean permanent, String label, GuildConfig config) {
		if (!type.equals("NONE")) {
			List<Button> buttons = new ArrayList<>();
			for (ActionRow actionRow : message.getActionRows()) {
				buttons.addAll(actionRow.getButtons());
			}
			buttons.add(Button.secondary(this.buildButtonId(type, role, permanent), label));
			message.editMessageComponents(MessageActionUtils.toActionRows(buttons)).queue();
		}
		MessageEmbed logEmbed = this.buildSelfRoleCreateEmbed(event.getUser(), role, event.getChannel(), message.getJumpUrl(), type, config.getSlashCommand());
		GuildUtils.getLogChannel(event.getGuild()).sendMessageEmbeds(logEmbed).queue();
		event.getHook().sendMessageEmbeds(logEmbed).setEphemeral(true).queue();
	}

	/**
	 * Constructs the Button id by combining the self role's type, the role's id and the boolean.
	 *
	 * @param type      The self role's type.
	 * @param role      The role.
	 * @param permanent Whether the user should be able to remove the role again.
	 * @return The complete button id.
	 */
	private String buildButtonId(String type, Role role, boolean permanent) {
		return String.format("self-role:%s:%s:%s", type.toLowerCase(), role.getId(), permanent);
	}

	private MessageEmbed buildSelfRoleEmbed(Role role, String description, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setDescription(String.format("%s\n%s", role.getAsMention(), description))
				.setColor(config.getDefaultColor())
				.build();
	}

	private MessageEmbed buildSelfRoleCreateEmbed(User createdBy, Role role, Channel channel, String jumpUrl, String type, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getAsTag(), jumpUrl, createdBy.getEffectiveAvatarUrl())
				.setTitle("Self Role created")
				.setColor(config.getDefaultColor())
				.addField("Channel", channel.getAsMention(), true)
				.addField("Role", role.getAsMention(), true)
				.addField("Type", String.format("`%s`", type), true)
				.addField("Message", String.format("[Jump to Message](%s)", jumpUrl), false)
				.setTimestamp(Instant.now())
				.build();
	}
}
