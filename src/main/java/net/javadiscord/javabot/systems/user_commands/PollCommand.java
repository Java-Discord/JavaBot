package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.javadiscord.javabot.util.Responses;

import java.time.Instant;

/**
 * <h3>This class represents the /poll command.</h3>
 */
public class PollCommand extends SlashCommand {
	private static final String ADD_OPTION = "Adds an option.";
	private static final String[] EMOTES = new String[]{"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private static final int MAX_OPTIONS = 10;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public PollCommand() {
		setSlashCommandData(Commands.slash("poll", "Creates a simple poll")
				.addOptions(
						new OptionData(OptionType.STRING, "title", "The title of your poll.", true),
						new OptionData(OptionType.STRING, "option-1", ADD_OPTION, true),
						new OptionData(OptionType.STRING, "option-2", ADD_OPTION, true),
						new OptionData(OptionType.STRING, "option-3", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-4", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-5", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-6", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-7", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-8", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-9", ADD_OPTION, false),
						new OptionData(OptionType.STRING, "option-10", ADD_OPTION, false)
				)
				.setGuildOnly(true)
		);
	}

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
					m.addReaction(Emoji.fromUnicode(EMOTES[i - 1])).queue();
				}
			}
			m.editMessageEmbeds(embed.build()).queue();
		});
	}
}
