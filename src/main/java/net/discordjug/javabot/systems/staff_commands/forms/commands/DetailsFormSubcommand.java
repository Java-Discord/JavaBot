package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.time.Instant;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form details` command.
 */
public class DetailsFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public DetailsFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("details", "Get details about a form").addOptions(
				new OptionData(OptionType.INTEGER, "form-id", "The ID of a form to get details for", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply().setEphemeral(false).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
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
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
				.queue();
	}

	private EmbedBuilder createFormDetailsEmbed(FormData form, Guild guild) {
		EmbedBuilder builder = new EmbedBuilder().setTitle("Form details");

		long id = form.getId();

		addCodeblockField(builder, "ID", id, true);
		builder.addField("Created at", String.format("<t:%s>", id / 1000L), true);

		String expiration;
		builder.addField("Expires at",
				form.hasExpirationTime() ? String.format("<t:%s>", form.getExpiration() / 1000L) : "`Never`", true);

		addCodeblockField(builder, "State", form.isClosed() ? "Closed" : form.hasExpired() ? "Expired" : "Open", false);

		builder.addField("Attached in",
				form.getMessageChannel() == null ? "*Not attached*" : "<#" + form.getMessageChannel() + ">", true);
		builder.addField("Attached to",
				form.getMessageChannel() == null || form.getMessageId() == null ? "*Not attached*"
						: String.format("[Link](https://discord.com/channels/%s/%s/%s)", guild.getId(),
								form.getMessageChannel(), form.getMessageId()),
				true);

		builder.addField("Submissions channel", "<#" + form.getSubmitChannel() + ">", true);
		builder.addField("Is one-time", form.isOnetime() ? ":white_check_mark:" : ":x:", true);
		addCodeblockField(builder, "Submission message",
				form.getSubmitMessage() == null ? "Default" : form.getSubmitMessage(), true);

		addCodeblockField(builder, "Number of fields", form.getFields().size(), true);
		addCodeblockField(builder, "Number of submissions", formsRepo.getTotalSubmissionsCount(form), true);

		return builder;
	}

	private static void addCodeblockField(EmbedBuilder builder, String name, Object content, boolean inline) {
		builder.addField(name, String.format("```\n%s\n```", content), inline);
	}

}
