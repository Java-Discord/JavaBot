package net.javadiscord.javabot.systems.staff.self_roles.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.GuildUtils;
import net.javadiscord.javabot.util.MessageActionUtils;

import java.time.Instant;

/**
 * Subcommand that enables all Elements on an ActionRow.
 */
@Slf4j
public class EnableSelfRoleSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var messageIdOption = event.getOption("message-id");
		if (messageIdOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		SlashCommandConfig config = Bot.config.get(event.getGuild()).getSlashCommand();
		event.getChannel().retrieveMessageById(messageIdOption.getAsString()).queue(message -> {
			message.editMessageComponents(MessageActionUtils.enableActionRows(message.getActionRows())).queue();
			MessageEmbed embed = buildSelfRoleEnabledEmbed(event.getUser(), message, config);
			GuildUtils.getLogChannel(event.getGuild()).sendMessageEmbeds(embed).queue();
			event.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue();
		}, e -> Responses.error(event.getHook(), e.getMessage()));
		return event.deferReply(true);
	}

	private MessageEmbed buildSelfRoleEnabledEmbed(User enabledBy, Message message, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(enabledBy.getAsTag(), message.getJumpUrl(), enabledBy.getEffectiveAvatarUrl())
				.setTitle("Self Role enabled")
				.setColor(config.getDefaultColor())
				.addField("Channel", message.getChannel().getAsMention(), true)
				.addField("Message", String.format("[Jump to Message](%s)", message.getJumpUrl()), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
