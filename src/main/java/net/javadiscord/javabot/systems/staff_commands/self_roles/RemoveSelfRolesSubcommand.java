package net.javadiscord.javabot.systems.staff_commands.self_roles;

import net.javadiscord.javabot.util.UserUtils;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Subcommands that removes all Elements on all ActionRows.
 */
public class RemoveSelfRolesSubcommand extends SlashCommand.Subcommand {
	private final NotificationService notificationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 */
	public RemoveSelfRolesSubcommand(NotificationService notificationService) {
		this.notificationService = notificationService;
		setCommandData(new SubcommandData("remove-all", "Removes all Self-Roles from a specified message.")
				.addOption(OptionType.STRING, "message-id", "Id of the message.", true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("message-id");
		if (idMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		event.deferReply(true).queue();
		event.getChannel().retrieveMessageById(idMapping.getAsLong()).queue(message -> {
			message.editMessageComponents().queue();
			MessageEmbed embed = buildSelfRoleDeletedEmbed(event.getUser(), message);
			notificationService.withGuild(event.getGuild()).sendToModerationLog(c -> c.sendMessageEmbeds(embed));
			event.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue();
		}, e -> Responses.error(event.getHook(), e.getMessage()));
	}

	private @NotNull MessageEmbed buildSelfRoleDeletedEmbed(@NotNull User changedBy, @NotNull Message message) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(changedBy), message.getJumpUrl(), changedBy.getEffectiveAvatarUrl())
				.setTitle("Self Roles Removed")
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Channel", message.getChannel().getAsMention(), true)
				.addField("Message", String.format("[Jump to Message](%s)", message.getJumpUrl()), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
