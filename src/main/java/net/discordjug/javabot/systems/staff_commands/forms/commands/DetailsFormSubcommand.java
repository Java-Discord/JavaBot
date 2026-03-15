package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.time.Instant;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form details` command. Displays information about the specified form.
 * The information is sent as a non-ephemeral embed in the same channel this
 * command is executed in.
 * 
 * @see FormData
 */
public class DetailsFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public DetailsFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("details", "Get details about a form").addOptions(
				new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a form to get details for", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply().setEphemeral(false).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this id").queue();
			return;
		}

		FormData form = formOpt.get();
		EmbedBuilder embedBuilder = createFormDetailsEmbed(form, event.getGuild());
		embedBuilder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
		embedBuilder.setTimestamp(Instant.now());

		MessageCreateData builder = new MessageCreateBuilder().addEmbeds(embedBuilder.build()).build();

		event.getHook().sendMessage(builder).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}

	private EmbedBuilder createFormDetailsEmbed(FormData form, Guild guild) {
		EmbedBuilder builder = new EmbedBuilder().setTitle("Form details");

		long id = form.id();

		addCodeblockField(builder, "ID", id, true);
		builder.addField("Created at", String.format("<t:%s>", id / 1000L), true);

		builder.addField("Expires at",
				form.hasExpirationTime() ? String.format("<t:%s>", form.expiration().toEpochMilli() / 1000L)
						: "`Never`",
				true);

		addCodeblockField(builder, "State", form.closed() ? "Closed" : form.hasExpired() ? "Expired" : "Open", false);

		builder.addField("Attached in",
				form.isAttached() ? "<#" + form.getMessageChannel().get() + ">" : "*Not attached*", true);
		builder.addField("Attached to",
				form.isAttached()
						? String.format("[Link](https://discord.com/channels/%s/%s/%s)", guild.getId(),
								form.getMessageChannel().get(), form.getMessageId().get())
						: "*Not attached*",
				true);

		builder.addField("Submissions channel", "<#" + form.submitChannel() + ">", true);
		builder.addField("Is one-time", form.onetime() ? ":white_check_mark:" : ":x:", true);
		addCodeblockField(builder, "Submission message",
				form.submitMessage() == null ? "Default" : form.submitMessage(), true);

		addCodeblockField(builder, "Number of fields", form.fields().size(), true);
		addCodeblockField(builder, "Number of submissions", formsRepo.getTotalSubmissionsCount(form), true);

		return builder;
	}

	private static void addCodeblockField(EmbedBuilder builder, String name, Object content, boolean inline) {
		builder.addField(name, String.format("```\n%s\n```", content), inline);
	}

}
