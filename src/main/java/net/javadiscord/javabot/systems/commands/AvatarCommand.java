package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

/**
 * Command for displaying a full-size version of a user's avatar.
 */
public class AvatarCommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var userOption = event.getOption("user");
		Member member = userOption == null ? event.getMember() : userOption.getAsMember();
		if (member == null) {
			return Responses.warning(event, "Sorry, this command can only be used in servers.");
		}
		return event.replyEmbeds(buildAvatarEmbed(member.getGuild(), member.getUser()));
	}

	private MessageEmbed buildAvatarEmbed(Guild guild, User createdBy) {
		return new EmbedBuilder()
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Avatar")
				.setImage(createdBy.getEffectiveAvatarUrl() + "?size=4096")
				.build();
	}

}
