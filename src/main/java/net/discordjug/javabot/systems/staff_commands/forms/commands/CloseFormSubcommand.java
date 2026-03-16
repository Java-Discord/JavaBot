package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
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
 * The `/form close` command. This command closes a form. A closed form doesn't
 * accept any new submissions. See {@link ReopenFormSubcommand} for a command
 * that can be used to re-open a closed form.
 * 
 * @see FormData
 */
public class CloseFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;
	private final FormInteractionManager interactionManager;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo          the forms repository
	 * @param interactionManager form interaction manager
	 * @param botConfig          main bot configuration
	 */
	public CloseFormSubcommand(FormsRepository formsRepo, FormInteractionManager interactionManager,
			BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		this.interactionManager = interactionManager;
		setCommandData(new SubcommandData("close", "Close an existing form, preventing further submissions.")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a form to close", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		long id = event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong);
		Optional<FormData> formOpt = formsRepo.getForm(id);
		if (formOpt.isEmpty()) {
			event.reply("A form with this ID was not found.").setEphemeral(true).queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.closed()) {
			event.reply("This form is already closed").setEphemeral(true).queue();
			return;
		}

		event.deferReply(true).queue();

		interactionManager.closeForm(event.getGuild(), form);

		event.getHook().sendMessage("Form closed!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
