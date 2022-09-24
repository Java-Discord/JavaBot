package net.javadiscord.javabot.systems.staff_commands.self_roles;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the `/self-role create` command.
 */
public class CreateSelfRoleSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public CreateSelfRoleSubcommand() {
		setSubcommandData(new SubcommandData("create", "Creates a reaction role")
				.addOptions(
						new OptionData(OptionType.STRING, "type", "The self-role's type.", true)
								.addChoice("None (Just creates the embed)", "NONE")
								.addChoice("Default", "DEFAULT")
								.addChoice("Staff Application", "STAFF")
								.addChoice("Expert Application", "EXPERT"),
						new OptionData(OptionType.ROLE, "role", "The role the button should add upon use.", true),
						new OptionData(OptionType.STRING, "description", "The embed's description. This should not be longer than 4096 characters.", true),
						new OptionData(OptionType.BOOLEAN, "permanent", "Whether the user should be able to remove the role again. This defaults to 'false'.", false),
						new OptionData(OptionType.STRING, "message-id", "If set, adds the button to the given message instead of creating a new embed message.", false)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping typeMapping = event.getOption("type");
		OptionMapping roleMapping = event.getOption("role");
		OptionMapping descriptionMapping = event.getOption("description");
		boolean permanent = event.getOption("permanent", false, OptionMapping::getAsBoolean);
		OptionMapping messageIdOption = event.getOption("message-id");
		if (typeMapping == null || roleMapping == null || descriptionMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		String type = typeMapping.getAsString();
		Role role = roleMapping.getAsRole();
		String buttonLabel = switch (type) {
			case "STAFF", "EXPERT" -> String.format("Apply for \"%s\"", role.getName());
			default -> String.format("Get \"%s\"", role.getName());
		};
		String description = descriptionMapping.getAsString();
		event.deferReply(true).queue();
		if (messageIdOption == null || type.equals("NONE")) {
			event.getChannel().sendMessageEmbeds(buildSelfRoleEmbed(role, description)).queue(
					message -> addSelfRoleButton(event, message, type, role, permanent, buttonLabel),
					e -> Responses.error(event.getHook(), e.getMessage()));
		} else {
			event.getChannel().retrieveMessageById(messageIdOption.getAsString()).queue(
					message -> addSelfRoleButton(event, message, type, role, permanent, buttonLabel),
					e -> Responses.error(event.getHook(), e.getMessage()));
		}
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
	 */
	private void addSelfRoleButton(SlashCommandInteractionEvent event, Message message, @NotNull String type, Role role, boolean permanent, String label) {
		if (!type.equals("NONE")) {
			List<Button> buttons = new ArrayList<>();
			for (ActionRow actionRow : message.getActionRows()) {
				buttons.addAll(actionRow.getButtons());
			}
			buttons.add(Button.secondary(this.buildButtonId(type, role, permanent), label));
			message.editMessageComponents(MessageActionUtils.toActionRows(buttons)).queue();
		}
		MessageEmbed logEmbed = this.buildSelfRoleCreateEmbed(event.getUser(), role, event.getChannel(), message.getJumpUrl(), type);
		NotificationService.withGuild(event.getGuild()).sendToModerationLog(c -> c.sendMessageEmbeds(logEmbed));
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
	private String buildButtonId(@NotNull String type, @NotNull Role role, boolean permanent) {
		return String.format("self-role:%s:%s:%s", type.toLowerCase(), role.getId(), permanent);
	}

	private @NotNull MessageEmbed buildSelfRoleEmbed(@NotNull Role role, String description) {
		return new EmbedBuilder()
				.setDescription(String.format("%s\n%s", role.getAsMention(), description))
				.setColor(Responses.Type.DEFAULT.getColor())
				.build();
	}

	private @NotNull MessageEmbed buildSelfRoleCreateEmbed(@NotNull User createdBy, @NotNull Role role, @NotNull Channel channel, String jumpUrl, String type) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getAsTag(), jumpUrl, createdBy.getEffectiveAvatarUrl())
				.setTitle("Self Role created")
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Channel", channel.getAsMention(), true)
				.addField("Role", role.getAsMention(), true)
				.addField("Type", String.format("`%s`", type), true)
				.addField("Message", String.format("[Jump to Message](%s)", jumpUrl), false)
				.setTimestamp(Instant.now())
				.build();
	}
}
