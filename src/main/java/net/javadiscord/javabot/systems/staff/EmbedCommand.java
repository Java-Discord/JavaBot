package net.javadiscord.javabot.systems.staff;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.awt.*;
import java.util.function.UnaryOperator;

// TODO: Refactor embed interface completely.

/**
 * Command that allows staff-members to create embed messages.
 */
@Deprecated(forRemoval = true)
public class EmbedCommand implements SlashCommand {

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {

		return switch (event.getSubcommandName()) {
			case "create" -> createEmbed(event);
			case "from-message" -> createEmbedFromLink(event);
			default -> Responses.warning(event, "Unknown subcommand.");
		};
	}

	private ReplyCallbackAction createEmbedFromLink(SlashCommandInteractionEvent event) {
		String link = event.getOption("link").getAsString();
		String[] value = link.split("/");

		Message message;

		TextChannel channel = event.getGuild().getTextChannelById(value[5]);
		message = channel.retrieveMessageById(value[6]).complete();

		OptionMapping embedOption = event.getOption("title");
		String title = embedOption == null ? null : embedOption.getAsString();

		var eb = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setTitle(title)
				.setDescription(message.getContentRaw())
				.build();

		event.getChannel().sendMessageEmbeds(eb).queue();
		return event.reply("Done!").setEphemeral(true);
	}

	private ReplyCallbackAction createEmbed(SlashCommandInteractionEvent event) {
		UnaryOperator<String> getOpt = s -> {
			var mapping = event.getOption(s);
			return mapping == null ? null : mapping.getAsString();
		};
		String title = getOpt.apply("title");
		String description = getOpt.apply("description");
		String authorname = getOpt.apply("author-name");
		String url = getOpt.apply("author-url");
		String iconurl = getOpt.apply("author-iconurl");
		String thumb = getOpt.apply("thumbnail-url");
		String img = getOpt.apply("image-url");
		String color = getOpt.apply("color");

		var eb = new EmbedBuilder();
		eb.setTitle(title);
		eb.setDescription(description);
		eb.setAuthor(authorname, url, iconurl);
		eb.setImage(img);
		eb.setThumbnail(thumb);
		eb.setColor(Color.decode(color));

		event.getChannel().sendMessageEmbeds(eb.build()).queue();
		return event.reply("Done!").setEphemeral(true);
	}
}