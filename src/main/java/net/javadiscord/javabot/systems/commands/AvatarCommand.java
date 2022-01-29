package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

/**
 * Command for displaying a full-size version of a user's avatar.
 */
public class AvatarCommand implements SlashCommandHandler {

	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
		OptionMapping option = event.getOption("user");
		Member member = option == null ? event.getMember() : option.getAsMember();
		if (member == null) {
			return Responses.warning(event, "Sorry, this command can only be used in servers.");
		}
		return event.replyEmbeds(generateAvatarEmbed(member.getGuild(), member.getEffectiveName(), member.getEffectiveAvatarUrl()));
	}

	private MessageEmbed generateAvatarEmbed(Guild guild, String tag, String avatarUrl) {
		return new EmbedBuilder()
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setAuthor(tag + " | Avatar")
				.setImage(avatarUrl + "?size=4096")
				.build();
	}
}
