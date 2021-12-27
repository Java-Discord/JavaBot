package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.time.Instant;

/**
 * Command that allows user to create polls with up to 10 options.
 */
public class PollCommand implements SlashCommandHandler {

	private final String[] EMOTES = new String[]{"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private final int MAX_OPTIONS = 10;

	@Override
	public ReplyAction handle(SlashCommandEvent event) {

		OptionMapping titleOption = event.getOption("title");
		if (titleOption == null) {
			return Responses.error(event, "Missing required arguments");
		}

		var e = new EmbedBuilder()
				.setAuthor(titleOption.getAsString(), null, event.getUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setDescription("")
				.setTimestamp(Instant.now());

		event.getChannel().sendMessageEmbeds(e.build()).queue(m -> {
			for (int i = 1; i < MAX_OPTIONS + 1; i++) {
				OptionMapping optionMap = event.getOption("option-" + i);
				if (optionMap != null) {
					e.getDescriptionBuilder()
							.append(EMOTES[i - 1] + " " + optionMap.getAsString())
							.append("\n");
					m.addReaction(Emoji.fromMarkdown(EMOTES[i - 1]).getAsMention()).queue();
				}
			}
			m.editMessageEmbeds(e.build()).queue();
		});

		return event.reply("Done!").setEphemeral(true);
	}
}
