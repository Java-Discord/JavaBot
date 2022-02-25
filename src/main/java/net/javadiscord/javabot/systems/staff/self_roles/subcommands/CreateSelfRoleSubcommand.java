package net.javadiscord.javabot.systems.staff.self_roles.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.GuildUtils;

import java.time.Instant;

/**
 * Subcommand that creates a new Reaction Role/Button.
 */
@Slf4j
public class CreateSelfRoleSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var typeOption = event.getOption("type");
		var roleOption = event.getOption("role");
		var descriptionOption = event.getOption("description");
		var permanentOption = event.getOption("permanent");
		if (typeOption == null || roleOption == null || descriptionOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		String type = typeOption.getAsString();
		String buttonLabel = switch (type) {
			case "STAFF", "EXPERT" -> "Apply";
			default -> "Get this Role";
		};
		Role role = roleOption.getAsRole();
		String description = descriptionOption.getAsString();
		boolean permanent = permanentOption != null && permanentOption.getAsBoolean();
		var config = Bot.config.get(event.getGuild());
		event.getChannel().sendMessageEmbeds(buildSelfRoleEmbed(role, description, config.getSlashCommand())).queue(message -> {
			Button roleButton = Button.secondary(buttonId(type, role, permanent), buttonLabel);
			message.editMessageComponents(ActionRow.of(roleButton)).queue(edit -> {
				MessageEmbed logEmbed = buildSelfRoleCreateEmbed(event.getUser(), role, event.getChannel(), edit.getJumpUrl(), type, config.getSlashCommand());
				GuildUtils.getLogChannel(event.getGuild()).sendMessageEmbeds(logEmbed).queue();
				event.getHook().sendMessageEmbeds(logEmbed).setEphemeral(true).queue();
			}, e -> Responses.error(event.getHook(), e.getMessage()));
		}, e -> Responses.error(event.getHook(), e.getMessage()));
		return event.deferReply(true);
	}

	private String buttonId(String type, Role role, boolean permanent) {
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
