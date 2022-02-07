package net.javadiscord.javabot.systems.staff.reaction_roles.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.GuildUtils;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Subcommand that creates a new Reaction Role/Button.
 */
@Slf4j
public class CreateSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var labelOption = event.getOption("label");
		var roleOption = event.getOption("role");
		var idOption = event.getOption("message-id");
		var permanentOption = event.getOption("permanent");
		var emoteOption = event.getOption("emote");
		if (labelOption == null || roleOption == null || idOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var buttonLabel = labelOption.getAsString();
		var role = roleOption.getAsRole();
		var messageId = idOption.getAsLong();
		var permanent = permanentOption != null && permanentOption.getAsBoolean();
		var emote = emoteOption == null ? null : emoteOption.getAsString();
		event.getChannel().retrieveMessageById(messageId).queue(m -> {
			var buttons = new ArrayList<>(m.getButtons());
			if (emote != null) {
				buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel, Emoji.fromMarkdown(emote)));
			} else {
				buttons.add(Button.of(ButtonStyle.SECONDARY, buttonId(role, permanent), buttonLabel));
			}
			m.editMessageComponents(ActionRow.of(buttons)).queue();
			var config = Bot.config.get(event.getGuild()).getSlashCommand();
			var embed = buildReactionRoleCreateEmbed(emote, event.getUser(), role, event.getChannel(), 0, buttonLabel, config);
			GuildUtils.getLogChannel(event.getGuild()).sendMessageEmbeds(embed).queue();
			event.replyEmbeds(embed).queue();
		}, e -> log.error("Could not create Reaction Role.", e));
		return event.deferReply(true);
	}

	/**
	 * Represents the Button id for a Reaction Role Button.
	 * The id consists of:
	 * <ol>
	 *     <li>"reaction-role:" - specifies the Button Type</li>
	 *     <li>"role.getID():" - The ID of the Role the Bot should give upon Interaction</li>
	 *     <li>"permanent:" - Specifies if the Role is permanent</li>
	 *     <li>"Instant.now();" - Value to avoid Buttons with the same ID.</li>
	 * </ol>
	 * <p>
	 * (This may be improved in the future.)
	 *
	 * @param role      The Role the Bot should give upon Interaction
	 * @param permanent Specifies if the Role is permanent
	 * @return The button's id as a String.
	 */
	private String buttonId(Role role, boolean permanent) {
		return "reaction-role:" + role.getId() + ":" + permanent + ":" + Instant.now();
	}

	private MessageEmbed buildReactionRoleCreateEmbed(String emote, User createdBy, Role role, Channel channel, long messageId, String buttonLabel, SlashCommandConfig config) {
		var embed = new EmbedBuilder()
				.setTitle("Reaction Role created")
				.setColor(config.getDefaultColor())
				.addField("Channel", channel.getAsMention(), true)
				.addField("Role", role.getAsMention(), true)
				.addField("Message Id", String.format("```%s```", messageId), false);
		if (emote != null) embed.addField("Emote", String.format("```%s```", emote), true);
		embed.addField("Button Label", "```" + buttonLabel + "```", true)
				.setFooter(createdBy.getAsTag(), createdBy.getEffectiveAvatarUrl())
				.setTimestamp(Instant.now());
		return embed.build();
	}
}
