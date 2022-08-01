package net.javadiscord.javabot.systems.staff_commands.self_roles;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Subcommands that removes all Elements on an ActionRow.
 */
public class RemoveSelfRolesSubcommand extends SlashCommand.Subcommand {

	public RemoveSelfRolesSubcommand() {
		setSubcommandData(new SubcommandData("remove-all", "Removes all Self-Roles from a specified message.")
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
			new GuildNotificationService(event.getGuild()).sendLogChannelNotification(embed);
			event.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue();
		}, e -> Responses.error(event.getHook(), e.getMessage()));
	}

	private @NotNull MessageEmbed buildSelfRoleDeletedEmbed(@NotNull User changedBy, @NotNull Message message) {
		return new EmbedBuilder()
				.setAuthor(changedBy.getAsTag(), message.getJumpUrl(), changedBy.getEffectiveAvatarUrl())
				.setTitle("Self Roles removed")
				.setColor(Responses.Type.DEFAULT.getColor())
				.addField("Channel", message.getChannel().getAsMention(), true)
				.addField("Message", String.format("[Jump to Message](%s)", message.getJumpUrl()), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
