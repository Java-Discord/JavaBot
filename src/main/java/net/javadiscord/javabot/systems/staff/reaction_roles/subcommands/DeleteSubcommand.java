package net.javadiscord.javabot.systems.staff.reaction_roles.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.GuildUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that deletes any Reaction Role/Button.
 */
public class DeleteSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var labelOption = event.getOption("label");
		var idOption = event.getOption("message-id");
		if (labelOption == null || idOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var buttonLabel = labelOption.getAsString();
		var messageId = idOption.getAsLong();
		event.getChannel().retrieveMessageById(messageId).queue(m -> {
			List<Button> buttons = new ArrayList<>(m.getActionRows().get(0).getButtons());
			buttons.removeIf(button -> button.getLabel().equals(buttonLabel));
			if (!buttons.isEmpty()) {
				m.editMessageComponents(ActionRow.of(buttons)).queue();
			} else {
				m.editMessageComponents().queue();
			}
			var config = Bot.config.get(event.getGuild()).getSlashCommand();
			var embed = buildReactionRoleDeleteEmbed(event.getUser(), messageId, buttonLabel, config);
			GuildUtils.getLogChannel(event.getGuild()).sendMessageEmbeds(embed).queue();
			event.replyEmbeds(embed).queue();
		});
		return event.deferReply(true);
	}

	private MessageEmbed buildReactionRoleDeleteEmbed(User createdBy, long messageId, String buttonLabel, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setTitle("Reaction Role deleted")
				.setColor(config.getDefaultColor())
				.addField("Message Id", String.format("```%s```", messageId), false)
				.addField("Button Label", "```" + buttonLabel + "```", true)
				.setFooter(createdBy.getAsTag(), createdBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now())
				.build();
	}
}
