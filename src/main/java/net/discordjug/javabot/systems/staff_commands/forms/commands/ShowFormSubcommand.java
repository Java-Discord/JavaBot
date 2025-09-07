package net.discordjug.javabot.systems.staff_commands.forms.commands;

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
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form show` command.
 */
public class ShowFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public ShowFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("show",
				"Forcefully opens a form dialog, even if it's closed, or not attached to a message")
				.addOption(OptionType.INTEGER, "form-id", "Form ID to add the field to", true, true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.reply("A form with this ID was not found.").setEphemeral(true).queue();
			return;
		}
		FormData form = formOpt.get();
		if (form.getFields().isEmpty()) {
			event.reply("You can't open a form with no fields").setEphemeral(true).queue();
			return;
		}
		event.replyModal(FormInteractionManager.createFormModal(form)).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
				.queue();
	}
}
