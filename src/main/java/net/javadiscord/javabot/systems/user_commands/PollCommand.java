package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;

import java.time.Instant;

/**
 * Command that allows user to create polls with up to 10 options.
 */
public class PollCommand extends SlashCommand {
	public PollCommand() {
		setSlashCommandData(Commands.slash("poll", "Creates a simple poll")
				.addOptions(
						new OptionData(OptionType.STRING, "title", "The title of your poll", true),
						new OptionData(OptionType.STRING, "option-1", "Adds an option", true),
						new OptionData(OptionType.STRING, "option-2", "Adds an option", true),
						new OptionData(OptionType.STRING, "option-3", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-4", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-5", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-6", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-7", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-8", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-9", "Adds an option", false),
						new OptionData(OptionType.STRING, "option-10", "Adds an option", false)
				)
				.setGuildOnly(true)
		);
	}

	private final String[] EMOTES = new String[]{"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private final int MAX_OPTIONS = 10;

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping titleOption = event.getOption("title");
		event.deferReply().queue();
		if (titleOption == null) {
			Responses.error(event, "Missing required arguments");
			return;
		}
		EmbedBuilder embed = new EmbedBuilder()
				.setAuthor(event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
				.setTitle(titleOption.getAsString())
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now());
		event.getHook().sendMessageEmbeds(embed.build()).queue(m -> {
			for (int i = 1; i < MAX_OPTIONS + 1; i++) {
				OptionMapping mapping = event.getOption("option-" + i);
				if (mapping != null) {
					embed.getDescriptionBuilder()
							.append(String.format("%s %s\n", EMOTES[i - 1], mapping.getAsString()));
					m.addReaction(Emoji.fromMarkdown(EMOTES[i - 1]).getAsMention()).queue();
				}
			}
			m.editMessageEmbeds(embed.build()).queue();
		});
	}
}
