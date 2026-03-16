package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.time.Instant;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormAttachmentInfo;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form modify` command. Modifies attributes of an existing form. For
 * modifying form fields see {@link AddFieldFormSubcommand} and
 * {@link RemoveFieldFormSubcommand}
 * 
 * @see FormData
 */
public class ModifyFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public ModifyFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("modify",
				"Modify an existing form's data. Use *-field commands to manage form fields")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "ID of the form to modify", true, true),
						new OptionData(OptionType.STRING, FORM_TITLE_FIELD, "Form title (shown in modal)"),
						new OptionData(OptionType.CHANNEL, FORM_SUBMIT_CHANNEL_FIELD,
								"Channel to log form submissions in"),
						new OptionData(OptionType.STRING, FORM_SUBMIT_MESSAGE_FIELD,
								"Message displayed to the user once they submit the form"),
						new OptionData(OptionType.STRING, FORM_EXPIRATION_FIELD,
								"UTC time after which the form stops accepting submissions. - for no expiration. "
										+ FormInteractionManager.DATE_FORMAT_STRING),
						new OptionData(OptionType.BOOLEAN, FORM_ONETIME_FIELD,
								"If the form should only accept one submission per user. Defaults to false.")));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this ID").queue();
			return;
		}
		FormData oldForm = formOpt.get();

		String title = event.getOption(FORM_TITLE_FIELD, oldForm.title(), OptionMapping::getAsString);
		long submitChannel = event.getOption(FORM_SUBMIT_CHANNEL_FIELD, oldForm.submitChannel(),
				OptionMapping::getAsLong);
		String submitMessage = event.getOption(FORM_SUBMIT_MESSAGE_FIELD, oldForm.submitMessage(),
				OptionMapping::getAsString);
		Instant expiration;
		if (event.getOption(FORM_EXPIRATION_FIELD) == null) {
			expiration = oldForm.expiration();
		} else {
			if ("-".equals(event.getOption(FORM_EXPIRATION_FIELD, OptionMapping::getAsString))) {
				expiration = null;
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
		}

		boolean onetime = event.getOption(FORM_ONETIME_FIELD, oldForm.onetime(), OptionMapping::getAsBoolean);

		Long messageId;
		Long messageChannel;

		Optional<FormAttachmentInfo> infoOptional = oldForm.getAttachmentInfo();
		if (infoOptional.isPresent()) {
			FormAttachmentInfo info = infoOptional.get();
			messageId = info.messageId();
			messageChannel = info.messageChannelId();
		} else {
			messageChannel = null;
			messageId = null;
		}

		FormData newForm = new FormData(oldForm.id(), oldForm.fields(), title, submitChannel, submitMessage, messageId,
				messageChannel, expiration, oldForm.closed(), onetime);

		formsRepo.updateForm(newForm);

		event.getHook().sendMessage("Form updated!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}

}
