package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.time.Instant;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form modify` command.
 */
public class ModifyFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public ModifyFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("modify", "Modify an existing form").addOptions(
				new OptionData(OptionType.INTEGER, "form-id", "ID of the form to modify", true, true),
				new OptionData(OptionType.STRING, "title", "Form title (shown in modal)"),
				new OptionData(OptionType.STRING, "json", "Form inputs data"),
				new OptionData(OptionType.CHANNEL, "submit-channel", "Channel to log form submissions in"),
				new OptionData(OptionType.STRING, "submit-message",
						"Message displayed to the user once they submit the form"),
				new OptionData(OptionType.STRING, "expiration",
						"UTC time after which the form will not accept further submissions. "
								+ FormInteractionManager.DATE_FORMAT_STRING),
				new OptionData(OptionType.BOOLEAN, "onetime",
						"If the form should only accept one submission per user. Defaults to false.")));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this ID").queue();
			return;
		}
		FormData oldForm = formOpt.get();

		String title = event.getOption("title", oldForm.title(), OptionMapping::getAsString);
		long submitChannel = event.getOption("submit-channel", oldForm.submitChannel(), OptionMapping::getAsLong);
		String submitMessage = event.getOption("submit-message", oldForm.submitMessage(), OptionMapping::getAsString);
		Instant expiration;
		if (event.getOption("expiration") == null) {
			expiration = oldForm.expiration();
		} else {
			Optional<Instant> expirationOpt;
			try {
				expirationOpt = FormInteractionManager.parseExpiration(event);
			} catch (IllegalArgumentException e) {
				event.getHook().sendMessage(e.getMessage()).queue();
				return;
			}
			expiration = expirationOpt.orElse(oldForm.expiration());
		}

		boolean onetime = event.getOption("onetime", oldForm.onetime(), OptionMapping::getAsBoolean);

		FormData newForm = new FormData(oldForm.id(), oldForm.fields(), title, submitChannel, submitMessage,
				oldForm.getMessageId().orElse(null), oldForm.getMessageChannel().orElse(null), expiration,
				oldForm.closed(), onetime);

		formsRepo.updateForm(newForm);

		event.getHook().sendMessage("Form updated!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.id())).toList()).queue();
	}

}
