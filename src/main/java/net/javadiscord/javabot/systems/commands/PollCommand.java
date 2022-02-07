package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

import java.time.Instant;

/**
 * Command that allows user to create polls with up to 10 options.
 */
public class PollCommand implements ISlashCommand {

	private final String[] EMOTES = new String[]{"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private final int MAX_OPTIONS = 10;

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var titleOption = event.getOption("title");
		if (titleOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var embed = new EmbedBuilder()
				.setAuthor(event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
				.setTitle(titleOption.getAsString())
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setTimestamp(Instant.now());
		event.getHook().sendMessageEmbeds(embed.build()).queue(m -> {
			for (int i = 1; i < MAX_OPTIONS + 1; i++) {
				var optionMap = event.getOption("option-" + i);
				if (optionMap != null) {
					embed.getDescriptionBuilder()
							.append(String.format("%s %s\n", EMOTES[i - 1], optionMap.getAsString()));
					m.addReaction(Emoji.fromMarkdown(EMOTES[i - 1]).getAsMention()).queue();
				}
			}
			m.editMessageEmbeds(embed.build()).queue();
		});

		return event.deferReply();
	}
}
